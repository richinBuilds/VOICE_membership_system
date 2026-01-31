# Quick Testing Reference Guide

## 🚀 Quick Start

### Run All Tests

```bash
mvn test
```

### Run Specific Test Type

```bash
# Unit tests only
mvn test -Dtest=*ServiceTest,*RepositoryTest,*ValidatorTest

# Controller tests only
mvn test -Dtest=*ControllerTest

# Integration tests only
mvn test -Dtest=*IntegrationTest
```

### Generate Coverage Report

```bash
mvn clean test jacoco:report
# Then open: target/site/jacoco/index.html
```

---

## 📊 Test Coverage Summary

| Layer        | Files  | Tests  | Coverage Target |
| ------------ | ------ | ------ | --------------- |
| Services     | 2      | 15     | 90%             |
| Repositories | 3      | 17     | 85%             |
| Controllers  | 5      | 30     | 80%             |
| Validation   | 1      | 12     | 90%             |
| Integration  | 3      | 18     | Key Flows       |
| **TOTAL**    | **14** | **92** | **80%+**        |

---

## 📁 Test Structure

```
src/test/
├── java/org/voice/membership/
│   ├── controllers/          # API endpoint tests
│   ├── integration/          # End-to-end tests
│   ├── repositories/         # Data access tests
│   ├── services/            # Business logic tests
│   └── validation/          # Validation rule tests
└── resources/
    └── application-test.yaml # Test configuration
```

---

## 🧪 Test Files & What They Cover

### Unit Tests

**UserServiceTest** - User authentication & password reset

- ✓ User login validation
- ✓ Password reset email
- ✓ Token generation & validation
- ✓ User role verification

**LandingPageServiceTest** - Landing page & membership data

- ✓ Membership retrieval
- ✓ Benefits retrieval
- ✓ Default data initialization

**UserRepositoryTest** - User data operations

- ✓ CRUD operations
- ✓ Email queries (case-sensitive & insensitive)
- ✓ User persistence

**MembershipRepositoryTest** - Membership data

- ✓ Active membership queries
- ✓ Membership persistence

**ChildRepositoryTest** - Child data & relationships

- ✓ Child CRUD operations
- ✓ Cascade delete operations

**StrongPasswordValidatorTest** - Password rules

- ✓ Length validation (8+ chars)
- ✓ Character requirements
- ✓ Edge cases

---

### Functional Tests

**HomeControllerTest** - Landing & home pages

- ✓ Landing page rendering
- ✓ Authentication redirects

**ProfileControllerTest** - User profile management

- ✓ View profile
- ✓ Edit profile
- ✓ Child management (add/edit/delete)

**RegisterControllerTest** - User registration

- ✓ Multi-step workflow
- ✓ Validation (email, password)
- ✓ Session management

**AdminControllerTest** - Admin dashboard

- ✓ Dashboard access control
- ✓ User filtering
- ✓ Excel export

**PasswordResetControllerTest** - Password reset

- ✓ Reset request
- ✓ Token validation
- ✓ Password update

---

### Integration Tests

**UserRegistrationIntegrationTest** - Complete registration flow

- ✓ Page accessibility
- ✓ Authorization checks
- ✓ Context loading

**ProfileManagementIntegrationTest** - Profile workflows

- ✓ Complete edit workflow
- ✓ Complete child management workflow

**AuthenticationIntegrationTest** - Auth workflows

- ✓ Complete password reset flow
- ✓ Session management

---

## 🔍 Common Commands

### Development

```bash
# Run tests while developing
mvn test -Dtest=NameOfTest

# Run tests with more output
mvn test -X

# Skip tests (not recommended)
mvn clean install -DskipTests
```

### Debugging

```bash
# Run single test method
mvn test -Dtest=UserServiceTest#loadUserByUsername_WithValidEmail_ShouldReturnUserDetails

# Run with debugger (port 5005)
mvn test -Dmaven.surefire.debug
```

### CI/CD

```bash
# Clean, compile, and test
mvn clean compile test

# Full build with tests
mvn clean install

# Test with coverage in CI
mvn clean test jacoco:report
```

---

## ✅ Pre-Commit Checklist

Before committing code:

- [ ] `mvn test` - All tests pass
- [ ] `mvn clean compile` - No compilation errors
- [ ] Code coverage maintained (check jacoco report)
- [ ] New features have corresponding tests
- [ ] Tests are meaningful and not just for coverage

---

## 📝 Writing New Tests

### Unit Test Template

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {

    @Mock
    private MyRepository repository;

    @InjectMocks
    private MyService service;

    @Test
    void methodName_withCondition_shouldExpectedBehavior() {
        // Given (Arrange)
        when(repository.method()).thenReturn(value);

        // When (Act)
        Result result = service.method();

        // Then (Assert)
        assertThat(result).isNotNull();
        verify(repository).method();
    }
}
```

### Controller Test Template

```java
@WebMvcTest(MyController.class)
@Import(SecurityConfig.class)
class MyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MyService service;

    @Test
    @WithMockUser(roles = "USER")
    void endpoint_withValidData_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/endpoint"))
               .andExpect(status().isOk())
               .andExpect(view().name("viewName"));
    }
}
```

---

## 🐛 Troubleshooting

### Tests won't run

```bash
# Clean and recompile
mvn clean compile
mvn test
```

### H2 database errors

- Check `src/test/resources/application-test.yaml`
- Ensure H2 dependency in test scope

### Mock issues

- Verify `@MockBean` vs `@Mock`
- Check `@Import(SecurityConfig.class)` for controller tests

### Coverage report not generated

```bash
mvn clean test jacoco:report
```

---

## 📚 Documentation

- **Full Testing Guide:** [TESTING_README.md](TESTING_README.md)
- **Manual Testing:** [MANUAL_TESTING_GUIDE.md](MANUAL_TESTING_GUIDE.md)
- **Execution Checklist:** [TEST_EXECUTION_CHECKLIST.md](TEST_EXECUTION_CHECKLIST.md)
- **Implementation Summary:** [TESTING_IMPLEMENTATION_SUMMARY.md](TESTING_IMPLEMENTATION_SUMMARY.md)

---

## 🎯 Testing Goals

- **Coverage:** Maintain > 80% code coverage
- **Quality:** All tests should be meaningful
- **Speed:** Full test suite < 2 minutes
- **Reliability:** Tests should be deterministic
- **Maintainability:** Tests should be easy to understand

---

## 💡 Best Practices

1. **Test Naming:** `methodName_condition_expectedBehavior`
2. **One Assertion:** Focus each test on one thing
3. **AAA Pattern:** Arrange, Act, Assert
4. **Independence:** Tests shouldn't depend on each other
5. **Mock External:** Mock external dependencies, not internal logic
6. **Clean Data:** Use `@BeforeEach` to set up clean test data

---

## 🔗 Quick Links

- JUnit 5: https://junit.org/junit5/
- Mockito: https://site.mockito.org/
- AssertJ: https://assertj.github.io/doc/
- Spring Boot Test: https://spring.io/guides/gs/testing-web/

---

_Quick Reference Guide v1.0_
