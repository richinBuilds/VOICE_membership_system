package org.voice.membership.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StrongPasswordValidator
 * Tests password validation rules
 */
class StrongPasswordValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    static class TestDto {
        @StrongPassword
        private String password;

        public TestDto(String password) {
            this.password = password;
        }
    }

    @Test
    void validate_WithValidPassword_ShouldPass() {
        // Given
        TestDto dto = new TestDto("ValidPass123!");

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Abcd123!", // Valid: uppercase, lowercase, digit, special
            "MyP@ssw0rd", // Valid
            "SecureP4ss!", // Valid
            "C0mpl3x!Pass" // Valid
    })
    void validate_WithValidPasswords_ShouldPass(String password) {
        // Given
        TestDto dto = new TestDto(password);

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_WithShortPassword_ShouldFail() {
        // Given
        TestDto dto = new TestDto("Short1!");

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_WithNoUppercase_ShouldFail() {
        // Given
        TestDto dto = new TestDto("lowercase123!");

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_WithNoLowercase_ShouldFail() {
        // Given
        TestDto dto = new TestDto("UPPERCASE123!");

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_WithNoDigit_ShouldFail() {
        // Given
        TestDto dto = new TestDto("NoDigitPass!");

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_WithNoSpecialCharacter_ShouldFail() {
        // Given
        TestDto dto = new TestDto("NoSpecial123");

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_WithNullPassword_ShouldFail() {
        // Given
        TestDto dto = new TestDto(null);

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @Test
    void validate_WithEmptyPassword_ShouldFail() {
        // Given
        TestDto dto = new TestDto("");

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "weak", // Too short
            "12345678", // Only digits
            "password", // Only lowercase
            "PASSWORD", // Only uppercase
            "Pass1234", // No special char
            "Pass!@#$", // No digit
            "pass123!" // No uppercase
    })
    void validate_WithInvalidPasswords_ShouldFail(String password) {
        // Given
        TestDto dto = new TestDto(password);

        // When
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
    }
}
