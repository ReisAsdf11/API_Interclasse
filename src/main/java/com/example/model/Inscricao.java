package com.example.model;

public class Inscricao {
    private int id;
    private int timeId;
    private int modalidadeId;
    private String dataInscricao;
    private String status;

    public Inscricao(){      
    }

    public Inscricao(int id, int timeId, int modalidadeId, String dataInscricao, String status){
        this.id = id;
        this.timeId = timeId;
        this.modalidadeId = modalidadeId;
        this.dataInscricao = dataInscricao;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTimeId() {
        return timeId;
    }

    public void setTimeId(int timeId) {
        this.timeId = timeId;
    }

    public int getModalidadeId() {
        return modalidadeId;
    }

    public void setModalidadeId(int modalidadeId) {
        this.modalidadeId = modalidadeId;
    }

    public String getDataInscricao() {
        return dataInscricao;
    }

    public void setDataInscricao(String dataInscricao) {
        this.dataInscricao = dataInscricao;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    
}
