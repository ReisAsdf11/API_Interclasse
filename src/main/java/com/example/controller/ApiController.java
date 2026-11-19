package com.example.controller;

import com.example.dao.ConfiguracaoDao;
import com.example.util.JsonUtil;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(urlPatterns = {"/api", "/api/configuracoes", "/api/admin/configuracoes"})
public class ApiController extends HttpServlet {
    private final ConfiguracaoDao configuracaoDao = new ConfiguracaoDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (request.getServletPath().endsWith("/configuracoes")) {
            try {
                JsonUtil.enviar(response, configuracaoDao.listar());
            } catch (SQLException e) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao consultar configuracoes");
            }
            return;
        }

        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("nome", "API Interclasse ETEC");
        resumo.put("versao", "0.1.0");
        resumo.put("descricao", "Sistema de organizacao do interclasse de final de ano");
        Map<String, Object> rotas = new LinkedHashMap<>();
        rotas.put("GET /api", "Resumo da API");
        rotas.put("POST /api/auth/registrar", "Cadastro de participante");
        rotas.put("POST /api/auth/registrar-organizador", "Cadastro de organizador");
        rotas.put("POST /api/auth/login", "Login");
        rotas.put("POST /api/auth/logout", "Logout");
        rotas.put("GET /api/auth/me", "Usuario logado");
        rotas.put("GET /api/times", "Lista de times");
        rotas.put("GET /api/classificacao?modalidadeId=1", "Classificacao dos times");
        rotas.put("GET /api/classificacao/csv?modalidadeId=1", "Exporta classificacao em CSV");
        rotas.put("GET /api/jogadores?modalidadeId=1", "Ranking individual");
        rotas.put("GET /api/configuracoes", "Consulta regras de pontuacao");
        rotas.put("PUT /api/admin/configuracoes", "Altera regras de pontuacao");
        rotas.put("GET/POST /api/turmas", "Consulta e cadastro de turmas");
        rotas.put("GET/POST /api/modalidades", "Consulta e cadastro de modalidades");
        rotas.put("GET/POST /api/inscricoes", "Consulta e cadastro de inscricoes");
        rotas.put("GET/POST /api/partidas", "Consulta e agendamento de partidas");
        rotas.put("POST /api/admin/partidas/finalizar?id=1", "Finaliza partida");
        rotas.put("POST /api/admin/jogadores", "Cadastra jogador");
        rotas.put("PUT /api/admin/jogadores?id=1", "Atualiza gols e assistencias");
        resumo.put("rotas", rotas);
        JsonUtil.enviar(response, resumo);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!"/api/admin/configuracoes".equals(request.getServletPath())) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Rota nao encontrada");
            return;
        }
        try {
            Entrada entrada = JsonUtil.ler(request, Entrada.class);
            if (entrada == null || entrada.chave == null || entrada.valor == null) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "Informe chave e valor");
                return;
            }
            String chave = entrada.chave.toUpperCase();
            if (!chave.matches("PONTOS_VITORIA|PONTOS_EMPATE|PONTOS_DERROTA|PERMITE_EMPATE")) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "Configuracao nao permitida");
                return;
            }
            if (entrada.valor < 0 || ("PERMITE_EMPATE".equals(chave) && entrada.valor != 0 && entrada.valor != 1)) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_BAD_REQUEST, "Valor invalido para a configuracao");
                return;
            }
            if (!configuracaoDao.atualizar(chave, entrada.valor)) {
                JsonUtil.enviarErro(response, HttpServletResponse.SC_NOT_FOUND, "Configuracao nao encontrada");
                return;
            }
            JsonUtil.enviar(response, configuracaoDao.listar());
        } catch (SQLException e) {
            JsonUtil.enviarErro(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erro ao atualizar configuracao");
        }
    }

    public static class Entrada {
        public String chave;
        public Integer valor;
    }
}
