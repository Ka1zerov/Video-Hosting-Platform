package com.tskrypko.authentication.dto;

import com.tskrypko.authentication.validation.PasswordMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@PasswordMatch
public class RegistrationRequest {

    @NotBlank(message = "Username is mandatory")
    private String username;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Confirm password is mandatory")
    private String confirmPassword;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is mandatory")
    @Size(max = 64, message = "First name must be at most 64 characters long")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    @Size(max = 64, message = "Last name must be at most 64 characters long")
    private String lastName;

    @Size(max = 128, message = "Location must be at most 128 characters long")
    private String location;

    @Size(max = 340, message = "Bio must be at most 340 characters long")
    private String bio;
}
