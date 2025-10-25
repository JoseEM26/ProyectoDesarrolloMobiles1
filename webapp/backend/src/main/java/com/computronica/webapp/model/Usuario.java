// src/main/java/com/computronica/webapp/model/Usuario.java
package com.computronica.webapp.model;

import com.google.cloud.Timestamp;

import lombok.Data;

@Data
public class Usuario {
    private String id;
    private String codigoInstitucional;
    private String sede;
    private String nombre;
    private String apellido;
    private String correoInstitucional;
    private String contrasena;
    private TipoUsuario tipo = TipoUsuario.estudiante;
    private boolean estado = true;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}