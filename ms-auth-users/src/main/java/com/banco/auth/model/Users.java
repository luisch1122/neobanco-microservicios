package com.banco.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.banco.auth.validation.ExistsByEmail;

@Entity
@Table(name = "usuarios")
@Data // Lombok genera los Getters y Setters automáticamente
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank
    private String nombre;

    @ExistsByEmail
    @Column(nullable = false, unique = true, length = 100)
    @NotBlank
    private String email;

    @Column(nullable = false)
    @NotBlank
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
    
    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO";
}