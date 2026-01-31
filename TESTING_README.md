# Testing Documentation - VOICE Membership System

## Overview

This document provides comprehensive information about the testing strategy, test execution, and test coverage for the VOICE Membership System.

## Testing Strategy

Our testing approach follows a three-tier strategy:

1. **Unit Testing** - Test individual components in isolation
2. **Functional/Integration Testing** - Test controllers and API endpoints
3. **End-to-End Integration Testing** - Test complete user workflows
4. **Manual Ad-Hoc Testing** - Exploratory and edge case testing

## Test Structure

```
src/test/java/org/voice/membership/
├── controllers/              # Controller layer tests
│   ├── AdminControllerTest.java
│   ├── HomeControllerTest.java
│   ├── PasswordResetControllerTest.java
│   ├── ProfileControllerTest.java
│   └── RegisterControllerTest.java
├── integration/             # End-to-end integration tests
│   ├── AuthenticationIntegrationTest.java
│   ├── ProfileManagementIntegrationTest.java
│   └── UserRegistrationIntegrationTest.java
├── repositories/            # Repository layer tests
│   ├── ChildRepositoryTest.java
│   ├── MembershipRepositoryTest.java
│   └── UserRepositoryTest.java
├── services/               # Service layer tests
│   ├── LandingPageServiceTest.java
│   └── UserServiceTest.java
└── validation/            # Validation tests
    └── StrongPasswordValidatorTest.java
```

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=UserServiceTest
```

### Run Tests with Coverage

```bash
mvn clean test jacoco:report
```

### Run Integration Tests Only

```bash
mvn test -Dtest=*IntegrationTest
```

### Run Unit Tests Only

```bash
mvn test -Dtest=!*IntegrationTest
```

## Test Configuration

### Test Properties

Location: `src/test/resources/application-test.yaml`

Key configurations:

- H2 in-memory database for testing
- Mock email server configuration
- Debug logging enabled

### Test Dependencies

- JUnit 5 (Jupiter) - Testing framework
- Mockito - Mocking framework
- AssertJ - Fluent assertions
- Spring Boot Test - Integration testing
- H2 Database - In-memory test database
- REST Assured - API testing

## Test Coverage

### Unit Tests

#### UserServiceTest

Tests the UserService business logic:

- ✅ Load user by username
- ✅ User authentication
- ✅ Password reset token generation
- ✅ Password reset execution
- ✅ Email validation
- ✅ Error handling for non-existent users

#### LandingPageServiceTest

Tests landing page and membership services:

- ✅ Retrieve all memberships
- ✅ Retrieve all benefits
- ✅ Get landing page content
- ✅ Initialize default memberships
- ✅ Initialize default benefits
- ✅ Initialize landing page content
- ✅ Idempotent initialization (doesn't duplicate data)

#### Repository Tests

Test data access layer:

- ✅ CRUD operations
- ✅ Custom query methods
- ✅ Cascade operations
- ✅ Transaction management

#### StrongPasswordValidatorTest

Tests password validation rules:

- ✅ Minimum 8 characters
- ✅ Contains uppercase letter
- ✅ Contains lowercase letter
- ✅ Contains digit
- ✅ Contains special character
- ✅ Edge cases and invalid passwords

### Functional Tests

#### Controller Tests

Test HTTP endpoints and request handling:

**HomeControllerTest:**

- ✅ Landing page rendering
- ✅ Login page access
- ✅ Home page redirects

**ProfileControllerTest:**

- ✅ View profile
- ✅ Edit profile
- ✅ Add child
- ✅ Edit child
- ✅ Delete child
- ✅ Authorization checks

**RegisterControllerTest:**

- ✅ Multi-step registration flow
- ✅ Email uniqueness validation
- ✅ Password validation
- ✅ Session management
- ✅ Error handling

**AdminControllerTest:**

- ✅ Admin dashboard access
- ✅ User filtering
- ✅ Export to Excel
- ✅ View user details
- ✅ Authorization checks

**PasswordResetControllerTest:**

- ✅ Forgot password flow
- ✅ Reset password with token
- ✅ Token validation
- ✅ Password mismatch handling

### Integration Tests

#### UserRegistrationIntegrationTest

Tests complete registration workflow:

- ✅ Landing page accessibility
- ✅ Registration page access
- ✅ Protected route authorization
- ✅ Application context loading
- ✅ Database connectivity

#### ProfileManagementIntegrationTest

Tests profile management end-to-end:

- ✅ View profile → Edit → Save workflow
- ✅ Add child → Edit → Delete workflow
- ✅ Session persistence
- ✅ Data consistency

#### AuthenticationIntegrationTest

Tests authentication workflows:

- ✅ Complete password reset flow
- ✅ Session management
- ✅ Login/logout functionality
- ✅ Token generation and validation

## Coverage Goals

| Component    | Target Coverage | Current Status |
| ------------ | --------------- | -------------- |
| Services     | 90%             | ✅ Achieved    |
| Repositories | 85%             | ✅ Achieved    |
| Controllers  | 80%             | ✅ Achieved    |
| Validation   | 90%             | ✅ Achieved    |
| Integration  | Key Flows       | ✅ Covered     |

## Manual Testing

See [MANUAL_TESTING_GUIDE.md](MANUAL_TESTING_GUIDE.md) for detailed manual testing procedures.

### Key Manual Test Areas:

1. User registration (all steps)
2. Login and authentication
3. Profile management
4. Child management
5. Admin dashboard and filtering
6. Password reset flow
7. Excel export functionality
8. Security and authorization
9. Browser compatibility
10. Mobile responsiveness

## Known Limitations

1. **Email Testing**: Tests use mocked email service. Manual testing required for actual email delivery.
2. **Payment Testing**: Payment integration requires manual testing with test cards.
3. **File Upload**: If file upload features are added, manual testing will be needed.
4. **Browser-Specific**: Some browser-specific behaviors require manual testing.

## Test Data Management

### Test Users

Tests create and clean up test data automatically:

- Regular test users
- Admin test users
- Users with children
- Users with various memberships

### Database State

- Each test uses H2 in-memory database
- Database is created fresh for each test class
- @Transactional ensures rollback after each test

## Continuous Integration

### CI/CD Pipeline Recommendations

```yaml
# Example GitHub Actions workflow
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: "21"
      - name: Run tests
        run: mvn clean test
      - name: Generate coverage report
        run: mvn jacoco:report
      - name: Upload coverage
        uses: codecov/codecov-action@v2
```

## Troubleshooting Tests

### Common Issues

**Tests fail with "Could not find or load main class"**

```bash
mvn clean compile
mvn test
```

**H2 database errors**

- Check application-test.yaml configuration
- Ensure H2 dependency is in test scope

**Mock issues**

- Verify @MockBean vs @Mock usage
- Check @Import(SecurityConfig.class) for controller tests

**Integration test failures**

- Ensure application starts correctly
- Check for port conflicts
- Verify test database configuration

## Best Practices

1. **Isolation**: Each test should be independent
2. **Naming**: Use descriptive test method names
3. **AAA Pattern**: Arrange, Act, Assert
4. **One Assertion**: Focus each test on one behavior
5. **Clean Up**: Use @BeforeEach and @AfterEach appropriately
6. **Mock Wisely**: Mock external dependencies, not internal logic
7. **Test Data**: Use builders for readable test data creation

## Future Testing Enhancements

- [ ] Add performance benchmarking tests
- [ ] Implement contract testing for APIs
- [ ] Add mutation testing
- [ ] Implement visual regression testing
- [ ] Add accessibility testing
- [ ] Create load testing scenarios
- [ ] Implement end-to-end tests with Selenium
- [ ] Add API documentation testing

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [AssertJ Documentation](https://assertj.github.io/doc/)

## Contact

For questions about testing:

- Testing Lead: [Name]
- Development Team: [Contact Info]
- Issue Tracker: [Link]

---

_Last Updated: January 2026_
_Version: 1.0_
