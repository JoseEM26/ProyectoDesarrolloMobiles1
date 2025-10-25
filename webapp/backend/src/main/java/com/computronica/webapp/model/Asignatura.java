package com.computronica.webapp.model;


import lombok.Data;

import java.util.List;

@Data
public class Asignatura {
    private String id;
    private String nombre;
    private String codigoAsignatura;
    private String descripcion;
    private int creditos;
    private List<String> profesores;
    private List<String> estudiantes;
}