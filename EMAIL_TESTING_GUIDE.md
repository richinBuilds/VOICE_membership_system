# Email Verification Testing Guide
## .env changed

## Current Email Configuration

Your application is configured to send verification emails using Gmail SMTP:
- **Host**: smtp.gmail.com
- **Port**: 587
- **Username**: rexwinston916@gmail.com
- **Password**: App Password (vgci qakw qkrk enlh)

## How Email Verification Works

1. User completes registration (steps 1-4)
2. System creates a verification token
3. Email is sent to user's email with verification link: `http://localhost:8080/register/verify?token=<UUID>`
4. User clicks link to verify their account

## Testing Methods

### Method 1: Test with Real Email (Current Setup)

**Prerequisites:**
- The Gmail account `rexwinston916@gmail.com` must have "App Passwords" enabled
- The app password `vgci qakw qkrk enlh` must be valid

**Steps:**
1. Start the application (locally or with Docker)
2. Go to http://localhost:8080/register
3. Complete the registration with **your own email address**
4. Check your inbox for "Verify Your Email - VOICE Membership"
5. Click the verification link in the email

**To verify it's working:**
```powershell
# Watch the application logs for email sending
docker logs -f voice-app | Select-String "email|verification"
```

### Method 2: Use a Test Email Service (Recommended for Development)

Use **Mailtrap** or **MailHog** to capture emails without sending them to real addresses.

#### Option A: Mailtrap (Cloud-based)
1. Sign up at https://mailtrap.io (free tier available)
2. Get your SMTP credentials from Mailtrap inbox
3. Update `application.yaml` or use environment variables:

```yaml
spring:
  mail:
    host: smtp.mailtrap.io
    port: 2525
    username: your-mailtrap-username
    password: your-mailtrap-password
```

#### Option B: MailHog (Local Docker)
1. Add to `docker-compose.yaml`:

```yaml
  mailhog:
    image: mailhog/mailhog
    container_name: voice-mailhog
    ports:
      - "1025:1025"  # SMTP server
      - "8025:8025"  # Web UI
```

2. Update app environment in `docker-compose.yaml`:

```yaml
  app:
    environment:
      - SPRING_MAIL_HOST=mailhog
      - SPRING_MAIL_PORT=1025
      - SPRING_MAIL_USERNAME=
      - SPRING_MAIL_PASSWORD=
```

3. Access MailHog UI at http://localhost:8025 to see all sent emails

### Method 3: Add Logging to Track Email Sending

Add this to your application to log when emails are sent:

1. Enable debug logging in `application.yaml`:
```yaml
logging:
  level:
    org:
      springframework:
        mail: DEBUG
```

2. Check logs for email activity:
```powershell
# For Docker
docker logs voice-app | Select-String "mail|email" -Context 2

# For local
# Check console output
```

### Method 4: Create a Test Endpoint

Add a test controller to manually trigger email sending:

```java
@RestController
@RequestMapping("/test")
public class EmailTestController {
    
    @Autowired
    private EmailSenderService emailSenderService;
    
    @GetMapping("/send-verification-email")
    public String testEmail(@RequestParam String email) {
        try {
            String testLink = "http://localhost:8080/register/verify?token=test-token-123";
            emailSenderService.sendVerificationEmail(email, "Test User", testLink);
            return "Email sent successfully to " + email;
        } catch (Exception e) {
            return "Failed to send email: " + e.getMessage();
        }
    }
}
```

Then test: http://localhost:8080/test/send-verification-email?email=yourtest@email.com

## Verification Process Testing

### 1. Check if Verification Token is Created

```sql
-- Connect to database
docker exec -it voice-db mysql -uroot -p

USE web_registration;

-- Check verification tokens
SELECT * FROM verification_tokens ORDER BY expiry_date DESC LIMIT 5;
```

### 2. Check if User is Marked as Unverified

```sql
-- Check user verification status
SELECT id, email, first_name, email_verified, email_verification_token 
FROM users 
ORDER BY creation DESC 
LIMIT 5;
```

### 3. Test Verification Link Manually

After registration, you can manually construct and test the verification URL:
```
http://localhost:8080/register/verify?token=<token-from-database>
```

## Common Issues and Troubleshooting

### Issue 1: Gmail Blocking Sign-in
**Error**: "Username and Password not accepted"

**Solutions:**
- Ensure 2-Factor Authentication is enabled on the Gmail account
- Generate an App Password: Google Account → Security → 2-Step Verification → App passwords
- Use the App Password (not regular password) in application.yaml

### Issue 2: Email Not Received
**Check:**
1. Check spam/junk folder
2. Verify SMTP credentials are correct
3. Check application logs for exceptions
4. Test with a simple email client to verify SMTP settings

### Issue 3: Verification Link Not Working
**Debug:**
```bash
# Check if token exists in database
docker exec -it voice-db mysql -uroot -p$DB_PASSWORD web_registration -e \
  "SELECT * FROM verification_tokens WHERE token='<your-token>';"

# Check token expiry
# Tokens expire after 24 hours by default
```

### Issue 4: Port Conflicts in Docker
If using MailHog and port 8025 is taken:
```yaml
ports:
  - "8026:8025"  # Change external port
```

## Quick Test Script

Create `test-email.ps1`:

```powershell
# Test email functionality
Write-Host "Testing Email Verification..." -ForegroundColor Cyan

# 1. Register a new user
$email = "test$(Get-Random)@example.com"
Write-Host "Registering user: $email"

# 2. Check database for token
Start-Sleep -Seconds 2
docker exec -i voice-db mysql -uroot -p$env:DB_PASSWORD web_registration -e `
  "SELECT u.email, v.token, v.expiry_date FROM users u 
   JOIN verification_tokens v ON u.id = v.user_id 
   WHERE u.email = '$email';"

# 3. Check application logs
Write-Host "`nChecking logs for email activity:"
docker logs --tail 20 voice-app | Select-String "email|verification|mail"
```

## Environment Variables for Docker

For production, use environment variables instead of hardcoded credentials:

```yaml
# docker-compose.yaml
app:
  environment:
    - SPRING_MAIL_HOST=smtp.gmail.com
    - SPRING_MAIL_PORT=587
    - SPRING_MAIL_USERNAME=${MAIL_USERNAME}
    - SPRING_MAIL_PASSWORD=${MAIL_PASSWORD}
```

Create `.env` file:
```
MAIL_USERNAME=rexwinston916@gmail.com
MAIL_PASSWORD=vgci qakw qkrk enlh
DB_PASSWORD=Study$2024
```

## Recommended Testing Workflow

1. **Development**: Use MailHog (see all emails locally without sending)
2. **Testing**: Use Mailtrap (safe testing with real email flow)
3. **Production**: Use real Gmail or professional SMTP service

## Next Steps

For better email testing:
1. Add MailHog to your docker-compose setup
2. Create a test endpoint for manual email sending
3. Add comprehensive logging
4. Consider using a professional email service (SendGrid, AWS SES) for production
