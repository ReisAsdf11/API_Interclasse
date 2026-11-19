package com.example.controller;

import com.example.dao.TimeDao;
import com.example.dao.ConfiguracaoDao;
import com.example.model.Time;
import com.example.util.Conexao;
import com.example.util.JsonUtil;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = {"/api/times", "/api/admin/times", "/api/classificacao", "/api/classificacao/csv", "/api/jogadores", "/api/admin/jogadores"})
public class TimeController extends HttpServlet {
    private final TimeDao dao = new TimeDao();
    private final ConfiguracaoDao configuracaoDao = new ConfiguracaoDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String rota = request.getServletPath();
        if (rota.startsWith("/api/classificacao")) {
            consultarClassificacao(request, response);
            return;
        }
        if ("/api/jogadores".equals(rota)) {
            consultarJogadores(request, response);
            return;
        }
        try {
            String id = request.getParameter("id");
            if (id == null) {
                JsonUtil.enviar(response, dao.listar());
                return;
            }
            Time time = dao.buscarPorId(Integer.parseInt(id));
            if (time == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Time nao encontrado");
                return;
            }
            JsonUtil.enviar(response, time);
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao consultar times");
        }
    }

    private void consultarClassificacao(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String modalidade = request.getParameter("modalidadeId");
        if (modalidade == null || modalidade.isBlank()) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "modalidadeId e obrigatorio");
            return;
        }
        try {
            int modalidadeId = Integer.parseInt(modalidade);
            int pontosVitoria = configuracaoDao.buscarValor("PONTOS_VITORIA");
            int pontosEmpate = configuracaoDao.buscarValor("PONTOS_EMPATE");
            int pontosDerrota = configuracaoDao.buscarValor("PONTOS_DERROTA");
            String sql = "SELECT t.id, t.nome, " +
                    "COALESCE(SUM(CASE WHEN p.id IS NULL THEN 0 WHEN (p.time_a_id=t.id AND p.placar_a>p.placar_b) OR (p.time_b_id=t.id AND p.placar_b>p.placar_a) THEN ? WHEN p.placar_a=p.placar_b THEN ? ELSE ? END),0) pontos, " +
                    "COALESCE(SUM(CASE WHEN p.status='FINALIZADA' AND ((p.time_a_id=t.id AND p.placar_a>p.placar_b) OR (p.time_b_id=t.id AND p.placar_b>p.placar_a)) THEN 1 ELSE 0 END),0) vitorias, " +
                    "COALESCE(SUM(CASE WHEN p.time_a_id=t.id THEN p.placar_a-p.placar_b WHEN p.time_b_id=t.id THEN p.placar_b-p.placar_a ELSE 0 END),0) saldo " +
                    "FROM time t JOIN inscricao i ON i.time_id=t.id AND i.status='CONFIRMADA' AND i.modalidade_id=? " +
                    "LEFT JOIN partida p ON p.modalidade_id=? AND p.status='FINALIZADA' AND (p.time_a_id=t.id OR p.time_b_id=t.id) " +
                    "GROUP BY t.id,t.nome ORDER BY pontos DESC,vitorias DESC,saldo DESC,t.nome COLLATE NOCASE";
            List<Map<String, Object>> tabela = new ArrayList<>();
            try (PreparedStatement ps = Conexao.getConexao().prepareStatement(sql)) {
                ps.setInt(1, pontosVitoria);
                ps.setInt(2, pontosEmpate);
                ps.setInt(3, pontosDerrota);
                ps.setInt(4, modalidadeId);
                ps.setInt(5, modalidadeId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("timeId", rs.getInt("id"));
                        item.put("time", rs.getString("nome"));
                        item.put("pontos", rs.getInt("pontos"));
                        item.put("vitorias", rs.getInt("vitorias"));
                        item.put("saldoGols", rs.getInt("saldo"));
                        tabela.add(item);
                    }
                }
            }
            if (request.getServletPath().endsWith("/csv")) {
                response.setContentType("text/csv;charset=UTF-8");
                response.setHeader("Content-Disposition", "attachment; filename=classificacao.csv");
                PrintWriter out = response.getWriter();
                out.println("Time,Pontos,Vitorias,Saldo de gols");
                for (Map<String, Object> item : tabela) {
                    out.println("\"" + item.get("time").toString().replace("\"", "\"\"") + "\"," + item.get("pontos") + "," + item.get("vitorias") + "," + item.get("saldoGols"));
                }
                out.flush();
            } else {
                JsonUtil.enviar(response, tabela);
            }
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "modalidadeId invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao calcular classificacao");
        }
    }

    private void consultarJogadores(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String modalidade = request.getParameter("modalidadeId");
        String sql = "SELECT j.id,j.nome,t.nome AS time,e.gols,e.assistencias FROM jogador j JOIN time t ON t.id=j.time_id LEFT JOIN estatistica_jogador e ON e.jogador_id=j.id ORDER BY e.gols DESC,e.assistencias DESC,j.nome";
        try (Connection conn = Conexao.getConexao()) {
            if (modalidade != null && !modalidade.isBlank()) {
                Integer.parseInt(modalidade);
                sql = "SELECT j.id,j.nome,t.nome AS time,e.gols,e.assistencias FROM jogador j JOIN time t ON t.id=j.time_id JOIN inscricao i ON i.time_id=t.id AND i.modalidade_id=? AND i.status='CONFIRMADA' LEFT JOIN estatistica_jogador e ON e.jogador_id=j.id ORDER BY e.gols DESC,e.assistencias DESC,j.nome";
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (modalidade != null && !modalidade.isBlank()) ps.setInt(1, Integer.parseInt(modalidade));
                List<Map<String, Object>> lista = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", rs.getInt("id"));
                        item.put("nome", rs.getString("nome"));
                        item.put("time", rs.getString("time"));
                        item.put("gols", rs.getInt("gols"));
                        item.put("assistencias", rs.getInt("assistencias"));
                        lista.add(item);
                    }
                }
                JsonUtil.enviar(response, lista);
            }
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "modalidadeId invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao consultar ranking de jogadores");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String rota = request.getServletPath();
        if ("/api/admin/jogadores".equals(rota)) {
            cadastrarJogador(request, response);
            return;
        }
        if (!rota.startsWith("/api/admin/")) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_FORBIDDEN, "Use a rota /api/admin/times");
            return;
        }
        try {
            Time time = JsonUtil.ler(request, Time.class);
            if (time == null || time.getNome() == null || time.getNome().isBlank()) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "nome e obrigatorio");
                return;
            }
            dao.inserir(time);
            JsonUtil.enviar(response, time);
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao cadastrar time");
        }
    }

    private void cadastrarJogador(HttpServletRequest request, HttpServletResponse response) throws IOException {
        EntradaJogador entrada = JsonUtil.ler(request, EntradaJogador.class);
        if (entrada == null || entrada.nome == null || entrada.nome.isBlank() || entrada.timeId == null || entrada.timeId <= 0) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "nome e timeId sao obrigatorios");
            return;
        }
        try (Connection conn = Conexao.getConexao()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO jogador(nome,time_id) VALUES(?,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, entrada.nome.trim());
                ps.setInt(2, entrada.timeId);
                ps.executeUpdate();
                int id;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("Nao foi possivel obter o id do jogador");
                    id = rs.getInt(1);
                }
                try (PreparedStatement stats = conn.prepareStatement("INSERT INTO estatistica_jogador(jogador_id,gols,assistencias) VALUES(?,0,0)")) {
                    stats.setInt(1, id);
                    stats.executeUpdate();
                }
                conn.commit();
                Map<String, Object> retorno = new LinkedHashMap<>();
                retorno.put("id", id);
                retorno.put("nome", entrada.nome.trim());
                retorno.put("timeId", entrada.timeId);
                JsonUtil.enviar(response, retorno);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "Time inexistente ou erro ao cadastrar jogador");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String rota = request.getServletPath();
        if ("/api/admin/jogadores".equals(rota)) {
            atualizarEstatisticas(request, response);
            return;
        }
        if (!rota.startsWith("/api/admin/")) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_FORBIDDEN, "Use a rota /api/admin/times");
            return;
        }
        try {
            String id = request.getParameter("id");
            if (id == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id e obrigatorio");
                return;
            }
            Time time = JsonUtil.ler(request, Time.class);
            if (time == null || time.getNome() == null || time.getNome().isBlank()) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "nome e obrigatorio");
                return;
            }
            time.setId(Integer.parseInt(id));
            if (!dao.atualizar(time)) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Time nao encontrado");
                return;
            }
            JsonUtil.enviar(response, time);
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao atualizar time");
        }
    }

    private void atualizarEstatisticas(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.getParameter("id");
        EntradaJogador entrada = JsonUtil.ler(request, EntradaJogador.class);
        if (id == null || entrada == null || entrada.gols == null || entrada.assistencias == null || entrada.gols < 0 || entrada.assistencias < 0) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "Informe id, gols e assistencias validos");
            return;
        }
        try (PreparedStatement ps = Conexao.getConexao().prepareStatement("UPDATE estatistica_jogador SET gols=?,assistencias=? WHERE jogador_id=?")) {
            ps.setInt(1, entrada.gols);
            ps.setInt(2, entrada.assistencias);
            ps.setInt(3, Integer.parseInt(id));
            if (ps.executeUpdate() == 0) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Jogador nao encontrado");
                return;
            }
            JsonUtil.enviarMensagem(response, HttpServletResponse.SC_OK, "Estatisticas atualizadas");
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao atualizar estatisticas");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!request.getServletPath().startsWith("/api/admin/")) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_FORBIDDEN, "Use a rota /api/admin/times");
            return;
        }
        try {
            String id = request.getParameter("id");
            if (id == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id e obrigatorio");
                return;
            }
            if (!dao.deletar(Integer.parseInt(id))) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Time nao encontrado");
                return;
            }
            JsonUtil.enviarMensagem(response, HttpServletResponse.SC_OK, "Time removido");
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "id invalido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_CONFLICT, "Time possui registros vinculados ou nao pode ser removido");
        }
    }

    public static class EntradaJogador {
        public String nome;
        public Integer timeId;
        public Integer gols;
        public Integer assistencias;
    }
}
