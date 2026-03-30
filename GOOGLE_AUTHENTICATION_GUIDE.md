# Google Authentication Guide

## Overview

The VOICE Membership System implements Google OAuth2 authentication with separate flows for **sign-up** (registration) and **sign-in** (login). This ensures users complete the full registration process (profile details, children information, membership selection, and email verification) before account activation.

---

## Architecture

### Key Components

1. **GoogleOAuth2UserService** - Handles OAuth2 user authentication and account creation
2. **SecurityConfig** - Configures Spring Security with OAuth2 login and error handling
3. **CustomAuthenticationSuccessHandler** - Routes authenticated users based on their status
4. **RegisterController** - Manages the multi-step registration flow

### Session Management

Two flow types are tracked via session attributes:

- `GOOGLE_AUTH_FLOW=signup` - User initiated Google sign-up from register page
- `GOOGLE_AUTH_FLOW` absent - User is attempting Google sign-in from login page

---

## User Flows

### Sign-Up Flow (New Users)

```
1. User clicks "Sign up with Google" on /register page
   ↓
2. RegisterController.startGoogleSignup() sets session flag:
   - GOOGLE_AUTH_FLOW = "signup"
   - Redirects to /oauth2/authorization/google
   ↓
3. Google OAuth2 callback received
   ↓
4. GoogleOAuth2UserService.loadUser():
   - Checks if signup flow via session flag
   - If user doesn't exist: CREATE new account with emailVerified=false
   - Prepares MultiStepRegistrationDto in session
   - Sets GOOGLE_SIGNUP_REDIRECT_STEP2=true and GOOGLE_SIGNUP_USER_ID
   ↓
5. CustomAuthenticationSuccessHandler.onAuthenticationSuccess():
   - Detects GOOGLE_SIGNUP_REDIRECT_STEP2 flag
   - Clears security context (prevents auto-login)
   - Redirects to /register/step2
   ↓
6. User completes registration flow:
   - Step 2: Add child information
   - Step 3: Select membership
   - Step 4: Proceed to checkout (if paid) or finalize
   ↓
7. RegisterController.completeRegistration():
   - Updates pre-created Google user with form data
   - Sets emailVerified=false
   - Creates/refreshes verification token
   - Sends verification email
   - Clears Google signup session markers
   - Redirects to /register/verification-sent
   ↓
8. User receives email with verification link
   ↓
9. User clicks verification link → /register/verify
   ↓
10. Email is marked verified, user can now sign-in with Google
```

### Sign-In Flow (Existing Users)

```
1. User clicks "Sign in with Google" on /login page
   ↓
2. No session flag set (not signup flow)
   ↓
3. GoogleOAuth2UserService.loadUser():
   - Checks signup flow: false (not set)
   - Looks up user by email
   ↓
   IF user doesn't exist:
   - Throws OAuth2AuthenticationException("google_signup_required")
   - SecurityConfig redirects to /login?googleSignupRequired=true
   ↓
   IF user exists but emailVerified=false:
   - Throws OAuth2AuthenticationException("email_unverified")
   - SecurityConfig redirects to /login?unverified=true
   ↓
   IF user exists and emailVerified=true:
   - Updates user profile from Google attributes
   - Returns authenticated OAuth2User
   ↓
4. CustomAuthenticationSuccessHandler:
   - No Google signup markers present
   - Resets failed login attempts
   - Redirects to /profile (USER) or /admin/dashboard (ADMIN)
```

---

## Configuration

### application.yaml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
            scope:
              - openid
              - email
              - profile
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
          provider:
            google:
              user-name-attribute: email
```

### Google Cloud Console Setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new OAuth 2.0 credential (Desktop or Web Application)
3. Add authorized redirect URIs:
   - Local: `http://localhost:8080/login/oauth2/code/google`
   - Production: `https://yourdomain.com/login/oauth2/code/google`
4. Copy Client ID and Client Secret to `application.yaml`

---

## Session Attributes

### Signup Flow Session Keys

| Key | Type | Purpose |
|-----|------|---------|
| `GOOGLE_AUTH_FLOW` | String | Set to `"signup"` to indicate sign-up flow |
| `GOOGLE_SIGNUP_REDIRECT_STEP2` | Boolean | Flag to redirect to step2 instead of auto-login |
| `GOOGLE_SIGNUP_USER_ID` | Integer | ID of pre-created Google user for later update |
| `registrationData` | MultiStepRegistrationDto | Multi-step registration data (child info, membership) |

### Cleanup

All Google signup session attributes are removed after `completeRegistration()`:
```java
session.removeAttribute(GoogleOAuth2UserService.GOOGLE_SIGNUP_USER_ID_SESSION_KEY);
session.removeAttribute(GoogleOAuth2UserService.GOOGLE_SIGNUP_REDIRECT_STEP2_SESSION_KEY);
```

---

## Error Handling

### OAuth2 Error Codes

| Error Code | Scenario | Redirect |
|------------|----------|----------|
| `google_signup_required` | User attempts sign-in without account | `/login?googleSignupRequired=true` |
| `email_unverified` | User sign-in before email verification | `/login?unverified=true` |
| `account_locked` | Account locked due to failed attempts | `/login?locked=true` |
| `invalid_google_account` | No email from Google (rare) | `/login?error=true` |

### User Messages

**Sign-In → No Account:**
```
"No account exists for this Google email. Please sign up first and then use Google to sign in."
```

**Sign-In → Email Not Verified:**
```
"Please verify your email before logging in. Check your inbox for the verification link."
[Resend verification email]
```

**Sign-In → Account Locked:**
```
"Your account has been temporarily locked due to multiple failed login attempts."
```

---

## User Data Mapping

### On Account Creation (from Google OAuth2User)

| Google Attribute | User Field | Required |
|------------------|-----------|----------|
| `email` | `email` | ✓ Yes |
| `given_name` | `firstName` | Falls back to "Google" |
| `family_name` | `lastName` | Falls back to "User" |
| (N/A) | `password` | Random UUID encoded |
| (N/A) | `emailVerified` | `false` (requires verification) |
| (N/A) | `role` | `USER` |

### On User Update (existing account)

- `firstName` updated only if currently null/blank
- `lastName` updated only if currently null/blank
- `emailVerified` **NOT** automatically set to true (must complete registration)

---

## Email Verification

### Verification Flow

1. User completes registration
2. VerificationToken created with random UUID
3. Verification email sent with token link: `/register/verify?token={token}`
4. User clicks link
5. Token validated:
   - Token must exist in database
   - Token must not be expired (24-hour TTL)
6. User's `emailVerified` set to `true`
7. Token deleted
8. User can now sign-in with Google

### Resend Verification Email

Endpoint: `POST /register/resend-verification`
- User provides email
- New token generated (old token deleted)
- Verification email re-sent
- Throttling recommended (not yet implemented)

---

## Security Considerations

### Current Implementation

✓ **Email Verification Required** - Google-created accounts must verify email  
✓ **No Auto-Login on Signup** - Authentication context cleared after signup  
✓ **Session Isolation** - Signup markers prevent accidental auto-login  
✓ **Profile Completion** - Full registration must be completed  
✓ **Account Lockout** - Failed login attempts trigger lockout  

### Recommendations for Production

1. **Enable HTTPS** - Set `useSecureCookie(true)` in SecurityConfig
2. **Add Rate Limiting** - Throttle OAuth2 login attempts
3. **Verify Email Domain** - Optionally restrict to corporate domains
4. **Token Expiration** - Verify 24-hour token TTL is appropriate
5. **Audit Logging** - Log all OAuth2 sign-in/signup attempts
6. **CSRF Protection** - Already enabled in SecurityConfig
7. **Scope Reduction** - Only request `openid email` if profile not needed

---

## Troubleshooting

### "No account found for this Google email"

**Cause:** User tried to sign-in from login page without prior registration.  
**Solution:** Redirect them to `/register` and use the "Sign up with Google" button.

### "Please verify your email"

**Cause:** User completed registration but didn't verify email before signing in.  
**Solution:** Check spam folder, offer resend via `/register/resend-verification`.

### "Invalid verification token"

**Cause:** Token doesn't exist or is expired.  
**Solution:** Generate new token via `/register/resend-verification`.

### Redirect loop to /register/step2

**Cause:** Browser back button after successful signup, or session expired.  
**Solution:** Check session attributes, ensure `registrationData` not null before accessing.

---

## Testing

### Manual Test Cases

#### Test: Google Sign-Up → Step2 Redirect
```
1. Go to /register
2. Click "Sign up with Google"
3. Complete Google OAuth2 flow
4. Verify redirected to /register/step2 (not auto-logged in)
5. Verify pre-filled form with Google names
```

#### Test: Complete Registration → Email Verification
```
1. Complete step2, step3, step4
2. Verify redirected to /register/verification-sent
3. Verify verification email received
4. Click verification link
5. Verify email marked verified
6. Attempt Google sign-in → should succeed
```

#### Test: Google Sign-In Without Account
```
1. Go to /login
2. Click "Sign in with Google"
3. Use new Google account (not previously registered)
4. Verify redirected to /login?googleSignupRequired=true
5. Verify error message shown
6. Verify link to /register present
```

#### Test: Google Sign-In Before Email Verification
```
1. Manually create user with emailVerified=false via database
2. Go to /login
3. Click "Sign in with Google"
4. Use the same Google email
5. Verify redirected to /login?unverified=true
6. Verify error message about email verification
7. Verify resend-verification link present
```

---

## Future Enhancements

1. **Social Account Linking** - Allow users to link existing account to Google
2. **Sign-Up Customization** - Skip unnecessary steps for Google users
3. **Roles from Google Groups** - Assign roles based on Google Workspace groups
4. **Multi-Language** - Support Google's language preference attribute
5. **Account Deletion** - Implement Google data deletion per DPA requirements
6. **Reauthentication Flow** - Require reauthentication for sensitive operations

---

## Related Files

- [SecurityConfig](src/main/java/org/voice/membership/config/SecurityConfig.java) - OAuth2 configuration
- [GoogleOAuth2UserService](src/main/java/org/voice/membership/services/GoogleOAuth2UserService.java) - OAuth2 user handling
- [RegisterController](src/main/java/org/voice/membership/controllers/RegisterController.java) - Registration flow
- [login.html](src/main/resources/templates/login.html) - Sign-in page
- [register.html](src/main/resources/templates/register.html) - Sign-up page
