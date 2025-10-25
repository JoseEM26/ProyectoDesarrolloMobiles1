// src/main/java/com/computronica/webapp/model/Usuario.java
package com.computronica.webapp.model;

public class Usuario {
    private String id;
    private String uid;
    private String email;
    private String nombre;

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}