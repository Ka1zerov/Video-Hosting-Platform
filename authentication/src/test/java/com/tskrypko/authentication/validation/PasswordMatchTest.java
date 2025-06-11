package com.tskrypko.authentication.validation;

import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PasswordMatchTest {

    @InjectMocks
    private PasswordMatchValidator passwordMatchValidator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("Passwords check")
    public class PasswordsCheck {

    @Test
    void testPasswordMatchValidator_passwordsMatch() {
        // Given: a request with matching password and confirmPassword
        Object matchingPasswordRequest = new Object() {
            private String password = "password123";
            private String confirmPassword = "password123";
        };

        // When: running validation
        boolean isValid = passwordMatchValidator.isValid(matchingPasswordRequest, context);

        // Then: validation should pass (return true)
        assertTrue(isValid);
    }

    @Test
    void testPasswordMatchValidator_passwordsDoNotMatch() {
        // Given: a request with non-matching password and confirmPassword
        Object nonMatchingPasswordRequest = new Object() {
            private String password = "password123";
            private String confirmPassword = "differentPassword123";
        };

        when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        when(violationBuilder.addPropertyNode(anyString()))
                .thenReturn(nodeBuilderCustomizableContext);
        when(violationBuilder.addConstraintViolation())
                .thenReturn(null); // No need to do anything here, just mock to complete the chain


        // When: running validation
        boolean isValid = passwordMatchValidator.isValid(nonMatchingPasswordRequest, context);

        // Then: validation should fail (return false)
        assertFalse(isValid);
    }
}
    @Nested
    @DisplayName("Validating null object")
    public class validatingNullObject {
        @Test
        void testPasswordMatchValidator_validatingObjectwasNull() {
            // Given: a null object to validate

            boolean isValid = passwordMatchValidator.isValid(null, context);

            // Then: validation should fail (return false)
            assertFalse(isValid);
        }
    }

    @Nested
    @DisplayName("castFieldValueToString method tests")
    public class castFieldValueToString {

        @Test
        void testPasswordMatchValidator_nullPasswordFields() {
            // Given: a request with null password and confirmPassword
            Object nullPasswordRequest = new Object() {
                private String password = null;
                private String confirmPassword = null;
            };

            // When: running validation
            boolean isValid = passwordMatchValidator.isValid(nullPasswordRequest, context);

            // Then: validation should fail (return false)
            assertFalse(isValid);
        }

        @Test
        void testPasswordMatchValidator_nullConfirmPasswordField() {
            // Given: a request with null password and confirmPassword
            Object nullPasswordRequest = new Object() {
                @SuppressWarnings("unused")
                private final String password = "test123";
                @SuppressWarnings("unused")
                private final String confirmPassword = null;
            };

            // When: running validation
            boolean isValid = passwordMatchValidator.isValid(nullPasswordRequest, context);

            // Then: validation should fail (return false)
            assertFalse(isValid);
        }
        @Test
        void testCastFieldValueToString_invalidType() {
            // Given: an invalid type (not String) passed to the cast function
            Object invalidTypeRequest = new Object() {
                private Integer password = 12345;
                private String confirmPassword = "test123";
            };

            boolean isValid = passwordMatchValidator.isValid(invalidTypeRequest, context);

            // Then: validation should fail (return false)
            assertFalse(isValid);

        }

        @Test
        void testCastFieldValueToString_emptyString() {
            Object emptyStringRequest = new Object() {
                private String password = "";
                private String confirmPassword = "test123";
            };
            // Given: an empty string passed to the cast function
            boolean isValid = passwordMatchValidator.isValid(emptyStringRequest, context);

            // Then: validation should fail (return false)
            assertFalse(isValid);
        }
    }
}

