// src/main/java/com/computronica/webapp/model/Calificaciones.java
package com.computronica.webapp.model;

import com.google.cloud.Timestamp;

public class Calificaciones {
    private String id;
    private String estudianteId;
    private String asignaturaId;
    private String evaluacion;
    private double nota;
    private Timestamp fecha;

    public Calificaciones() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEstudianteId() { return estudianteId; }
    public void setEstudianteId(String estudianteId) { this.estudianteId = estudianteId; }

    public String getAsignaturaId() { return asignaturaId; }
    public void setAsignaturaId(String asignaturaId) { this.asignaturaId = asignaturaId; }

    public String getEvaluacion() { return evaluacion; }
    public void setEvaluacion(String evaluacion) { this.evaluacion = evaluacion; }

    public double getNota() { return nota; }
    public void setNota(double nota) { this.nota = nota; }

    public Timestamp getFecha() { return fecha; }
    public void setFecha(Timestamp fecha) { this.fecha = fecha; }
}