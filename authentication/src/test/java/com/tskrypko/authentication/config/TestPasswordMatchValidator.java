package com.tskrypko.authentication.config;

import com.tskrypko.authentication.validation.PasswordMatch;
import com.tskrypko.authentication.validation.PasswordMatchValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class TestPasswordMatchValidator {

    @Bean
    @Primary
    public PasswordMatchValidator passwordMatchValidator() {

        return new PasswordMatchValidator() {
            private String passwordField = "password";
            private String confirmPasswordField = "confirmPassword";

            @Override
            public void initialize(PasswordMatch constraintAnnotation) {
            }
        };
    }
}
