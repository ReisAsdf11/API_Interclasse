package com.example.dao;

import com.example.model.Configuracao;
import com.example.util.Conexao;
import java.sql.*;
import java.util.*;

public class ConfiguracaoDao {
    private static final String[] CHAVES = {"PONTOS_VITORIA", "PONTOS_EMPATE", "PONTOS_DERROTA", "PERMITE_EMPATE"};

    public ConfiguracaoDao() {
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("INSERT OR IGNORE INTO configuracao(chave, valor) VALUES (?, ?)")) {
            String[] valores = {"3", "1", "0", "1"};
            for (int i = 0; i < CHAVES.length; i++) {
                ps.setString(1, CHAVES[i]); ps.setString(2, valores[i]); ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) { throw new RuntimeException("Erro ao criar configuracoes padrao", e); }
    }

    public List<Configuracao> listar() throws SQLException {
        List<Configuracao> lista = new ArrayList<>();
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("SELECT chave, valor FROM configuracao WHERE chave IN (?,?,?,?) ORDER BY chave")) {
            for (int i=0;i<CHAVES.length;i++) ps.setString(i+1, CHAVES[i]);
            try (ResultSet rs=ps.executeQuery()) { while(rs.next()) lista.add(new Configuracao(rs.getString("chave"),rs.getString("valor"))); }
        }
        return lista;
    }

    public int buscarValor(String chave) throws SQLException {
        try (PreparedStatement ps=Conexao.getConexao().prepareStatement("SELECT valor FROM configuracao WHERE chave=?")) {
            ps.setString(1,chave); try(ResultSet rs=ps.executeQuery()){ return rs.next()?Integer.parseInt(rs.getString(1)):0; }
        }
    }

    public boolean atualizar(String chave, int valor) throws SQLException {
        try (PreparedStatement ps=Conexao.getConexao().prepareStatement("UPDATE configuracao SET valor=? WHERE chave=?")) {
            ps.setString(1,String.valueOf(valor)); ps.setString(2,chave); return ps.executeUpdate()>0;
        }
    }
}
