// src/main/java/com/computronica/webapp/model/Calificaciones.java
package com.computronica.webapp.model;

import com.google.cloud.Timestamp;

import lombok.Data;

@Data
public class Calificaciones {
    private String id;
    private String estudianteId;
    private String asignaturaId;
    private String evaluacion;
    private double nota;
    private Timestamp fecha;
}