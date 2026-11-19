package com.example.model;

public class Modalidade {
    private int id;
    private String nome;
    private boolean ativo;

    public Modalidade() {
    }

    public Modalidade(int id, String nome, boolean ativo){
        this.id = id;
        this.nome = nome;
        this.ativo = ativo;
    }

    public int getId(){
        return id;
    }

    public String getNome(){
        return nome;
    }

    public boolean isAtivo(){
        return ativo;
    }

    public void setId(int id){
        this.id = id;
    }

    public void setNome(String nome){
        this.nome = nome;
    }

    public void setAtivo(boolean ativo){
        this.ativo = ativo;
    }
}
