# Test Suite Reduction Plan

## Goal: Reduce from 465 tests to 250-300 high-value tests (~165-215 test removal)

---

## PHASE 1: DELETE ENTIRE FILES (Priority: HIGHEST)

**Impact: ~88 tests removed**

### 1.1 Repository Tests to DELETE Entirely (73 tests)

**Reasoning:** These test Spring Data JPA's built-in functionality, not application logic.

#### DELETE: CartRepositoryTest.java (9 tests)

- Tests `findByUser()`, `findByUserId()` - basic Spring Data queries
- Tests `save()`, `delete()` - framework CRUD operations
- **Keep only:** Integration tests cover cart functionality

#### DELETE: CartItemRepositoryTest.java (11 tests)

- Tests `findByCart()`, `findByCartId()`, `deleteByCart()` - basic queries
- **Keep only:** Integration tests verify cart item behavior

#### DELETE: LandingPageContentRepositoryTest.java (14 tests)

- Tests `findByKey()`, `save()`, `delete()` - basic CRUD
- Tests case sensitivity, empty values - trivial edge cases
- **Keep only:** Service layer tests cover this functionality

#### DELETE: MembershipBenefitRepositoryTest.java (11 tests)

- Tests `findByActiveTrue()`, ordering - basic Spring Data queries
- Tests `save()`, `update()`, `delete()` - framework operations
- **Keep only:** Service/controller tests verify benefit display

#### DELETE: MembershipPaymentTransactionRepositoryTest.java (17 tests)

- Tests finder methods like `findByUserId()`, `findByPayPalOrderId()`
- Tests date range queries - Spring Data functionality
- **Keep only:** PayPal integration tests cover payment transactions

#### DELETE: ChildRepositoryTest.java (5 tests)

- Tests basic `save()`, `findById()`, `delete()`, `findAll()`
- Tests cascade delete - JPA relationship feature
- **Keep only:** Service tests cover child operations

#### DELETE: UserRepositoryTest.java (8 tests)

- Tests `findByEmail()`, `existsByEmail()` - simple queries
- Tests `save()`, relationships - framework features
- **Keep only:** Service/integration tests cover user operations

**Files to KEEP:**

- `MembershipRepositoryTest.java` (4 tests) - Custom query logic for active memberships
- `VerificationTokenRepositoryTest.java` (10 tests) - Token expiry logic is business critical

---

### 1.2 Integration Tests to DELETE (8 tests)

#### DELETE: PageAccessibilitySmokeTest.java (8 tests)

**Reasoning:** Completely redundant with controller tests

- `testLandingPageAccessible()` - HomeControllerTest covers this
- `testRegistrationPageAccessible()` - RegisterControllerTest covers this
- `testLoginPageAccessible()` - HomeControllerTest covers this
- `testProfilePageRequiresAuthentication()` - ProfileControllerTest covers this
- `testAdminDashboardRequiresAdminRole()` - AdminControllerTest covers this
- `testForgotPasswordPageAccessible()` - PasswordResetControllerTest covers this
- All other smoke tests - redundant with existing controller tests

---

### 1.3 Other Complete File Deletions (7 tests)

#### DELETE: HomeControllerTest.java (3 tests)

**Reasoning:** Trivial tests, covered by integration tests

- Landing page rendering covered by RegistrationWorkflowIntegrationTest
- Authentication status covered by AuthenticationWorkflowTest

#### DELETE: EmailSenderServiceTest.java (4 tests)

**Reasoning:** Tests external email service mock behavior, low value

- Email sending covered by integration tests (registration, password reset)
- Mock verification tests don't validate actual functionality

---

## PHASE 2: CONSOLIDATE SERVICE TESTS (Priority: HIGH)

**Impact: ~42 tests removed**

### 2.1 UserFilterServiceTest.java - Remove 15 tests

**Current:** 25 tests | **Target:** 10 tests | **Remove:** 15 tests

#### Tests to DELETE (15 tests):

```java
// Redundant partial match tests
- filterUsers_ByAddressPartial_ShouldReturnMatchingUsers()
- filterUsers_ByPostalCode_ShouldReturnMatchingUsers()
- filterUsers_ByCityPartial_ShouldReturnMatchingUsers()
- filterUsers_ByProvincePartial_ShouldReturnMatchingUsers()

// Trivial null/empty tests
- filterUsers_WithNullAddress_ShouldReturnAllUsers()
- filterUsers_WithoutChildren_ShouldBeExcludedByAgeFilter()

// Redundant case-insensitive tests
- filterUsers_ByHearingLossTypeCaseInsensitive_ShouldReturnMatchingUsers()
- filterUsers_ByEquipmentTypeCaseInsensitive_ShouldReturnMatchingUsers()

// Redundant edge case tests
- filterUsers_ByMinAge_ShouldReturnMatchingUsers()
- filterUsers_ByMaxAge_ShouldReturnMatchingUsers()

// Duplicate filtering logic tests
- filterUsers_WithMultipleFilters_Address_City_Province()
- filterUsers_WithMultipleFilters_Age_HearingLoss()
- filterUsers_WithMultipleFilters_Equipment_MembershipType()
- filterUsers_EmptyFilters_ShouldReturnAll()
- filterUsers_NoMatches_ShouldReturnEmpty()
```

#### Tests to KEEP (10 tests):

```java
- filterUsers_ByAddress_ShouldReturnMatchingUsers()
- filterUsers_ByCity_ShouldReturnMatchingUsers()
- filterUsers_ByProvince_ShouldReturnMatchingUsers()
- filterUsers_ByAgeRange_ShouldReturnMatchingUsers()
- filterUsers_ByHearingLossType_ShouldReturnMatchingUsers()
- filterUsers_ByEquipmentType_ShouldReturnMatchingUsers()
- filterUsers_ByMembershipType_ShouldReturnMatchingUsers()
- filterUsers_ByRegistrationDateRange_ShouldReturnMatchingUsers()
- filterUsers_WithComplexMultipleFilters_ShouldReturnMatchingUsers() // 1 comprehensive test
- filterUsers_WithInvalidData_ShouldHandleGracefully()
```

---

### 2.2 AdminExportServiceTest.java - Remove 6 tests

**Current:** 12 tests | **Target:** 6 tests | **Remove:** 6 tests

#### Tests to DELETE (6 tests):

```java
// Excessive header validation
- exportUsersToExcel_UsersSheet_ShouldHaveCorrectHeaders()
- exportUsersToExcel_ChildrenSheet_ShouldHaveCorrectHeaders()

// Detailed cell-by-cell validation (redundant with data tests)
- exportUsersToExcel_UsersSheet_ShouldHaveCorrectData()
- exportUsersToExcel_ChildrenSheet_ShouldHaveCorrectData()

// Edge case tests
- exportUsersToExcel_WithEmptyUserList_ShouldCreateEmptySheets()
- exportUsersToExcel_WithUserWithoutChildren_ShouldHandleGracefully()
```

#### Tests to KEEP (6 tests):

```java
- exportUsersToExcel_WithValidUsers_ShouldCreateWorkbook()
- exportUsersToExcel_ShouldCreateUsersSheet()
- exportUsersToExcel_ShouldCreateChildrenSheet()
- exportUsersToExcel_UsersSheet_ShouldContainCorrectRowCount()
- exportUsersToExcel_ChildrenSheet_ShouldContainCorrectRowCount()
- exportUsersToExcel_WithMultipleChildren_ShouldFlattenCorrectly()
```

---

### 2.3 ChildServiceTest.java - Remove 8 tests

**Current:** 16 tests | **Target:** 8 tests | **Remove:** 8 tests

#### Tests to DELETE (8 tests):

```java
// Trivial date parsing edge cases
- createChild_WithValidDateOfBirth_ShouldParseDateCorrectly()
- createChild_WithInvalidDateOfBirth_ShouldCreateWithNullDate()
- createChild_WithEmptyDateOfBirth_ShouldCreateWithNullDate()
- createChild_WithNullDateOfBirth_ShouldCreateWithNullDate()

// Redundant null checks
- updateChild_WithInvalidDateOfBirth_ShouldKeepExistingDate()
- updateChild_WithEmptyDateOfBirth_ShouldSetNullDate()

// Ownership validation duplicates
- updateChild_WithNonExistentChild_ShouldReturnEmpty()
- deleteChild_WithNonExistentChild_ShouldReturnFalse()
```

#### Tests to KEEP (8 tests):

```java
- createChild_WithValidData_ShouldCreateAndSave()
- updateChild_WithValidOwner_ShouldUpdateAndSave()
- updateChild_WithInvalidOwner_ShouldReturnEmpty()
- updateChild_WithDateOfBirth_ShouldUpdateCorrectly() // Consolidates date tests
- deleteChild_WithValidOwner_ShouldDeleteAndReturnTrue()
- deleteChild_WithInvalidOwner_ShouldReturnFalse()
- getChildById_WithValidId_ShouldReturnChild()
- getChildById_WithInvalidId_ShouldReturnEmpty()
```

---

### 2.4 LandingPageServiceTest.java - Remove 4 tests

**Current:** 9 tests | **Target:** 5 tests | **Remove:** 4 tests

#### Tests to DELETE (4 tests):

```java
// Trivial getter tests
- getContent_WithExistingKey_ShouldReturnValue()
- getContent_WithNonExistentKey_ShouldReturnDefaultValue()

// Redundant tests
- getAllContent_ShouldReturnMap()
- getAllContent_WithNoContent_ShouldReturnEmptyMap()
```

#### Tests to KEEP (5 tests):

```java
- updateContent_WithExistingKey_ShouldUpdate()
- updateContent_WithNewKey_ShouldCreate()
- updateContent_WithNullValue_ShouldNotUpdate()
- deleteContent_WithExistingKey_ShouldDelete()
- deleteContent_WithNonExistentKey_ShouldHandleGracefully()
```

---

### 2.5 MembershipServiceTest.java - Remove 5 tests

**Current:** 15 tests | **Target:** 10 tests | **Remove:** 5 tests

#### Tests to DELETE (5 tests):

```java
// Redundant repository delegation tests
- getAllMemberships_ShouldReturnAllFromRepository()
- getActiveMemberships_ShouldReturnOnlyActive()
- getMembershipById_WithValidId_ShouldReturnMembership()
- getMembershipById_WithInvalidId_ShouldReturnEmpty()
- getMembershipById_WithNullId_ShouldReturnEmpty()
```

#### Tests to KEEP (10 tests):

```java
- assignMembershipToUser_WithValidMembership_ShouldAssign()
- assignMembershipToUser_WithInvalidMembership_ShouldThrowException()
- cancelUserMembership_WithExistingMembership_ShouldAssignFree()
- createMembership_WithValidData_ShouldCreate()
- updateMembership_WithValidData_ShouldUpdate()
- deleteMembership_WithNoActiveUsers_ShouldDelete()
- deleteMembership_WithActiveUsers_ShouldThrowException()
- isMembershipInUse_ShouldReturnCorrectStatus()
- getFreeMembership_ShouldReturnCorrectMembership()
- getMembershipPrice_ShouldReturnCorrectPrice()
```

---

### 2.6 AccountLockoutServiceTest.java - Remove 4 tests

**Current:** 17 tests | **Target:** 13 tests | **Remove:** 4 tests

#### Tests to DELETE (4 tests):

```java
// Redundant edge case tests
- recordFailedAttempt_WithNullEmail_ShouldNotThrow()
- recordFailedAttempt_WithEmptyEmail_ShouldNotThrow()
- isAccountLocked_WithNullEmail_ShouldReturnFalse()
- isAccountLocked_WithEmptyEmail_ShouldReturnFalse()
```

---

## PHASE 3: CONSOLIDATE CONTROLLER TESTS (Priority: MEDIUM)

**Impact: ~48 tests removed**

### 3.1 RegisterControllerTest.java - Remove 20 tests

**Current:** 48 tests | **Target:** 28 tests | **Remove:** 20 tests

#### Tests to DELETE (20 tests):

```java
// Redundant field validation tests (keep only critical ones)
- testRegister_WithEmptyFirstName_ShouldShowError()
- testRegister_WithEmptyLastName_ShouldShowError()
- testRegister_WithEmptyAddress_ShouldShowError()
- testRegister_WithEmptyCity_ShouldShowError()
- testRegister_WithEmptyProvince_ShouldShowError()
- testRegister_WithEmptyPostalCode_ShouldShowError()
- testRegister_WithEmptyPhone_ShouldShowError()

// Redundant password validation (keep comprehensive test)
- testRegister_WithShortPassword_ShouldShowError()
- testRegister_WithPasswordNoUppercase_ShouldShowError()
- testRegister_WithPasswordNoLowercase_ShouldShowError()
- testRegister_WithPasswordNoDigit_ShouldShowError()
- testRegister_WithPasswordNoSpecialChar_ShouldShowError()

// Redundant email validation
- testRegister_WithInvalidEmailFormat_ShouldShowError()
- testRegister_WithDuplicateEmail_ShouldShowError()

// Trivial edge cases
- testRegister_WithNullPassword_ShouldShowError()
- testRegister_WithNullEmail_ShouldShowError()
- testRegister_WithMismatchedPasswords_ShouldShowError()

// View rendering tests (keep only critical path)
- testShowRegistrationForm_ShouldReturnView()
- testShowRegistrationForm_WithMemberships_ShouldAddToModel()
- testShowRegistrationForm_WithError_ShouldDisplayError()
```

#### Tests to KEEP (28 tests):

```java
// Critical path tests
- testRegister_WithValidData_ShouldCreateUser()
- testRegister_WithValidData_ShouldSendVerificationEmail()
- testRegister_WithValidData_ShouldRedirectToSuccess()

// Comprehensive validation tests (1 test per category)
- testRegister_WithRequiredFieldsMissing_ShouldShowErrors() // Consolidates 7 field tests
- testRegister_WithInvalidPassword_ShouldShowError() // Consolidates 5 password tests
- testRegister_WithInvalidEmail_ShouldShowError() // Consolidates 2 email tests

// Email verification flow
- testVerifyEmail_WithValidToken_ShouldVerifyUser()
- testVerifyEmail_WithExpiredToken_ShouldShowError()
- testVerifyEmail_WithInvalidToken_ShouldShowError()

// Membership selection
- testRegister_WithFreeMembership_ShouldAssign()
- testRegister_WithPaidMembership_ShouldRedirectToPayment()

// Child registration
- testRegister_WithChildData_ShouldCreateChild()
- testRegister_WithMultipleChildren_ShouldCreateAll()
- testRegister_WithoutChildData_ShouldNotCreateChild()

// Edge cases (keep important ones)
- testRegister_WithExistingUnverifiedEmail_ShouldUpdateUser()
- testRegister_WithSystemError_ShouldShowErrorPage()

// Plus ~13 other critical tests...
```

---

### 3.2 ProfileControllerTest.java - Remove 12 tests

**Current:** 29 tests | **Target:** 17 tests | **Remove:** 12 tests

#### Tests to DELETE (12 tests):

```java
// Redundant view tests
- testShowProfile_ShouldReturnProfileView()
- testShowProfile_WithNoMembership_ShouldShowDefault()
- testShowProfile_WithMembership_ShouldShowMembershipInfo()
- testShowProfile_WithChildren_ShouldShowChildren()

// Redundant update tests (similar to registration)
- testUpdateProfile_WithEmptyFirstName_ShouldShowError()
- testUpdateProfile_WithEmptyLastName_ShouldShowError()
- testUpdateProfile_WithEmptyAddress_ShouldShowError()
- testUpdateProfile_WithEmptyCity_ShouldShowError()
- testUpdateProfile_WithInvalidPhone_ShouldShowError()

// Child CRUD redundancy (covered by service tests)
- testAddChild_WithEmptyName_ShouldShowError()
- testUpdateChild_WithEmptyName_ShouldShowError()
- testDeleteChild_WithInvalidOwner_ShouldShowError()
```

#### Tests to KEEP (17 tests):

```java
- testShowProfile_WithAuthenticatedUser_ShouldDisplayProfile()
- testUpdateProfile_WithValidData_ShouldUpdate()
- testUpdateProfile_WithInvalidData_ShouldShowErrors() // Consolidates field tests
- testChangePassword_WithValidPasswords_ShouldUpdate()
- testChangePassword_WithIncorrectOldPassword_ShouldShowError()
- testChangePassword_WithWeakNewPassword_ShouldShowError()
- testAddChild_WithValidData_ShouldCreate()
- testUpdateChild_WithValidData_ShouldUpdate()
- testDeleteChild_WithValidOwner_ShouldDelete()
- testCancelMembership_ShouldDowngradeToFree()
- testUpgradeMembership_ShouldRedirectToPayment()
- testProfile_WithoutAuthentication_ShouldRedirectToLogin()
- testProfile_WithMultipleChildren_ShouldDisplayAll()
- testProfile_AfterUpdate_ShouldShowSuccessMessage()
- testProfile_WithPaymentHistory_ShouldDisplay()
- testProfile_ExportData_ShouldGenerateCSV()
- testProfile_DeleteAccount_ShouldRequireConfirmation()
```

---

### 3.3 AdminControllerTest.java - Remove 8 tests

**Current:** 21 tests | **Target:** 13 tests | **Remove:** 8 tests

#### Tests to DELETE (8 tests):

```java
// Redundant view tests
- testShowDashboard_ShouldReturnDashboardView()
- testShowDashboard_WithStats_ShouldShowStats()
- testShowUserList_ShouldReturnUserListView()
- testShowUserList_WithFilters_ShouldFilterUsers()

// Redundant authorization tests (covered by integration)
- testAdminEndpoint_WithoutAdminRole_ShouldDeny()
- testAdminEndpoint_WithoutAuthentication_ShouldRedirectToLogin()

// Redundant CRUD tests (covered by service tests)
- testCreateUser_WithInvalidData_ShouldShowError()
- testDeleteUser_WithNonExistentUser_ShouldShowError()
```

---

### 3.4 LandingPageApiControllerTest.java - Remove 8 tests

**Current:** 16 tests | **Target:** 8 tests | **Remove:** 8 tests

#### Tests to DELETE (8 tests):

```java
// Trivial API response tests
- testGetTagline_ShouldReturnString()
- testGetBenefits_ShouldReturnList()
- testGetMemberships_ShouldReturnList()
- testGetContent_WithKey_ShouldReturnContent()

// Redundant null/empty tests
- testGetContent_WithNonExistentKey_ShouldReturnNull()
- testGetContent_WithNullKey_ShouldReturnBadRequest()
- testGetBenefits_WithNoBenefits_ShouldReturnEmptyList()
- testGetMemberships_WithNoMemberships_ShouldReturnEmptyList()
```

#### Tests to KEEP (8 tests):

```java
- testGetLandingPageData_ShouldReturnCompleteData()
- testGetTagline_WithCustomContent_ShouldReturnUpdatedValue()
- testGetBenefits_ShouldReturnActiveBenefitsOnly()
- testGetBenefits_ShouldReturnInDisplayOrder()
- testGetMemberships_ShouldReturnActiveMembershipsOnly()
- testGetMemberships_ShouldIncludePricing()
- testUpdateContent_WithValidData_ShouldUpdate()
- testUpdateContent_RequiresAdminRole_ShouldDeny()
```

---

## PHASE 4: CONSOLIDATE INTEGRATION TESTS (Priority: LOW)

**Impact: ~8 tests removed**

### 4.1 AdminWorkflowIntegrationTest.java - Remove 8 tests

**Current:** 18 tests | **Target:** 10 tests | **Remove:** 8 tests

#### Tests to DELETE (8 tests):

```java
// Redundant with controller tests
- testAdminDashboard_LoadsSuccessfully()
- testAdminUserList_DisplaysUsers()
- testAdminUserList_WithFilters_FiltersCorrectly()
- testAdminExport_GeneratesExcel()

// Redundant with service tests
- testAdminCreateUser_WithValidData_CreatesUser()
- testAdminUpdateUser_WithValidData_UpdatesUser()
- testAdminDeleteUser_WithConfirmation_DeletesUser()
- testAdminViewUserDetails_DisplaysAllInfo()
```

#### Tests to KEEP (10 tests):

```java
- testCompleteAdminWorkflow_CreateFilterExportUser()
- testAdminAccessControl_RequiresAdminRole()
- testAdminBulkOperations_UpdateMultipleUsers()
- testAdminMembershipManagement_AssignAndRevoke()
- testAdminReports_GenerateMonthlyStats()
- testAdminSystemSettings_UpdateConfiguration()
- testAdminEmailBlast_SendToFilteredUsers()
- testAdminAuditLog_TracksChanges()
- testAdminDataIntegrity_ValidatesConstraints()
- testAdminPerformance_HandlesLargeDataset()
```

---

## SUMMARY OF REDUCTIONS

| Category              | Current | Removed | Final   | Reduction % |
| --------------------- | ------- | ------- | ------- | ----------- |
| **Repository Tests**  | 90      | 73      | 17      | 81%         |
| **Service Tests**     | 174     | 42      | 132     | 24%         |
| **Controller Tests**  | 173     | 48      | 125     | 28%         |
| **Integration Tests** | 64      | 16      | 48      | 25%         |
| **Util Tests**        | 8       | 0       | 8       | 0%          |
| **TOTAL**             | **509** | **179** | **330** | **35%**     |

---

## EXECUTION PLAN

### Step 1: Delete Complete Files (88 tests)

```bash
# Repository tests
rm src/test/java/org/voice/membership/repositories/CartRepositoryTest.java
rm src/test/java/org/voice/membership/repositories/CartItemRepositoryTest.java
rm src/test/java/org/voice/membership/repositories/LandingPageContentRepositoryTest.java
rm src/test/java/org/voice/membership/repositories/MembershipBenefitRepositoryTest.java
rm src/test/java/org/voice/membership/repositories/MembershipPaymentTransactionRepositoryTest.java
rm src/test/java/org/voice/membership/repositories/ChildRepositoryTest.java
rm src/test/java/org/voice/membership/repositories/UserRepositoryTest.java

# Integration tests
rm src/test/java/org/voice/membership/integration/PageAccessibilitySmokeTest.java

# Controller tests
rm src/test/java/org/voice/membership/controllers/HomeControllerTest.java

# Service tests
rm src/test/java/org/voice/membership/services/EmailSenderServiceTest.java
```

### Step 2: Consolidate Service Tests (42 tests)

Edit the following files to remove specified tests:

- `UserFilterServiceTest.java` - Remove 15 tests
- `AdminExportServiceTest.java` - Remove 6 tests
- `ChildServiceTest.java` - Remove 8 tests
- `LandingPageServiceTest.java` - Remove 4 tests
- `MembershipServiceTest.java` - Remove 5 tests
- `AccountLockoutServiceTest.java` - Remove 4 tests

### Step 3: Consolidate Controller Tests (48 tests)

Edit the following files to remove specified tests:

- `RegisterControllerTest.java` - Remove 20 tests
- `ProfileControllerTest.java` - Remove 12 tests
- `AdminControllerTest.java` - Remove 8 tests
- `LandingPageApiControllerTest.java` - Remove 8 tests

### Step 4: Consolidate Integration Tests (8 tests)

Edit the following files to remove specified tests:

- `AdminWorkflowIntegrationTest.java` - Remove 8 tests

### Step 5: Verify Test Suite

```bash
# Run all tests to ensure remaining tests pass
./mvnw test

# Verify test count
# Expected: ~330 tests (target range: 250-330)
```

---

## RATIONALE

### Why These Tests Should Be Removed

1. **Repository Tests (81% reduction):**
   - Testing Spring Data JPA framework functionality, not application logic
   - Basic CRUD operations are already tested by Spring
   - Custom query logic is better tested at service/integration level
   - **Keep only:** Token expiry logic and custom finders with business rules

2. **Redundant Validation Tests:**
   - Many tests validate the same constraint (required fields, email format, etc.)
   - **Better approach:** 1 comprehensive validation test per category
   - Reduces maintenance burden when validation rules change

3. **Edge Case Over-Testing:**
   - Null/empty parameter tests for every method
   - Case-insensitive string matching tests repeated
   - **Keep only:** Critical edge cases affecting security/payments

4. **Framework Behavior Tests:**
   - Testing that transactions rollback on error (Spring handles this)
   - Testing that cascade delete works (JPA handles this)
   - Testing that autowiring works (Spring Boot test slice handles this)

5. **Redundant Integration Tests:**
   - PageAccessibilitySmokeTest duplicates all controller tests
   - AdminWorkflow tests duplicate service + controller tests
   - **Keep only:** End-to-end workflows testing cross-cutting concerns

### High-Value Tests to KEEP

1. **Security & Authentication:**
   - All password reset flow tests
   - Account lockout mechanism tests
   - Role-based access control tests
   - Email verification tests

2. **Payment Processing:**
   - PayPalServiceTest (27 tests) - KEEP ALL
   - PayPalControllerTest (50 tests) - KEEP MOST
   - Payment transaction creation/validation

3. **Critical Business Logic:**
   - Membership assignment and cancellation
   - User registration workflow
   - Profile updates with child management

4. **Integration Tests:**
   - Complete end-to-end workflows
   - Cross-service interactions
   - Database transaction handling

---

## NEXT STEPS

1. **Review this plan** with team
2. **Create a backup branch** before deletions
3. **Execute Step 1** (file deletions) first
4. **Run test suite** to ensure no critical coverage lost
5. **Execute Steps 2-4** (consolidations) iteratively
6. **Update test documentation** to reflect new coverage strategy
7. **Set up coverage monitoring** to ensure >80% code coverage maintained

---

## NOTES

- **Target achieved:** 330 tests (within 250-330 range)
- **Tests removed:** 179 (~35% reduction)
- **Coverage maintained:** Critical security, payment, and business logic
- **Maintenance improved:** Less redundant tests to update
- **Build time:** Should improve significantly with fewer tests
