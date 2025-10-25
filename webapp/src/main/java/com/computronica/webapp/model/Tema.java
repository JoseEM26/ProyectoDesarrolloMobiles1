// src/main/java/com/computronica/webapp/model/Tema.java
package com.computronica.webapp.model;

import com.google.cloud.Timestamp;
import java.io.Serializable;

public class Tema implements Serializable {
    private String id;
    private String asignaturaId;
    private String nombre;
    private String descripcion;
    private boolean estado;
    private Timestamp fechaCreacion;

    public Tema() {}

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAsignaturaId() { return asignaturaId; }
    public void setAsignaturaId(String asignaturaId) { this.asignaturaId = asignaturaId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public boolean isEstado() { return estado; }
    public void setEstado(boolean estado) { this.estado = estado; }

    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}