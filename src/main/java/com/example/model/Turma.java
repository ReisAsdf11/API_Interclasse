package com.example.model;

public class Turma {

    private int id;
    private String nome;
    private String curso;
    private int ano;

    public Turma(){
    }

    public Turma(int id, String nome, String curso, int ano) {
        this.id = id;
        this.nome = nome;
        this.curso = curso;
        this.ano = ano;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getCurso() {
        return curso;
    }

    public int getAno() {
        return ano;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }
}

