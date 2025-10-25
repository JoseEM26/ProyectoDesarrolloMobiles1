package com.computronica.webapp.dto;

import com.computronica.webapp.model.TipoUsuario;
import lombok.Data;

@Data
public class RegisterRequest {
    private String codigoInstitucional;
    private String sede;
    private String nombre;
    private String apellido;
    private String correoInstitucional;
    private String contrasena;
    private TipoUsuario tipo;
}