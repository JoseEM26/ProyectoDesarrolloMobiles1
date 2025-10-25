// src/main/java/com/computronica/webapp/model/Tema.java
package com.computronica.webapp.model;

import com.google.cloud.Timestamp;
import lombok.Data;

import java.io.Serializable;

@Data
public class Tema {
    private String id;
    private String asignaturaId;
    private String nombre;
    private String descripcion;
    private boolean estado=true;
    private Timestamp fechaCreacion;

}