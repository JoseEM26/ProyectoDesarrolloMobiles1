// src/main/java/com/computronica/webapp/model/Asignatura.java
package com.computronica.webapp.model;

import java.io.Serializable;
import java.util.List;

public class Asignatura implements Serializable {
    private String id;
    private String nombre;
    private String codigoAsignatura;
    private String descripcion;
    private int creditos;
    private List<String> profesores;
    private List<String> estudiantes;

    public Asignatura() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCodigoAsignatura() { return codigoAsignatura; }
    public void setCodigoAsignatura(String codigoAsignatura) { this.codigoAsignatura = codigoAsignatura; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCreditos() { return creditos; }
    public void setCreditos(int creditos) { this.creditos = creditos; }

    public List<String> getProfesores() { return profesores; }
    public void setProfesores(List<String> profesores) { this.profesores = profesores; }

    public List<String> getEstudiantes() { return estudiantes; }
    public void setEstudiantes(List<String> estudiantes) { this.estudiantes = estudiantes; }
}