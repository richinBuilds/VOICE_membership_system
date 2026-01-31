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

    // Test 1: Valid strong password passes validation
    @Test
    void validate_WithValidPassword_ShouldPass() {
        TestDto dto = new TestDto("ValidPass123!");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    // Test 2: Multiple valid password formats pass validation
    @ParameterizedTest
    @ValueSource(strings = {
            "Abcd123!",
            "MyP@ssw0rd",
            "SecureP4ss!",
            "C0mpl3x!Pass"
    })
    void validate_WithValidPasswords_ShouldPass(String password) {
        TestDto dto = new TestDto(password);

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    // Test 3: Password too short (less than 8 characters) fails
    @Test
    void validate_WithShortPassword_ShouldFail() {
        TestDto dto = new TestDto("Short1!");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }

    // Test 4: Password without uppercase letter fails
    @Test
    void validate_WithNoUppercase_ShouldFail() {
        TestDto dto = new TestDto("lowercase123!");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }

    // Test 5: Password without lowercase letter fails
    @Test
    void validate_WithNoLowercase_ShouldFail() {
        TestDto dto = new TestDto("UPPERCASE123!");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }

    // Test 6: Password without number fails
    @Test
    void validate_WithNoDigit_ShouldFail() {
        TestDto dto = new TestDto("NoDigitPass!");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }

    // Test 7: Password without special character fails
    @Test
    void validate_WithNoSpecialCharacter_ShouldFail() {
        TestDto dto = new TestDto("NoSpecial123");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }

    // Test 8: Null password fails validation
    @Test
    void validate_WithNullPassword_ShouldFail() {
        TestDto dto = new TestDto(null);

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }

    // Test 9: Empty password fails validation
    @Test
    void validate_WithEmptyPassword_ShouldFail() {
        TestDto dto = new TestDto("");

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }

    // Test 10: Multiple weak password formats all fail validation
    @ParameterizedTest
    @ValueSource(strings = {
            "weak",
            "12345678",
            "password",
            "PASSWORD",
            "Pass1234",
            "Pass!@#$",
            "pass123!"
    })
    void validate_WithInvalidPasswords_ShouldFail(String password) {
        TestDto dto = new TestDto(password);

        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
    }
}
