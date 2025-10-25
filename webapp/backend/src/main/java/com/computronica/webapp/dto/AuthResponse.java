// src/main/java/com/computronica/webapp/dto/AuthResponse.java
package com.computronica.webapp.dto;

import com.computronica.webapp.model.TipoUsuario;
import lombok.Data;

@Data
public class AuthResponse {
    private String id;
    private String codigoInstitucional;
    private String sede;
    private String nombre;
    private String apellido;
    private String correoInstitucional;
    private TipoUsuario tipo;
    private boolean estado;
    private String token;
}