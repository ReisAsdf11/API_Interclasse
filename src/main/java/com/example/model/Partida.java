package com.example.model;

public class Partida {
    private int id;
    private int modalidadeId;
    private int timeAId;
    private int timeBId;
    private int placarA;
    private int placarB;
    private String data;
    private String hora;
    private String local;
    private String fase;
    private String status;

    public Partida(){
    }

    public Partida(int id, int modalidadeId, int timeAId, int timeBId, int placarA, int placarB, String data, String hora, String local, String fase, String status) {
        this.id = id;
        this.modalidadeId = modalidadeId;
        this.timeAId = timeAId;
        this.timeBId = timeBId;
        this.placarA = placarA;
        this.placarB = placarB;
        this.data = data;
        this.hora = hora;
        this.local = local;
        this.fase = fase;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getModalidadeId() {
        return modalidadeId;
    }

    public void setModalidadeId(int modalidadeId) {
        this.modalidadeId = modalidadeId;
    }

    public int getTimeAId() {
        return timeAId;
    }

    public void setTimeAId(int timeAId) {
        this.timeAId = timeAId;
    }

    public int getTimeBId() {
        return timeBId;
    }

    public void setTimeBId(int timeBId) {
        this.timeBId = timeBId;
    }

    public int getPlacarA() {
        return placarA;
    }

    public void setPlacarA(int placarA) {
        this.placarA = placarA;
    }

    public int getPlacarB() {
        return placarB;
    }

    public void setPlacarB(int placarB) {
        this.placarB = placarB;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getFase() {
        return fase;
    }

    public void setFase(String fase) {
        this.fase = fase;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    
}
