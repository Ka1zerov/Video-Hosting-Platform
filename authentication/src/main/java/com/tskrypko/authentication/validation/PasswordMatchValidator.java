package com.tskrypko.authentication.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;


/**
 * Validator for the {@link PasswordMatch} annotation.
 * <p>
 * This validator checks if two specified fields (e.g., password and confirm password) contain matching values.
 * It retrieves the field names from the annotation and uses reflection to validate their values.
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
 * <h3>Validation Process:</h3>
 * <ul>
 *     <li>Retrieves the {@code passwordField} and {@code confirmPasswordField} from the annotation.</li>
 *     <li>Uses reflection to access the field values.</li>
 *     <li>If the fields exist in Class that is using annotation and their values do not match, a validation error is added.</li>
 *     <li>If the fields do not exist, an error is logged.</li>
 * </ul>
 *
 * <h3>Notes:</h3>
 * <ul>
 *     <li>if validated object is null, an error is logged and validation fails (assumes {@code @NotNull} will handle that).</li>
 *     <li>If the field names are incorrect, an error is logged, and validation fails.</li>
 *     <li>If the fields are inaccessible, an error is logged and validation fails.</li>
 * </ul>
 *
 * @author [Piotr Baranowski GPT]
 * @see PasswordMatch
 */

public class PasswordMatchValidator implements ConstraintValidator<PasswordMatch, Object> {

    private static final Logger log = LoggerFactory.getLogger(PasswordMatchValidator.class);
    private String passwordField = "password";
    private String confirmPasswordField = "confirmPassword";

    @Override
    public void initialize(PasswordMatch constraintAnnotation) {
        this.passwordField = constraintAnnotation.passwordField();
        this.confirmPasswordField = constraintAnnotation.confirmPasswordField();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {

        if (object == null) {
            log.error("@PasswordMatch annotation failed cannot validate null object");
            return false;
        }

        try {
            Field password = object.getClass().getDeclaredField(passwordField);
            Field confirmPassword = object.getClass().getDeclaredField(confirmPasswordField);

            password.setAccessible(true);
            confirmPassword.setAccessible(true);

            String passwordValue = castFieldValueToString(password.get(object), password);
            String confirmPasswordValue = castFieldValueToString(confirmPassword.get(object), confirmPassword);

            if (passwordValue.isEmpty() || confirmPasswordValue.isEmpty()) {
                return false;
            }

            boolean isValid = passwordValue.equals(confirmPasswordValue);

            if (!isValid) {
                context.buildConstraintViolationWithTemplate("%s and %s do not match".formatted(passwordField, confirmPasswordField))
                        .addPropertyNode(confirmPasswordField)
                        .addConstraintViolation();

                return false;
            }
            return true;
        } catch (NoSuchFieldException e) {
            log.error("@PasswordMatch annotation failed in class @'{}': Field '{}' not found in class '{}'.", object.getClass().getSimpleName(), e.getMessage(), object.getClass().getSimpleName());
            return false;
        } catch (IllegalAccessException e) {
            log.error("@PasswordMatch annotation failed in class @'{}': Cannot access field '{}'. Ensure it is accessible.", object.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }


    /**
     * Attempts to cast the provided field value to a `String` and performs validation checks.
     * <p>
     * This method checks whether the given field value is null or an empty string. If either of these conditions
     * is met, an error is logged. Additionally, it checks if the field value is of the expected type (`String`),
     * and if not, a `ClassCastException` is caught, and an error is logged with details about the mismatch.
     * </p>
     *
     * @param value The value to be cast to a `String`.
     * @param field The field of which value is cast, used for logging purposes.
     * @return The string value if valid, otherwise an empty string.
     * If the value is null, empty, or a class cast exception occurs, it logs an error message.
     * @throws ClassCastException If the field value cannot be cast to `String`, an error is logged with the details.
     */

    private String castFieldValueToString(Object value, Field field) {
        if (value == null) {
            log.error("@PasswordMatch annotation failed, Field '{}' cannot be null, use @NotBlank annotation on field", field.getName());
            return "";
        }

        String stringValue = "";
        try {
            stringValue = (String) value;
            if (stringValue.isBlank()) {
                log.error("@PasswordMatch annotation failed, Field '{}' cannot be empty string, use @NotBlank annotation on field", field.getName());
            }
        } catch (ClassCastException e) {
            log.error("@PasswordMatch annotation failed, ClassCastException: Field '{}' was of type '{}', but expected 'String'.",
                    field.getName(), value.getClass().getName());
        }
        return stringValue;
    }
}
