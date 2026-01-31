# Manual Testing Guide - VOICE Membership System

## Table of Contents

1. [Testing Environment Setup](#testing-environment-setup)
2. [User Registration Testing](#user-registration-testing)
3. [Authentication Testing](#authentication-testing)
4. [Profile Management Testing](#profile-management-testing)
5. [Admin Dashboard Testing](#admin-dashboard-testing)
6. [Password Reset Testing](#password-reset-testing)
7. [Security Testing](#security-testing)
8. [Performance & Load Testing](#performance--load-testing)
9. [Browser Compatibility Testing](#browser-compatibility-testing)
10. [Mobile Responsiveness Testing](#mobile-responsiveness-testing)
11. [Bug Report Template](#bug-report-template)

---

## Testing Environment Setup

### Prerequisites

- Java 21 installed
- MySQL database running
- Application running on http://localhost:8080
- Modern web browsers (Chrome, Firefox, Edge, Safari)
- Test data prepared

### Test Accounts

Create the following test accounts for manual testing:

**Regular User:**

- Email: testuser@example.com
- Password: TestPass123!

**Admin User:**

- Email: admin@example.com
- Password: AdminPass123!

---

## User Registration Testing

### Test Case 1.1: Successful Registration (Free Membership)

**Steps:**

1. Navigate to http://localhost:8080/register
2. Fill in Step 1 - User Details:
   - Name: John Doe
   - Email: john.doe@example.com
   - Password: SecurePass123!
   - Confirm Password: SecurePass123!
   - Phone: 555-123-4567
   - Address: 123 Main Street
   - Postal Code: 12345
3. Click "Next"
4. Skip child information (Step 2) by clicking "Next"
5. Select "Free" membership on Step 3
6. Complete registration

**Expected Results:**

- ✓ Registration completes successfully
- ✓ User is redirected to login page
- ✓ User can log in with new credentials
- ✓ Profile shows correct information

---

### Test Case 1.2: Registration with Child Information

**Steps:**

1. Navigate to registration page
2. Complete Step 1 (User Details)
3. On Step 2, add child information:
   - Child Name: Jane Doe
   - Date of Birth: 01/01/2015
   - Hearing Loss Type: Profound
   - Equipment Type: Cochlear Implant
4. Click "Add Another Child" and add a second child:
   - Child Name: Jack Doe
   - Date of Birth: 06/15/2018
   - Hearing Loss Type: Moderate
   - Equipment Type: Hearing Aid
5. Continue to Step 3 and select membership
6. Complete registration

**Expected Results:**

- ✓ Both children are saved to profile
- ✓ Children information displays correctly in profile
- ✓ Age is calculated correctly

---

### Test Case 1.3: Registration Validation Testing

**Steps to Test Each Validation:**

**Email Validation:**

- Try registering with invalid emails:
  - `notanemail` (Expected: Error message)
  - `missing@domain` (Expected: Error message)
  - `test@` (Expected: Error message)

**Password Validation:**

- Try weak passwords:
  - `weak` (Expected: Error - too short)
  - `password123` (Expected: Error - no uppercase)
  - `PASSWORD123` (Expected: Error - no lowercase)
  - `Password` (Expected: Error - no number)
  - `Password123` (Expected: Error - no special character)

**Duplicate Email:**

- Try registering with existing email (Expected: Error message)

**Password Mismatch:**

- Enter different passwords in Password and Confirm Password fields
- Expected: Error message "Passwords do not match"

---

## Authentication Testing

### Test Case 2.1: Successful Login

**Steps:**

1. Navigate to http://localhost:8080/login
2. Enter valid credentials
3. Click "Login"

**Expected Results:**

- ✓ User is redirected to /profile
- ✓ User sees their dashboard
- ✓ Navigation shows "Logout" option

---

### Test Case 2.2: Failed Login Attempts

**Test Scenarios:**

1. **Wrong Password:**
   - Email: testuser@example.com
   - Password: WrongPassword123!
   - Expected: Error message "Invalid credentials"

2. **Non-existent Email:**
   - Email: nonexistent@example.com
   - Password: AnyPassword123!
   - Expected: Error message

3. **Empty Fields:**
   - Leave email or password blank
   - Expected: Validation error

---

### Test Case 2.3: Session Management

**Steps:**

1. Log in successfully
2. Navigate to profile page
3. Open new tab and try to access http://localhost:8080/profile
4. Close browser and reopen
5. Try to access protected pages

**Expected Results:**

- ✓ Session persists across tabs
- ✓ Session expires after browser close (if configured)
- ✓ Logout clears session properly

---

## Profile Management Testing

### Test Case 3.1: View Profile

**Steps:**

1. Log in as regular user
2. Navigate to /profile
3. Verify all information displayed

**Expected Results:**

- ✓ Name displayed correctly
- ✓ Email displayed correctly
- ✓ Phone number displayed
- ✓ Address displayed
- ✓ Postal code displayed
- ✓ Membership type shown
- ✓ Children list displayed (if any)

---

### Test Case 3.2: Edit Profile

**Steps:**

1. From profile page, click "Edit Profile"
2. Update the following fields:
   - Name: Updated Name
   - Phone: 555-999-8888
   - Address: 456 New Street
   - Postal Code: 54321
3. Click "Save Changes"

**Expected Results:**

- ✓ Profile updates successfully
- ✓ Redirected to profile page
- ✓ Updated information displays correctly
- ✓ Success message shown

---

### Test Case 3.3: Email Update and Session Refresh

**Steps:**

1. Edit profile
2. Change email to newemail@example.com
3. Save changes

**Expected Results:**

- ✓ Email updated in database
- ✓ User session updates with new email
- ✓ User remains logged in
- ✓ Can log in with new email

---

### Test Case 3.4: Child Management

**Add Child:**

1. Click "Add Child"
2. Fill in child information
3. Save

**Edit Child:**

1. Click "Edit" next to child name
2. Modify information
3. Save

**Delete Child:**

1. Click "Delete" next to child name
2. Confirm deletion

**Expected Results:**

- ✓ All operations complete successfully
- ✓ Profile updates immediately
- ✓ Confirmation messages shown

---

## Admin Dashboard Testing

### Test Case 4.1: Access Admin Dashboard

**Steps:**

1. Log in as admin user
2. Navigate to /admin/dashboard

**Expected Results:**

- ✓ Dashboard loads successfully
- ✓ Total user count displayed
- ✓ User list shows all registered users
- ✓ Filter options available

---

### Test Case 4.2: User Filtering

**Test Each Filter:**

**Address Filter:**

1. Enter address in filter: "Main Street"
2. Click "Filter"
3. Verify only users with matching addresses shown

**Age Range Filter:**

1. Set Min Age: 5
2. Set Max Age: 10
3. Filter
4. Verify children in age range

**Hearing Loss Type:**

1. Select "Profound"
2. Filter
3. Verify results

**Date Range:**

1. Set start and end dates
2. Filter
3. Verify users registered in range

---

### Test Case 4.3: Export Users to Excel

**Steps:**

1. From admin dashboard, click "Export Users"
2. File should download

**Verify Excel File Contains:**

- ✓ All user information
- ✓ Children details in separate sheet
- ✓ Proper formatting
- ✓ All columns present

---

### Test Case 4.4: View User Details

**Steps:**

1. From user list, click on a user
2. View detailed information modal/page

**Expected Results:**

- ✓ User details displayed
- ✓ Children information shown
- ✓ Membership details visible
- ✓ Registration date shown

---

## Password Reset Testing

### Test Case 5.1: Complete Password Reset Flow

**Steps:**

1. Log out if logged in
2. Click "Forgot Password?" on login page
3. Enter email: testuser@example.com
4. Click "Send Reset Link"
5. Check email inbox
6. Click reset link in email
7. Enter new password: NewPass123!
8. Confirm password: NewPass123!
9. Submit
10. Try logging in with new password

**Expected Results:**

- ✓ Reset email sent
- ✓ Reset link works
- ✓ Password reset page loads
- ✓ Password updates successfully
- ✓ Can log in with new password
- ✓ Old password no longer works

---

### Test Case 5.2: Invalid Email for Reset

**Steps:**

1. Go to forgot password page
2. Enter non-existent email
3. Submit

**Expected Results:**

- ✓ Error message: "No account found with that email address"

---

### Test Case 5.3: Expired/Invalid Token

**Steps:**

1. Try accessing reset password URL with invalid token
2. Try using old/expired token

**Expected Results:**

- ✓ Error message shown
- ✓ Redirect to forgot password page

---

## Security Testing

### Test Case 6.1: Authorization Testing

**Test Unauthorized Access:**

1. **Without Login:**
   - Try accessing /profile (Expected: Redirect to login)
   - Try accessing /admin/dashboard (Expected: Redirect to login)

2. **Regular User accessing Admin:**
   - Log in as regular user
   - Try accessing /admin/dashboard
   - Expected: 403 Forbidden or redirect

---

### Test Case 6.2: CSRF Protection

**Steps:**

1. Open browser developer tools
2. Try submitting forms without CSRF token
3. Try modifying CSRF token

**Expected Results:**

- ✓ Forms without token are rejected
- ✓ Invalid tokens are rejected

---

### Test Case 6.3: SQL Injection Testing

**Test in All Input Fields:**

- `' OR '1'='1`
- `'; DROP TABLE users; --`
- `<script>alert('xss')</script>`

**Expected Results:**

- ✓ All malicious inputs sanitized
- ✓ No SQL errors shown
- ✓ No script execution

---

### Test Case 6.4: Session Security

**Steps:**

1. Log in and note session cookie
2. Try modifying session cookie
3. Try accessing with expired session
4. Test logout completely clears session

---

## Performance & Load Testing

### Test Case 7.1: Page Load Times

**Measure load times for:**

- Landing page
- Login page
- Registration page
- Profile page
- Admin dashboard

**Acceptable Times:**

- < 2 seconds for simple pages
- < 3 seconds for data-heavy pages

---

### Test Case 7.2: Concurrent Users

**Steps:**

1. Have multiple team members log in simultaneously
2. Perform various operations at the same time
3. Monitor system performance

**Expected Results:**

- ✓ No errors occur
- ✓ Response times remain acceptable
- ✓ Data consistency maintained

---

### Test Case 7.3: Large Dataset Handling

**Steps:**

1. Add 100+ users to database
2. Add multiple children per user
3. Access admin dashboard
4. Test filtering and export

**Expected Results:**

- ✓ Dashboard loads without issues
- ✓ Filtering works correctly
- ✓ Export completes successfully

---

## Browser Compatibility Testing

### Test Case 8.1: Cross-Browser Testing

**Test on Each Browser:**

- Google Chrome (latest)
- Mozilla Firefox (latest)
- Microsoft Edge (latest)
- Safari (latest - Mac/iOS)

**Test All Key Features:**

- Registration
- Login
- Profile editing
- Child management
- Admin dashboard

**Expected Results:**

- ✓ All features work on all browsers
- ✓ UI displays correctly
- ✓ No console errors

---

## Mobile Responsiveness Testing

### Test Case 9.1: Mobile Device Testing

**Test on Different Screen Sizes:**

- iPhone (375x667, 390x844)
- Android (360x640, 412x915)
- Tablet (768x1024, 1024x768)

**Test Features:**

1. Navigation menu
2. Registration form
3. Profile page
4. Admin dashboard

**Expected Results:**

- ✓ Layout adapts to screen size
- ✓ All buttons are clickable
- ✓ Text is readable
- ✓ Forms are usable
- ✓ No horizontal scrolling

---

### Test Case 9.2: Touch Interface Testing

**Steps:**

1. Test on actual mobile device
2. Verify all touch interactions work:
   - Tapping buttons
   - Filling forms
   - Selecting dropdowns
   - Scrolling

---

## Ad-Hoc Testing Scenarios

### Exploratory Testing Checklist

**General Navigation:**

- [ ] Click all navigation links
- [ ] Use browser back/forward buttons
- [ ] Try bookmarking pages
- [ ] Refresh pages at various points
- [ ] Test with slow internet connection

**Error Scenarios:**

- [ ] Fill forms with unusual characters
- [ ] Submit empty forms
- [ ] Rapidly click submit buttons
- [ ] Try accessing deleted resources
- [ ] Test with JavaScript disabled

**Edge Cases:**

- [ ] Very long names (100+ characters)
- [ ] Special characters in all fields
- [ ] Past dates where future expected
- [ ] Future dates where past expected
- [ ] Extremely large/small numbers

**User Workflows:**

- [ ] Register → Edit Profile → Add Child → Delete Child
- [ ] Forgot Password → Reset → Login with new password
- [ ] Register with Premium → Check payment flow
- [ ] Admin: View all users → Filter → Export → View details

---

## Bug Report Template

When you find a bug, use this template:

```markdown
### Bug Title

Brief description of the issue

### Severity

- [ ] Critical (Blocks major functionality)
- [ ] High (Important feature broken)
- [ ] Medium (Minor feature issue)
- [ ] Low (Cosmetic or minor inconvenience)

### Environment

- Browser: [Chrome/Firefox/etc.]
- OS: [Windows/Mac/Linux]
- Screen Size: [Desktop/Mobile/Tablet]
- User Type: [Admin/Regular User/Guest]

### Steps to Reproduce

1. Step 1
2. Step 2
3. Step 3

### Expected Behavior

What should happen

### Actual Behavior

What actually happens

### Screenshots/Videos

[Attach screenshots if applicable]

### Console Errors

[Include any browser console errors]

### Additional Notes

Any other relevant information
```

---

## Testing Checklist Summary

### Before Each Testing Session:

- [ ] Database is in known state
- [ ] Application is running
- [ ] Test accounts are available
- [ ] Browser cache cleared

### After Each Testing Session:

- [ ] Log all bugs found
- [ ] Document any unclear behaviors
- [ ] Note performance issues
- [ ] Clean up test data (if needed)

---

## Test Coverage Matrix

| Feature                | Unit Tests | Integration Tests | Manual Tests | Status |
| ---------------------- | ---------- | ----------------- | ------------ | ------ |
| User Registration      | ✓          | ✓                 | Required     |        |
| Login/Authentication   | ✓          | ✓                 | Required     |        |
| Profile Management     | ✓          | ✓                 | Required     |        |
| Child Management       | ✓          | ✓                 | Required     |        |
| Admin Dashboard        | ✓          | ✓                 | Required     |        |
| Password Reset         | ✓          | ✓                 | Required     |        |
| Membership Selection   | ✓          | ✓                 | Required     |        |
| Data Export            | ✓          | -                 | Required     |        |
| Email Notifications    | -          | -                 | Required     |        |
| Security/Authorization | ✓          | ✓                 | Required     |        |

---

## Notes for Testers

1. **Test Data:** Use realistic test data that represents actual user scenarios
2. **Browser DevTools:** Keep browser developer tools open to catch console errors
3. **Network Tab:** Monitor network requests for errors or slow responses
4. **Documentation:** Document everything, even if it seems minor
5. **Communication:** Report issues immediately to development team
6. **Retesting:** Always retest fixed bugs to verify the fix
7. **Regression:** Test related features when one area is fixed
8. **User Perspective:** Think like an actual user, try unexpected actions

---

## Test Execution Schedule

### Day 1: Smoke Testing

- Basic navigation
- User registration
- Login/Logout
- Profile viewing

### Day 2: Feature Testing

- Complete registration flow
- Profile editing
- Child management
- Password reset

### Day 3: Admin Testing

- Admin dashboard
- User filtering
- Data export
- User details

### Day 4: Security & Performance

- Authorization testing
- Security vulnerabilities
- Performance testing
- Load testing

### Day 5: Cross-Platform

- Browser compatibility
- Mobile responsiveness
- Touch interface testing

### Day 6: Ad-Hoc & Regression

- Exploratory testing
- Edge cases
- Regression testing of bug fixes

---

## Contact Information

**For Testing Questions:**

- Development Team Lead: [Name]
- QA Lead: [Name]
- Product Owner: [Name]

**Bug Reporting:**

- Issue Tracker: [Link]
- Email: [Email]
- Slack Channel: [Channel]

---

_Last Updated: [Date]_
_Version: 1.0_
