package com.example.controller;

import com.example.dao.InscricaoDao;
import com.example.dao.ModalidadeDao;
import com.example.dao.PartidaDao;
import com.example.dao.ConfiguracaoDao;
import com.example.model.Partida;
import com.example.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/partidas", "/api/partidas/finalizar", "/api/admin/partidas", "/api/admin/partidas/finalizar"})
public class PartidaController extends HttpServlet {

    private PartidaDao partidaDao = new PartidaDao();
    private ModalidadeDao modalidadeDao = new ModalidadeDao();
    private InscricaoDao inscricaoDao = new InscricaoDao();
    private ConfiguracaoDao configuracaoDao = new ConfiguracaoDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String id = req.getParameter("id");

            if (id != null && !id.isBlank()) {
                Partida partida = partidaDao.buscarPorId(Integer.parseInt(id));

                if (partida == null) {
                    JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Partida não encontrada");
                    return;
                }

                JsonUtil.enviar(resp, partida);
                return;
            }

            String modalidadeId = req.getParameter("modalidadeId");
            String status = req.getParameter("status");
            String data = req.getParameter("data");

            Integer filtroModalidade = null;
            if (modalidadeId != null && !modalidadeId.isBlank()) {
                filtroModalidade = Integer.parseInt(modalidadeId);
            }

            LocalDate filtroData = null;
            if (data != null && !data.isBlank()) {
                filtroData = LocalDate.parse(data);
            }

            List<Partida> partidas = partidaDao.buscarComFiltros(filtroModalidade, status, filtroData);
            JsonUtil.enviar(resp, partidas);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (DateTimeParseException e){
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Data invalida");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getServletPath().startsWith("/api/admin/")) { JsonUtil.enviarErro(resp, HttpServletResponse.SC_FORBIDDEN, "Use a rota /api/admin para esta operacao"); return; }
        if (req.getServletPath() != null && req.getServletPath().endsWith("/finalizar")) {
            finalizar(req, resp);
            return;
        }

        try {
            Partida partida = JsonUtil.ler(req, Partida.class);

            if (partida == null) {
                JsonUtil.enviarErro(resp , HttpServletResponse.SC_BAD_REQUEST, "Corpo da requisição inválido");
                return;
            }
            if (modalidadeDao.buscarPorId(partida.getModalidadeId()) == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Modalidade inválida");
                return;
            }
            if (!validarTimes(partida, resp)) {
                return;
            }
            if (partida.getFase() == null || !partida.getFase().matches("AVULSO|GRUPOS|QUARTAS|SEMI|FINAL")) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Fase invalida. Use GRUPOS, QUARTAS, SEMI ou FINAL");
                return;
            }
            if (partida.getData() == null || partida.getData().isBlank()) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "A data é obrigatória");
                return;
            }
            if (LocalDate.now().isAfter(LocalDate.parse(partida.getData()))){
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "A data não pode ser no passado");
                return;
            }

            partidaDao.inserir(partida);
            JsonUtil.enviar(resp, partida);

        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (DateTimeParseException e){
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Data invalida");
        }
    }

    
    private boolean validarTimes(Partida partida, HttpServletResponse resp) throws IOException, SQLException {
        if (partida.getTimeAId() == partida.getTimeBId()) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Os times devem ser diferentes");
            return false;
        }
        if (!inscricaoDao.estaConfirmada(partida.getTimeAId(), partida.getModalidadeId())
                || !inscricaoDao.estaConfirmada(partida.getTimeBId(), partida.getModalidadeId())) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Os dois times precisam ter inscrição CONFIRMADA nesta modalidade");
            return false;
        }
        return true;
    }

    
    private void finalizar(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String id = req.getParameter("id");
            if (id == null || id.isBlank()) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "O id é obrigatório");
                return;
            }

            Partida partida = partidaDao.buscarPorId(Integer.parseInt(id));
            if (partida == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Partida não encontrada");
                return;
            }
            if ("FINALIZADA".equals(partida.getStatus())) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_CONFLICT, "Partida já finalizada");
                return;
            }
            if ("CANCELADA".equals(partida.getStatus())) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_CONFLICT, "Partida cancelada não pode ser finalizada");
                return;
            }

            if (partida.getPlacarA() == partida.getPlacarB() && configuracaoDao.buscarValor("PERMITE_EMPATE") == 0) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_CONFLICT, "Empate nao permitido nesta competicao");
                return;
            }

            partida.setStatus("FINALIZADA");
            if (!partidaDao.atualizar(partida)) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Partida não encontrada");
                return;
            }

            JsonUtil.enviar(resp, partida);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    protected  void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getServletPath().startsWith("/api/admin/")) { JsonUtil.enviarErro(resp, HttpServletResponse.SC_FORBIDDEN, "Use a rota /api/admin para esta operacao"); return; }
        try {
            String id = req.getParameter("id");
            if (id == null || id.isBlank()) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "O id é obrigatório");
                return;
            }

            Partida existente = partidaDao.buscarPorId(Integer.parseInt(id));
            if (existente == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Partida não encontrada");
                return;
            }

            Partida partida = JsonUtil.ler(req, Partida.class);
            if (partida == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Corpo da requisição inválido");
                return;
            }
            partida.setId(existente.getId());

            if ("FINALIZADA".equals(existente.getStatus())) {
                boolean placarMudou = partida.getPlacarA() != existente.getPlacarA()
                        || partida.getPlacarB() != existente.getPlacarB();
                if (placarMudou || !"FINALIZADA".equals(partida.getStatus())) {
                    JsonUtil.enviarErro(resp, HttpServletResponse.SC_CONFLICT,
                            "Partida finalizada não pode ser alterada");
                    return;
                }
            }

            if (modalidadeDao.buscarPorId(partida.getModalidadeId()) == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Modalidade inválida");
                return;
            }
            if (!validarTimes(partida, resp)) {
                return;
            }

            if (!partidaDao.atualizar(partida)) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Partida não encontrada");
                return;
            }
            JsonUtil.enviar(resp, partida);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        } catch (SQLException e){
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPut(req, resp);
        } else {
            super.service(req, resp);
        }
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getServletPath().startsWith("/api/admin/")) { JsonUtil.enviarErro(resp, HttpServletResponse.SC_FORBIDDEN, "Use a rota /api/admin para esta operacao"); return; }
        try {
            String id = req.getParameter("id");
            if (id == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "O id é obrigatório");
                return;
            }

            if (!partidaDao.deletar(Integer.parseInt(id))){
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Partida não encontrada");
                return;
            }

            JsonUtil.enviarMensagem(resp, HttpServletResponse.SC_NO_CONTENT, "Partida excluída com sucesso");

        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        }
    }
}

