package com.example.controller;

import com.example.dao.ModalidadeDao;
import com.example.model.Modalidade;
import com.example.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/modalidades", "/api/admin/modalidades"})
public class ModalidadeController extends HttpServlet {

    private ModalidadeDao modalidadeDao = new ModalidadeDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String id = req.getParameter("id");

            if (id == null) {
                List<Modalidade> modalidades = modalidadeDao.listar();
                JsonUtil.enviar(resp, modalidades);
                return;
            }

            Modalidade modalidade = modalidadeDao.buscarPorId(Integer.parseInt(id));

            if (modalidade == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Modalidade não encontrada");
                return;
            }

            JsonUtil.enviar(resp, modalidade);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getServletPath().startsWith("/api/admin/")) { JsonUtil.enviarErro(resp, HttpServletResponse.SC_FORBIDDEN, "Use a rota /api/admin para esta operacao"); return; }
        try {
            Modalidade modalidade = JsonUtil.ler(req, Modalidade.class);

            if (modalidade == null || modalidade.getNome() ==  null || modalidade.getNome().isBlank()) {
                JsonUtil.enviarErro(resp , HttpServletResponse.SC_BAD_REQUEST, "Nome obrigatório");
                return;
            }

            modalidadeDao.inserir(modalidade);
            JsonUtil.enviar(resp, modalidade);

        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    protected  void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!req.getServletPath().startsWith("/api/admin/")) { JsonUtil.enviarErro(resp, HttpServletResponse.SC_FORBIDDEN, "Use a rota /api/admin para esta operacao"); return; }
        try {
            String id = req.getParameter("id");
            if (id == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "O id é obrigatório");
                return;
            }

            Modalidade modalidade = JsonUtil.ler(req, Modalidade.class);
            if (modalidade == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Corpo da requisição inválido");
                return;
            }
            modalidade.setId(Integer.parseInt(id));
            if (!modalidadeDao.atualizar(modalidade)) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Modalidade não encontrada");
                return;
            }
            JsonUtil.enviar(resp, modalidade);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        } catch (SQLException e){
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
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

            if (!modalidadeDao.deletar(Integer.parseInt(id))){
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Modalidade não encontrada");
                return;
            }

            JsonUtil.enviarMensagem(resp, HttpServletResponse.SC_NO_CONTENT, "Modalidade excluída com sucesso");

        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY")) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_CONFLICT, "Modalidade possui registros vinculados");
                return;
            }
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        }
    }
}

