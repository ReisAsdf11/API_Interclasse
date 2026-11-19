package com.example.controller;

import com.example.dao.TurmaDao;
import com.example.model.Turma;
import com.example.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet(urlPatterns = {"/api/turmas", "/api/admin/turmas"})
public class TurmaController extends HttpServlet {

    private TurmaDao turmaDao = new TurmaDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String id = req.getParameter("id");

            if (id == null) {
                List<Turma> turmas = turmaDao.listar();
                JsonUtil.enviar(resp, turmas);
                return;
            }

            Turma turma = turmaDao.buscarPorId(Integer.parseInt(id));

            if (turma == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Turma não encontrada");
                return;
            }

            JsonUtil.enviar(resp, turma);

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
            Turma turma = JsonUtil.ler(req, Turma.class);

            if (turma == null || turma.getNome() ==  null || turma.getNome().isBlank()) {
                JsonUtil.enviarErro(resp , HttpServletResponse.SC_BAD_REQUEST, "Nome obrigatório");
                return;
            }

            turmaDao.inserir(turma);
            JsonUtil.enviar(resp, turma);

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

            Turma turma = JsonUtil.ler(req, Turma.class);
            if (turma == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Corpo da requisição inválido");
                return;
            }
            turma.setId(Integer.parseInt(id));
            if (!turmaDao.atualizar(turma)) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Turma não encontrada");
                return;
            }
            JsonUtil.enviar(resp, turma);

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

            if (!turmaDao.deletar(Integer.parseInt(id))){
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Turma não encontrada");
                return;
            }

            JsonUtil.enviarMensagem(resp, HttpServletResponse.SC_NO_CONTENT, "Turma excluída com sucesso");

        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        }
    }
}
