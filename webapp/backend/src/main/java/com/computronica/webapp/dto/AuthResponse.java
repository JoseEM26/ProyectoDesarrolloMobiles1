// src/main/java/com/computronica/webapp/dto/AuthResponse.java
package com.computronica.webapp.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String id;
    private String codigoInstitucional;
    private String sede;
    private String nombre;
    private String apellido;
    private String correoInstitucional;
    private String tipo;
    private boolean estado;
    private String token;
}