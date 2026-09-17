package com.banco.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data // Lombok genera los Getters y Setters automáticamente
@NoArgsConstructor
@AllArgsConstructor
public class UsersRequest {


    @NotBlank
    private String nombre;

    @Email 
    @NotBlank
    private String email;

}
