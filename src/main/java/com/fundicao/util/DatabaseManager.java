package com.fundicao.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static volatile DatabaseManager instance;
    private static final String DB_NAME = "fundicao.db";

    private final String dbUrl;
    private Connection connection; // conexão única reutilizada

    private DatabaseManager() {
        String appDataDir = System.getenv("LOCALAPPDATA");
        if (appDataDir == null) {
            appDataDir = System.getProperty("user.home");
        }
        Path dbPath = Paths.get(appDataDir, "FundicaoApp", DB_NAME);
        File dir = dbPath.getParent().toFile();
        if (!dir.exists()) {
            dir.mkdirs();
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

    /**
     * Retorna sempre a mesma conexão. Se estiver fechada ou nula, abre uma nova.
     * Ideal para SQLite local com acesso single-thread (JavaFX UI thread).
     */
    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(dbUrl);
            // WAL mode: permite leituras simultâneas sem bloquear escritas
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                stmt.execute("PRAGMA foreign_keys = ON;");
                stmt.execute("PRAGMA synchronous = NORMAL;"); // mais rápido, ainda seguro
            }
        }
        return connection;
    }

    /**
     * Fecha a conexão explicitamente. Chame no encerramento da aplicação.
     */
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

    public void inicializar() {
        try (Statement stmt = getConnection().createStatement()) {
            criarTabelaEntidades(stmt);
            criarTabelaProdutos(stmt);
            criarTabelaProdutoFornecedor(stmt);
            criarTabelaNotasFiscais(stmt);
            criarTabelaNotaProdutos(stmt);
            criarTabelaEstoqueMovimentacoes(stmt);
            System.out.println("Banco inicializado com sucesso!");
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inicializar banco: " + e.getMessage(), e);
        }
    }

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
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nota_id INTEGER NOT NULL REFERENCES notas_fiscais(id) ON DELETE CASCADE,
                produto_id INTEGER NOT NULL REFERENCES produtos(id),
                codigo TEXT,
                valor_unitario REAL NOT NULL,
                unidade_medida TEXT,
                quantidade REAL NOT NULL,
                valor_total REAL
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