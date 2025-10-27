// src/main/java/com/computronica/webapp/dto/LoginRequest.java
package com.computronica.webapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class LoginRequest {
    @NotBlank(message = "El token es obligatorio")
    private String idToken;
}