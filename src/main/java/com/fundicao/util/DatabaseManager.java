package com.fundicao.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static volatile DatabaseManager instance;
    private static final String DB_NAME = "fundicao.db";

    /** Versão atual do schema. Incremente sempre que alterar tabelas. */
    private static final int SCHEMA_VERSION = 3;

    private final String dbUrl;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver SQLite não encontrado.", e);
        }

        String appDataDir = System.getenv("LOCALAPPDATA");
        if (appDataDir == null) {
            appDataDir = System.getProperty("user.home");
        }

        Path dbPath = Paths.get(appDataDir, "FundicaoApp", DB_NAME);
        File dir = dbPath.getParent().toFile();
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Não foi possível criar a pasta do banco: " + dir.getAbsolutePath());
        }

        this.dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        System.out.println("Banco de dados em: " + dbPath.toAbsolutePath());
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA synchronous = NORMAL;");
            }
        }
        return connection;
    }

    public synchronized void fechar() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Erro ao fechar conexão: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }

    // =========================================================================
    // Inicialização + Migração
    // =========================================================================

    public void inicializar() {
        try (Statement stmt = getConnection().createStatement()) {
            criarTabelaEntidades(stmt);
            criarTabelaProdutos(stmt);
            criarTabelaProdutoFornecedor(stmt);
            criarTabelaNotasFiscais(stmt);
            criarTabelaNotaProdutos(stmt);
            criarTabelaEstoqueMovimentacoes(stmt);

            int versaoAtual = obterVersaoSchema(stmt);
            aplicarMigracoes(stmt, versaoAtual);

            System.out.println("Banco inicializado com sucesso! Schema v" + SCHEMA_VERSION);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco: " + e.getMessage(), e);
        }
    }

    private int obterVersaoSchema(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void definirVersaoSchema(Statement stmt, int versao) throws SQLException {
        stmt.execute("PRAGMA user_version = " + versao);
    }

    private void aplicarMigracoes(Statement stmt, int versaoAtual) throws SQLException {

        // ── v0/v1 → v2 ──────────────────────────────────────────────────────
        // Adicionava vr_unitario / vr_total como colunas extras.
        // Mantido para bancos que nunca rodaram v2.
        if (versaoAtual < 2) {
            adicionarColunaSeNaoExistir(stmt, "nota_produtos", "vr_unitario", "REAL DEFAULT 0");
            adicionarColunaSeNaoExistir(stmt, "nota_produtos", "vr_total",    "REAL DEFAULT 0");
            executarSilencioso(stmt,
                "UPDATE nota_produtos SET vr_unitario = valor_unitario WHERE vr_unitario = 0 AND valor_unitario IS NOT NULL");
            executarSilencioso(stmt,
                "UPDATE nota_produtos SET vr_total = valor_total WHERE vr_total = 0 AND valor_total IS NOT NULL");
            definirVersaoSchema(stmt, 2);
            System.out.println("[DB] Migração v2 aplicada.");
        }

        // ── v2 → v3 ──────────────────────────────────────────────────────────
        // Problema: a tabela nota_produtos original tinha valor_unitario NOT NULL
        // e valor_total NOT NULL, bloqueando INSERTs que não preenchem essas colunas.
        // No SQLite não é possível remover NOT NULL via ALTER TABLE —
        // a solução é recriar a tabela com o schema correto.
        if (versaoAtual < 3) {
            recriarNotaProdutos(stmt);
            definirVersaoSchema(stmt, 3);
            System.out.println("[DB] Migração v3 aplicada: nota_produtos recriada sem NOT NULL.");
        }

        // ── Adicione próximas migrações aqui ─────────────────────────────────
        // if (versaoAtual < 4) {
        //     ...
        //     definirVersaoSchema(stmt, 4);
        // }
    }

    /**
     * Recria a tabela nota_produtos usando a técnica padrão do SQLite:
     * 1. Renomeia a tabela antiga para _backup
     * 2. Cria a nova tabela com o schema correto (sem NOT NULL nas colunas de valores)
     * 3. Copia os dados preservando vr_unitario / vr_total (ou valor_unitario / valor_total como fallback)
     * 4. Remove o backup
     */
    private void recriarNotaProdutos(Statement stmt) throws SQLException {
        stmt.execute("PRAGMA foreign_keys = OFF");
        try {
            stmt.execute("ALTER TABLE nota_produtos RENAME TO nota_produtos_backup");

            stmt.execute("""
                CREATE TABLE nota_produtos (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    nota_id     INTEGER NOT NULL REFERENCES notas_fiscais(id) ON DELETE CASCADE,
                    produto_id  INTEGER NOT NULL REFERENCES produtos(id),
                    quantidade  REAL DEFAULT 0,
                    vr_unitario REAL DEFAULT 0,
                    vr_total    REAL DEFAULT 0
                )
            """);

            // Copia dados: usa vr_unitario se existir, senão tenta valor_unitario (legado)
            executarSilencioso(stmt, """
                INSERT INTO nota_produtos (id, nota_id, produto_id, quantidade, vr_unitario, vr_total)
                SELECT
                    id,
                    nota_id,
                    produto_id,
                    COALESCE(quantidade,   0),
                    COALESCE(vr_unitario,  valor_unitario, 0),
                    COALESCE(vr_total,     valor_total,    0)
                FROM nota_produtos_backup
            """);

            stmt.execute("DROP TABLE nota_produtos_backup");
        } finally {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
    }

    // =========================================================================
    // Helpers de migração
    // =========================================================================

    private void adicionarColunaSeNaoExistir(Statement stmt,
                                              String tabela,
                                              String coluna,
                                              String definicao) throws SQLException {
        boolean existe = false;
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tabela + ")")) {
            while (rs.next()) {
                if (coluna.equalsIgnoreCase(rs.getString("name"))) {
                    existe = true;
                    break;
                }
            }
        }
        if (!existe) {
            stmt.execute("ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + definicao);
            System.out.println("[DB] Coluna '" + coluna + "' adicionada em '" + tabela + "'.");
        }
    }

    private void executarSilencioso(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("[DB] Aviso na migração (ignorado): " + e.getMessage());
        }
    }

    // =========================================================================
    // Criação de tabelas
    // =========================================================================

    private void criarTabelaEntidades(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS entidades (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                razao_social TEXT NOT NULL,
                tipo TEXT NOT NULL CHECK(tipo IN ('Cliente', 'Fornecedor')),
                tipo_pessoa TEXT CHECK(tipo_pessoa IN ('PJ', 'PF')),
                cnpj_cpf TEXT,
                inscricao_estadual TEXT,
                site TEXT,
                telefone TEXT,
                fax TEXT,
                email TEXT,
                rua TEXT,
                numero TEXT,
                complemento TEXT,
                bairro TEXT,
                cidade TEXT,
                uf TEXT,
                cep TEXT,
                situacao TEXT DEFAULT 'Ativo' CHECK(situacao IN ('Ativo', 'Inativo')),
                criado_em DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    private void criarTabelaProdutos(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS produtos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                descricao TEXT NOT NULL,
                classificacao_fiscal TEXT,
                criado_em DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    private void criarTabelaProdutoFornecedor(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS produto_fornecedor (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                produto_id INTEGER NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
                fornecedor_id INTEGER NOT NULL REFERENCES entidades(id) ON DELETE CASCADE,
                peso_kg REAL,
                vr_kg REAL,
                vr_peca REAL,
                vr_total REAL,
                UNIQUE(produto_id, fornecedor_id)
            )
        """);
    }

    private void criarTabelaNotasFiscais(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS notas_fiscais (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                natureza TEXT NOT NULL,
                numero TEXT NOT NULL,
                data DATE NOT NULL,
                ordem_compra TEXT,
                entidade_id INTEGER REFERENCES entidades(id),
                transporte_rs REAL DEFAULT 0,
                desconto_rs REAL DEFAULT 0,
                transportadora TEXT,
                peso_bruto REAL,
                peso_liquido REAL,
                criado_em DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }

    private void criarTabelaNotaProdutos(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS nota_produtos (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                nota_id     INTEGER NOT NULL REFERENCES notas_fiscais(id) ON DELETE CASCADE,
                produto_id  INTEGER NOT NULL REFERENCES produtos(id),
                quantidade  REAL DEFAULT 0,
                vr_unitario REAL DEFAULT 0,
                vr_total    REAL DEFAULT 0
            )
        """);
    }

    private void criarTabelaEstoqueMovimentacoes(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS estoque_movimentacoes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                produto_id INTEGER NOT NULL REFERENCES produtos(id),
                tipo TEXT NOT NULL CHECK(tipo IN ('Entrada', 'Saida')),
                quantidade REAL NOT NULL,
                data_movimentacao DATE NOT NULL,
                nota_id INTEGER REFERENCES notas_fiscais(id),
                valor_unitario REAL,
                entidade_id INTEGER REFERENCES entidades(id),
                transportadora TEXT,
                ordem_compra TEXT,
                observacoes TEXT,
                criado_em DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);
    }
}
