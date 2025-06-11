package com.tskrypko.authentication.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to validate that two specified fields (typically password and confirm password) contain matching values.
 * <p>
 * This annotation should be placed on a field within a DTO class, usually on the confirmPassword field.
 * It is advised to use @NotBlank annotation on fields that are checked.
 * Fields have to be String type.
 * It requires two field names to be specified: {@code passwordField} and {@code confirmPasswordField}.
 * If these fields are not explicitly defined, the annotation defaults to "password" and "confirmPassword".
 * </p>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * {@code
 * @PasswordMatch(passwordField = "password", confirmPasswordField = "confirmPassword")
 * private String confirmPassword;
 * }
 * </pre>
 *
 * <p>
 * If the specified fields are missing from the class, an error will be logged.
 * </p>
 *
 * @author [Piotr Baranowski GPT]
 * @see PasswordMatchValidator
 */

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = { PasswordMatchValidator.class })
public @interface PasswordMatch {

    /**
     * Error message to be returned when the validation fails.
     *
     * @return the error message
     */
    String message() default "Password and confirm password do not match";

    /**
     * The name of the field that holds the original password.
     * Defaults to "password" if not specified.
     *
     * @return the name of the password field
     */
    String passwordField() default "password";

    /**
     * The name of the field that holds the confirmation password.
     * Defaults to "confirmPassword" if not specified.
     *
     * @return the name of the confirm password field
     */
    String confirmPasswordField() default "confirmPassword";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
