
package com.example.dao;

import com.example.model.Partida;
import com.example.util.Conexao;

import javax.net.ssl.SSLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PartidaDao {

    private static final String SQL_LISTAR = "SELECT * FROM partida ORDER BY data, hora";
    private static final String SQL_BUSCAR = "SELECT * FROM partida WHERE id = ?";
    private static final String SQL_INSERIR = "INSERT INTO partida (modalidade_id, time_a_id, time_b_id, placar_a, placar_b, data, hora, local, fase, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_ATUALIZAR = "UPDATE partida SET modalidade_id = ?, time_a_id = ?, time_b_id = ?, placar_a = ?, placar_b = ?, data = ?, hora = ?, local = ?, fase = ?, status = ? WHERE id = ?";
    private static final String SQL_DELETAR = "DELETE FROM partida WHERE id = ?";

    public List<Partida> listar() throws SQLException {
        List<Partida> partidas = new ArrayList<>();
        Connection conn = Conexao.getConexao();

        try (PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                partidas.add(mapear(rs));
            }
        }

        return partidas;
    }

    public Partida buscarPorId(int id) throws SQLException {
        Connection conn = Conexao.getConexao();

        try (PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public Partida inserir(Partida partida) throws SQLException {
        Connection conn = Conexao.getConexao();

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, partida.getModalidadeId());
            ps.setInt(2, partida.getTimeAId());
            ps.setInt(3, partida.getTimeBId());
            ps.setInt(4, partida.getPlacarA());
            ps.setInt(5, partida.getPlacarB());
            ps.setString(6, partida.getData());
            ps.setString(7, partida.getHora());
            ps.setString(8, partida.getLocal());
            ps.setString(9, partida.getFase());
            ps.setString(10, partida.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    partida.setId(keys.getInt(1));
                }
            }
        }

        return partida;
    }

    public boolean atualizar(Partida partida) throws SQLException {
        Connection conn = Conexao.getConexao();

        try (PreparedStatement ps = conn.prepareStatement(SQL_ATUALIZAR)) {
            ps.setInt(1, partida.getModalidadeId());
            ps.setInt(2, partida.getTimeAId());
            ps.setInt(3, partida.getTimeBId());
            ps.setInt(4, partida.getPlacarA());
            ps.setInt(5, partida.getPlacarB());
            ps.setString(6, partida.getData());
            ps.setString(7, partida.getHora());
            ps.setString(8, partida.getLocal());
            ps.setString(9, partida.getFase());
            ps.setString(10, partida.getStatus());
            ps.setInt(11, partida.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletar(int id) throws SQLException {
        Connection conn = Conexao.getConexao();

        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETAR)) {
            ps.setInt(1, id);

            return ps.executeUpdate() > 0;
        }
    }

    private Partida mapear(ResultSet rs) throws SQLException {
        Partida partida = new Partida();

        partida.setId(rs.getInt("id"));
        partida.setModalidadeId(rs.getInt("modalidade_id"));
        partida.setTimeAId(rs.getInt("time_a_id"));
        partida.setTimeBId(rs.getInt("time_b_id"));
        partida.setPlacarA(rs.getInt("placar_a"));
        partida.setPlacarB(rs.getInt("placar_b"));
        partida.setData(rs.getString("data"));
        partida.setHora(rs.getString("hora"));
        partida.setLocal(rs.getString("local"));
        partida.setFase(rs.getString("fase"));
        partida.setStatus(rs.getString("status"));

        return partida;
    }

    public List<Partida> buscarComFiltros(Integer modalidadeId, String status, LocalDate data) throws SQLException {
        List<Partida> partidas = new ArrayList<>();
        List<Object> parametros = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM partida WHERE 1=1 ");
        if (modalidadeId != null) {
            sql.append("AND modalidade_id=? ");
            parametros.add(modalidadeId);
        }
        if (status != null && !status.isBlank()) {
            sql.append("AND status LIKE ? ");
            parametros.add("%" + status + "%");
        }
        if (data != null) {
            sql.append("AND data LIKE ? ");
            parametros.add("%" + data + "%");
        }

        Connection conn = Conexao.getConexao();
        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                stmt.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    partidas.add(mapear(rs));
                }
            }
        }
        return partidas;
    }
}
