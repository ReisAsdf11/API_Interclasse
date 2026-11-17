package com.example.controller;

import com.example.dao.InscricaoDao;
import com.example.model.Inscricao;
import com.example.util.JsonUtil;
import com.example.util.SessaoUtil;
import com.example.dao.ModalidadeDao;
import com.example.dao.TimeDao;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@WebServlet("/api/inscricoes")
public class InscricaoController extends HttpServlet {

    private InscricaoDao inscricaoDao = new InscricaoDao();
    private ModalidadeDao modalidadeDao = new ModalidadeDao();
    private TimeDao timeDao = new TimeDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String id = req.getParameter("id");
            String modalidadeId = req.getParameter("modalidadeId");

            if (id != null && !id.isBlank()) {
                Inscricao inscricao = inscricaoDao.buscarPorId(Integer.parseInt(id));

                if (inscricao == null) {
                    JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Inscricao não encontrada");
                    return;
                }

                JsonUtil.enviar(resp, inscricao);
                return;
            }

            List<Inscricao> inscricoes;
            if (modalidadeId != null && !modalidadeId.isBlank()) {
                inscricoes = inscricaoDao.listarPorModalidade(Integer.parseInt(modalidadeId));
            } else {
                inscricoes = inscricaoDao.listar();
            }

            JsonUtil.enviar(resp, inscricoes);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Inscricao inscricao = JsonUtil.ler(req, Inscricao.class);

            if (inscricao == null) {
                JsonUtil.enviarErro(resp , HttpServletResponse.SC_BAD_REQUEST, "Inscrição inválida");
                return;
            }

            Integer modalidadeId = lerId(req.getParameter("modalidadeId"), inscricao.getModalidadeId());
            Integer timeId = lerId(req.getParameter("timeId"), inscricao.getTimeId());

            if (modalidadeId == null || timeId == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "modalidadeId e timeId são obrigatórios");
                return;
            }

            if (modalidadeDao.buscarPorId(modalidadeId) == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Modalidade não encontrada");
                return;
            }

            if (timeDao.buscarPorId(timeId) == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Time não encontrado");
                return;
            }

            if (inscricaoDao.buscarPorTimeEModalidade(timeId, modalidadeId) != null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_CONFLICT, "Time já inscrito nesta modalidade");
                return;
            }

            inscricao.setModalidadeId(modalidadeId);
            inscricao.setTimeId(timeId);
            if (inscricao.getStatus() == null || inscricao.getStatus().isBlank()) {
                inscricao.setStatus("PENDENTE");
            }
            if (inscricao.getDataInscricao() == null || inscricao.getDataInscricao().isBlank()) {
                inscricao.setDataInscricao(LocalDate.now().toString());
            }

            inscricaoDao.inserir(inscricao);
            JsonUtil.enviar(resp, inscricao);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    protected  void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String id = req.getParameter("id");
            if (id == null || id.isBlank()) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "O id é obrigatório");
                return;
            }

            Inscricao existente = inscricaoDao.buscarPorId(Integer.parseInt(id));
            if (existente == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Inscricao não encontrada");
                return;
            }

            Inscricao inscricao = JsonUtil.ler(req, Inscricao.class);
            if (inscricao == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Corpo da requisição inválido");
                return;
            }

            Integer modalidadeId = lerId(req.getParameter("modalidadeId"), inscricao.getModalidadeId());
            Integer timeId = lerId(req.getParameter("timeId"), inscricao.getTimeId());
            if (modalidadeId == null) {
                modalidadeId = existente.getModalidadeId();
            }
            if (timeId == null) {
                timeId = existente.getTimeId();
            }

            if (modalidadeId == null || modalidadeId <= 0 || timeId == null || timeId <= 0) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "modalidadeId e timeId são obrigatórios");
                return;
            }

            if (modalidadeDao.buscarPorId(modalidadeId) == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Modalidade não encontrada");
                return;
            }

            if (timeDao.buscarPorId(timeId) == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Time não encontrado");
                return;
            }

            Inscricao outra = inscricaoDao.buscarPorTimeEModalidade(timeId, modalidadeId);
            if (outra != null && outra.getId() != existente.getId()) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_CONFLICT, "Time já inscrito nesta modalidade");
                return;
            }

            inscricao.setId(existente.getId());
            inscricao.setModalidadeId(modalidadeId);
            inscricao.setTimeId(timeId);
            if (inscricao.getStatus() == null || inscricao.getStatus().isBlank()) {
                inscricao.setStatus(existente.getStatus());
            }
            if (inscricao.getDataInscricao() == null || inscricao.getDataInscricao().isBlank()) {
                inscricao.setDataInscricao(existente.getDataInscricao());
            }

            if (!Objects.equals(inscricao.getStatus(), existente.getStatus())) {
                if (!SessaoUtil.estaLogado(req)) {
                    JsonUtil.enviarErro(resp, HttpServletResponse.SC_UNAUTHORIZED, "faca login para alterar o status");
                    return;
                }
                if (!SessaoUtil.ehOrganizador(req)) {
                    JsonUtil.enviarErro(resp, HttpServletResponse.SC_FORBIDDEN, "somente organizador pode alterar o status");
                    return;
                }
            }

            if (!inscricaoDao.atualizar(inscricao)) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Inscricao não encontrada");
                return;
            }

            JsonUtil.enviar(resp, inscricao);

        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        } catch (SQLException e){
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /** Usa o id da query string quando informado; caso contrário, o id vindo do corpo. */
    private Integer lerId(String parametro, int idCorpo) {
        if (parametro != null && !parametro.isBlank()) {
            int id = Integer.parseInt(parametro);
            return id > 0 ? id : null;
        }
        return idCorpo > 0 ? idCorpo : null;
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String id = req.getParameter("id");
            if (id == null) {
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "O id é obrigatório");
                return;
            }

            if (!inscricaoDao.deletar(Integer.parseInt(id))){
                JsonUtil.enviarErro(resp, HttpServletResponse.SC_NOT_FOUND, "Inscricao não encontrada");
                return;
            }

            JsonUtil.enviarMensagem(resp, HttpServletResponse.SC_NO_CONTENT, "Inscricao excluída com sucesso");

        } catch (SQLException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (NumberFormatException e) {
            JsonUtil.enviarErro(resp, HttpServletResponse.SC_BAD_REQUEST, "Id inválido");
        }
    }
}

