// src/main/java/com/computronica/webapp/dto/LoginRequest.java
package com.computronica.webapp.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String correoInstitucional;
    private String contrasena;
}