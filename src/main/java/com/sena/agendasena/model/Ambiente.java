package com.sena.agendasena.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ambientes")
public class Ambiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoAmbiente tipo;

    @Column(nullable = false)
    private int capacidad;

    @Column(nullable = false)
    private boolean activo;

    // Constructors
    public Ambiente() {
    }

    public Ambiente(String nombre, TipoAmbiente tipo, int capacidad, boolean activo) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.capacidad = capacidad;
        this.activo = activo;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoAmbiente getTipo() {
        return tipo;
    }

    public void setTipo(TipoAmbiente tipo) {
        this.tipo = tipo;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
