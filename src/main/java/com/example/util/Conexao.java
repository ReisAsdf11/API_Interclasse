package com.example.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexão com o banco SQLite (arquivo único, sem servidor de banco).
 *
 * O banco e as tabelas são criados automaticamente na primeira conexão.
 */
public class Conexao {

    /** Caminho do arquivo do banco. Pode ser alterado pela variável de ambiente INTERCLASSE_DB. */
    private static final String URL = "jdbc:sqlite:" + caminhoBanco();

    private static Connection conexao = null;

    private static String caminhoBanco() {
        String env = System.getenv("INTERCLASSE_DB");
        if (env == null || env.isBlank()) {
            env = "interclasse.db";
        }
        return env;
    }

    public static Connection getConexao() throws SQLException {
        if (conexao == null || conexao.isClosed()) {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver do SQLite nao encontrado", e);
            }
            Connection nova = DriverManager.getConnection(URL);
            try {
                criarTabelas(nova);
            } catch (SQLException e) {
                nova.close();
                throw e;
            }
            conexao = nova;
        }
        return conexao;
    }

    /**
     * Script das tabelas usadas pela base. Alunos podem ampliar este script
     * conforme implementam as atividades do README.
     */
    private static void criarTabelas(Connection conn) throws SQLException {
        String[] scripts = {
            """
            CREATE TABLE IF NOT EXISTS usuario (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                nome     TEXT    NOT NULL,
                email    TEXT    NOT NULL UNIQUE,
                senha    TEXT    NOT NULL,
                tipo     TEXT    NOT NULL DEFAULT 'PARTICIPANTE'
                             CHECK (tipo IN ('PARTICIPANTE', 'ORGANIZADOR')),
                criado_em TEXT    NOT NULL DEFAULT (datetime('now'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS chave_mestra (
                id    INTEGER PRIMARY KEY AUTOINCREMENT,
                chave TEXT NOT NULL UNIQUE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS time (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                nome        TEXT    NOT NULL,
                cor         TEXT,
                responsavel TEXT,
                contato     TEXT,
                criado_em   TEXT    NOT NULL DEFAULT (datetime('now'))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS turma (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                nome       TEXT NOT NULL,
                curso      TEXT,
                ano        INTEGER DEFAULT (CAST(strftime('%Y', 'now') AS INTEGER))
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS modalidade (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                nome       TEXT NOT NULL,
                ativo      BOOLEAN DEFAULT TRUE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS inscricao (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                time_id    INTEGER,
                modalidade_id INTEGER,
                data_inscricao TEXT NOT NULL DEFAULT (datetime('now')),
                status TEXT CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'NEGADA')),
                FOREIGN KEY (time_id) REFERENCES time(id),
                FOREIGN KEY (modalidade_id) REFERENCES modalidade(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS partida (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                modalidade_id INTEGER,
                time_a_id INTEGER,
                time_b_id INTEGER,
                placar_a INTEGER,
                placar_b INTEGER,
                data DATE,
                hora TIME,
                local TEXT,
                fase TEXT CHECK (fase IN ('AVULSO', 'GRUPOS', 'QUARTAS', 'SEMI', 'FINAL')),
                status TEXT CHECK (status IN ('AGENDADA', 'EM_ANDAMENTO', 'FINALIZADA', 'CANCELADA')),
                FOREIGN KEY (modalidade_id) REFERENCES modalidade(id),
                FOREIGN KEY (time_a_id) REFERENCES time(id),
                FOREIGN KEY (time_b_id) REFERENCES time(id)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS configuracao (
                chave TEXT PRIMARY KEY,
                valor TEXT
            )
            """,
            "INSERT OR IGNORE INTO chave_mestra (chave) VALUES ('ETEC-INTERCLASSE-2026')"
        };

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            for (String script : scripts) {
                stmt.execute(script);
            }
        }
    }
}
