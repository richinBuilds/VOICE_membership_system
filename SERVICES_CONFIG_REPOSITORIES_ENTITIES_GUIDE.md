# VOICE Membership System - Detailed Technical Guide

## Services, Config, Repositories, and Entities Explained

This document provides an in-depth explanation of the four core components that power the VOICE Membership System backend.

---

## 🏗️ ARCHITECTURE LAYERS

```
┌─────────────────────────────────────────┐
│  Controllers (HTTP Requests)            │
├─────────────────────────────────────────┤
│  SERVICES (Business Logic)              │  ← What you'll read here
├─────────────────────────────────────────┤
│  REPOSITORIES (Database Access - JPA)   │  ← What you'll read here
├─────────────────────────────────────────┤
│  ENTITIES (Database Models)             │  ← What you'll read here
├─────────────────────────────────────────┤
│  CONFIG (Setup & Configuration)         │  ← What you'll read here
├─────────────────────────────────────────┤
│  MySQL Database                         │
└─────────────────────────────────────────┘
```

---

# 📦 SECTION 1: ENTITIES (Database Models)

Entities are Java classes that map to database tables. Each field in the entity corresponds to a column in the database. They use the `@Entity` annotation from Jakarta Persistence (JPA).

## What are Entities?

- **Purpose**: Represent database tables as Java objects
- **Annotation**: `@Entity` marks a class as a database entity
- **Table Mapping**: `@Table(name = "table_name")` specifies the database table
- **Relationships**: Define how entities relate to each other (One-to-Many, Many-to-One, etc.)

---

## 1.1 USER ENTITY

**File**: `entities/User.java`

**Database Table**: `users`

### Purpose
Stores user account information and membership details for VOICE platform users.

### Key Fields

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private int id;                          // Primary key, auto-incremented

// Personal Information
@Column(name = "first_name")
private String firstName;                // User's first name

@Column(name = "middle_name")
private String middleName;               // Optional middle name

@Column(name = "last_name")
private String lastName;                 // User's last name

private String email;                    // Email address (unique login)
private String password;                 // BCrypt encrypted password
private String phone;                    // Contact phone number
private String address;                  // Street address
private String city;                     // City of residence
private String province;                 // Province/State
private String postal_code;              // Postal code

// Account Information
private String role;                     // "USER" or "ADMIN"
private Date creation;                   // Account creation date

// Membership Information
@Column(name = "membership_start_date")
private Date membershipStartDate;        // When membership began

@Column(name = "membership_expiry_date")
private Date membershipExpiryDate;       // When membership expires

// Relationships
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Child> children;            // List of children linked to user

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "membership_id")
private Membership membership;           // Current membership plan

@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private Cart cart;                       // Shopping cart for purchases
```

### Database Schema
```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(100),
    middle_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    phone VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    province VARCHAR(100),
    postal_code VARCHAR(20),
    role VARCHAR(50),
    creation DATETIME,
    membership_start_date DATE,
    membership_expiry_date DATE,
    membership_id INT,
    FOREIGN KEY (membership_id) REFERENCES membership_options(id)
);
```

### Relationships

```
User (1) ──────→ (Many) Child
User (1) ──────→ (1) Cart
User (Many) ──→ (1) Membership
```

---

## 1.2 MEMBERSHIP ENTITY

**File**: `entities/Membership.java`

**Database Table**: `membership_options`

### Purpose
Defines different membership plans available to users (e.g., Free, Premium).

### Key Fields

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private int id;                          // Primary key

@Column(nullable = false)
private String name;                     // "Free" or "Premium"

@Column(columnDefinition = "TEXT")
private String description;              // Plan details (e.g., "Premium membership for families")

@Column(columnDefinition = "DECIMAL(10, 2)")
private BigDecimal price;                // Annual cost (0.00 for free, 20.00 for premium)

@Column(columnDefinition = "TEXT")
private String features;                 // Features included (e.g., "Access to all resources")

@Column(name = "is_free")
private boolean isFree;                  // true for Free plan, false for Premium

@Column(name = "display_order")
private int displayOrder;                // Order to display on landing page (1, 2, 3...)

private boolean active;                  // Whether this plan is available for purchase
```

### Example Data

```
ID | Name    | Price | Is_Free | Active | Display_Order
1  | Free    | 0.00  | true    | true   | 1
2  | Premium | 20.00 | false   | true   | 2
```

### Usage in Application
- **Registration Step 3**: User selects a membership plan
- **Landing Page**: Displays available memberships
- **Cart**: Items reference membership plans
- **User Profile**: Shows user's current membership

---

## 1.3 CHILD ENTITY

**File**: `entities/Child.java`

**Database Table**: `children`

### Purpose
Stores information about children associated with user accounts. Users can add multiple children.

### Key Fields

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private int id;                          // Primary key

@Column(nullable = false)
private String name;                     // Child's full name

private Integer age;                     // Child's age

@Column(name = "date_of_birth")
@Temporal(TemporalType.DATE)
private Date dateOfBirth;                // Child's birth date

@Column(name = "hearing_loss_type")
private String hearingLossType;          // Type of hearing loss
                                         // (e.g., "Sensorineural", "Conductive")

@Column(name = "equipment_type")
private String equipmentType;            // Hearing aid type
                                         // (e.g., "Cochlear Implant", "Hearing Aid")

@Column(name = "siblings_names")
private String siblingsNames;            // Names of other children in family

@Column(name = "chapter_location")
private String chapterLocation;          // Local VOICE chapter location

// Relationship
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;                       // Parent user reference
```

### Database Schema
```sql
CREATE TABLE children (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    age INT,
    date_of_birth DATE,
    hearing_loss_type VARCHAR(100),
    equipment_type VARCHAR(100),
    siblings_names VARCHAR(255),
    chapter_location VARCHAR(255),
    user_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### Relationship
```
User (1) ──────→ (Many) Child
```

One user can have multiple children. When a user is deleted, all their children are automatically deleted (cascade).

---

## 1.4 CART & CART ITEM ENTITIES

**File**: `entities/Cart.java` and `entities/CartItem.java`

**Database Tables**: `carts`, `cart_items`

### Cart Entity

```java
@Entity
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;                   // One cart per user

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems;    // Items in cart

    private BigDecimal totalPrice;       // Total cart value
    private Date createdDate;            // When cart was created
    private Date updatedDate;            // Last updated
}
```

### CartItem Entity

```java
@Entity
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;                   // Which cart this item belongs to

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id")
    private Membership membership;       // Which membership is being purchased

    private int quantity;                // Number of memberships (usually 1)
    private BigDecimal price;            // Price at time of purchase
}
```

### Relationships
```
Cart (1) ──────→ (1) User
Cart (1) ──────→ (Many) CartItem
CartItem (Many) → (1) Membership
```

---

## 1.5 OTHER ENTITIES

### LandingPageContent
```java
@Entity
@Table(name = "landing_page_content")
public class LandingPageContent {
    @Id
    private int id;
    
    private String title;               // Section title
    private String description;         // Section description
    private String imageUrl;            // Image for section
    private int displayOrder;           // Order on page
    private boolean active;             // Whether to display
}
```
**Purpose**: Stores dynamic content for the landing page (hero banner, features, testimonials)

### MembershipBenefit
```java
@Entity
@Table(name = "membership_benefits")
public class MembershipBenefit {
    @Id
    private int id;
    
    private String title;               // Benefit title
    private String description;         // Benefit description
    private String icon;                // Icon name/class
    private int displayOrder;           // Order on landing page
    private boolean active;             // Whether to display
}
```
**Purpose**: Stores benefits of membership displayed on landing page

### Role
```java
public enum Role {
    USER,
    ADMIN
}
```
**Purpose**: Defines available user roles for authorization

---

# 🗄️ SECTION 2: REPOSITORIES (Database Access Layer)

Repositories handle all database operations using Spring Data JPA. They abstract database queries so you don't need to write SQL.

## What are Repositories?

- **Purpose**: Data access objects that query the database
- **Framework**: Spring Data JPA (automatic query generation)
- **Benefit**: No SQL needed - methods are auto-implemented
- **Pattern**: Extends `JpaRepository<Entity, IdType>`

---

## 2.1 USER REPOSITORY

**File**: `repositories/UserRepository.java`

```java
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // Find user by exact email
    User findByEmail(String email);

    // Find user by email (case-insensitive)
    User findByEmailIgnoreCase(String email);

    // Find all users with matching email (returns list)
    List<User> findAllByEmailIgnoreCase(String email);
}
```

### Methods Explained

| Method | Query | Purpose |
|--------|-------|---------|
| `findByEmail("email@test.com")` | `SELECT * FROM users WHERE email = ?` | Find specific user for login |
| `findByEmailIgnoreCase("Email@Test.com")` | `SELECT * FROM users WHERE LOWER(email) = LOWER(?)` | Flexible email search |
| `save(user)` | `INSERT INTO users VALUES (...)` | Create new user |
| `update(user)` | `UPDATE users SET ... WHERE id = ?` | Update user info |
| `delete(user)` | `DELETE FROM users WHERE id = ?` | Delete user account |
| `findAll()` | `SELECT * FROM users` | Get all users |
| `findById(1)` | `SELECT * FROM users WHERE id = 1` | Get user by ID |

### Usage Example

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);  // Database query
    }

    public void createUser(User user) {
        userRepository.save(user);                 // Save to database
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();           // Get all from database
    }
}
```

---

## 2.2 CHILD REPOSITORY

**File**: `repositories/ChildRepository.java`

```java
@Repository
public interface ChildRepository extends JpaRepository<Child, Integer> {
    // Find all children for a specific user object
    List<Child> findByUser(User user);

    // Find all children for a specific user by user ID
    List<Child> findByUserId(int userId);
}
```

### Methods Explained

| Method | Query | Purpose |
|--------|-------|---------|
| `findByUser(user)` | `SELECT * FROM children WHERE user_id = ?` | Get all children for logged-in user |
| `findByUserId(5)` | `SELECT * FROM children WHERE user_id = 5` | Get all children for user ID 5 |
| `save(child)` | `INSERT INTO children VALUES (...)` | Add new child |
| `delete(child)` | `DELETE FROM children WHERE id = ?` | Remove child |

### Usage Example

```java
@Controller
public class ProfileController {
    @Autowired
    private ChildRepository childRepository;

    @GetMapping("/profile")
    public String viewProfile(Authentication auth, Model model) {
        User user = userRepository.findByEmail(auth.getName());
        
        // Get all children for this user
        List<Child> children = childRepository.findByUser(user);
        
        model.addAttribute("children", children);
        return "profile";
    }
}
```

---

## 2.3 MEMBERSHIP REPOSITORY

**File**: `repositories/MembershipRepository.java`

```java
@Repository
public interface MembershipRepository extends JpaRepository<Membership, Integer> {
    // Find all active membership plans
    List<Membership> findByActiveTrue();

    // Find all active memberships ordered for display
    List<Membership> findByActiveTrueOrderByDisplayOrderAsc();

    // Find only free or only paid memberships
    List<Membership> findByIsFree(boolean isFree);
}
```

### Methods Explained

| Method | Query | Purpose |
|--------|-------|---------|
| `findByActiveTrue()` | `SELECT * FROM membership_options WHERE active = true` | Get all available plans |
| `findByActiveTrueOrderByDisplayOrderAsc()` | `SELECT * FROM membership_options WHERE active = true ORDER BY display_order` | Get plans for landing page display |
| `findByIsFree(true)` | `SELECT * FROM membership_options WHERE is_free = true` | Get only free plans |
| `findByIsFree(false)` | `SELECT * FROM membership_options WHERE is_free = false` | Get only paid plans |

### Usage Example

```java
@Service
public class LandingPageService {
    @Autowired
    private MembershipRepository membershipRepository;

    public List<Membership> getActiveMemberships() {
        // Automatically ordered for display
        return membershipRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }
}
```

---

## 2.4 CART REPOSITORIES

**File**: `repositories/CartRepository.java`

```java
@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Cart findByUserId(int userId);
    Cart findByUser(User user);
}
```

**File**: `repositories/CartItemRepository.java`

```java
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByCartId(int cartId);
    List<CartItem> findByCart(Cart cart);
}
```

---

## 2.5 OTHER REPOSITORIES

### LandingPageContentRepository
```java
@Repository
public interface LandingPageContentRepository extends JpaRepository<LandingPageContent, Integer> {
    List<LandingPageContent> findByActiveTrueOrderByDisplayOrderAsc();
}
```
**Purpose**: Query dynamic landing page content

### MembershipBenefitRepository
```java
@Repository
public interface MembershipBenefitRepository extends JpaRepository<MembershipBenefit, Integer> {
    List<MembershipBenefit> findByActiveTrueOrderByDisplayOrderAsc();
}
```
**Purpose**: Query membership benefits for display

---

# ⚙️ SECTION 3: SERVICES (Business Logic)

Services contain the business logic and orchestrate repositories. They handle complex operations that involve multiple entities.

## What are Services?

- **Purpose**: Execute business logic and coordinate database operations
- **Annotation**: `@Service` marks a class as a service
- **Pattern**: Controllers call services, services call repositories
- **Responsibility**: Complex logic, validation, transactions

---

## 3.1 USER SERVICE

**File**: `services/UserService.java`

### Purpose
Manages user-related operations and implements Spring Security authentication.

### Key Methods

#### 1. **loadUserByUsername(String email)**
```java
@Override
public UserDetails loadUserByUsername(String email) {
    User user = userRepository.findByEmail(email);
    if (user != null) {
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }
    throw new UsernameNotFoundException("User not found with email: " + email);
}
```

**What it does:**
- Called by Spring Security during login
- Finds user by email in database
- Returns UserDetails object for Spring Security
- Throws exception if user not found

**When called:**
- User enters email/password on login form
- Spring Security calls this to load user details

**Example flow:**
```
User fills login form
    ↓
Spring Security intercepts request
    ↓
Calls loadUserByUsername("user@email.com")
    ↓
Service queries: SELECT * FROM users WHERE email = "user@email.com"
    ↓
Returns UserDetails with email, password, role
    ↓
Spring Security validates password with BCrypt
    ↓
If valid → Create session → Redirect to dashboard
If invalid → Show error → Redirect to login
```

#### 2. **sendPasswordResetEmail(String email)**
```java
public boolean sendPasswordResetEmail(String email) {
    // 1. Find user by email
    User user = userRepository.findByEmail(email);
    if (user == null) {
        return false;  // User not found
    }
    
    // 2. Generate unique token
    String token = java.util.UUID.randomUUID().toString();
    
    // 3. Store token in memory (maps token → email)
    resetTokens.put(token, user.getEmail());
    
    // 4. Create reset link with token
    String resetLink = "http://localhost:8080/reset-password?token=" + token;
    
    // 5. Send email with link
    if (emailSenderService != null) {
        emailSenderService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }
    
    return true;
}
```

**What it does:**
- User enters email on "Forgot Password" page
- Service generates unique token
- Stores token temporarily
- Sends email with reset link

**Example flow:**
```
User clicks "Forgot Password"
    ↓
Enters email: user@example.com
    ↓
Service finds user in database
    ↓
Generates token: abc123xyz789
    ↓
Stores: {abc123xyz789 → user@example.com}
    ↓
Creates link: http://localhost:8080/reset-password?token=abc123xyz789
    ↓
Sends email with link
    ↓
User clicks link in email
    ↓
Token validated → Shows password reset form
```

#### 3. **resetPassword(String token, String newPassword)**
```java
public boolean resetPassword(String token, String newPassword) {
    // 1. Look up email from token
    String email = resetTokens.get(token);
    if (email == null) {
        return false;  // Invalid/expired token
    }
    
    // 2. Find user by email
    User user = userRepository.findByEmail(email);
    if (user == null) {
        return false;
    }
    
    // 3. Encrypt new password with BCrypt
    user.setPassword(passwordEncoder.encode(newPassword));
    
    // 4. Update user in database
    userRepository.save(user);
    
    // 5. Delete token (one-time use)
    resetTokens.remove(token);
    
    return true;
}
```

**What it does:**
- User submits new password on reset form
- Service validates token
- Encrypts password with BCrypt
- Updates user in database
- Invalidates token (can't be reused)

**Security considerations:**
- Token is single-use only
- Password is encrypted before storing
- Tokens expire (in production, add expiration time)
- No plain-text passwords stored

---

## 3.2 EMAIL SENDER SERVICE

**File**: `services/EmailSenderService.java`

### Purpose
Handles sending emails using SMTP and Thymeleaf templates.

### Key Methods

#### 1. **sendPasswordResetEmail(String to, String resetLink)**
```java
public void sendPasswordResetEmail(String to, String resetLink) {
    // 1. Create MIME message (supports HTML)
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    
    try {
        // 2. Create helper for easy message setup
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject("Password Reset Request");

        // 3. Process Thymeleaf template
        Context context = new Context();
        context.setVariable("resetLink", resetLink);
        String htmlContent = templateEngine.process("reset-password-email", context);
        
        // 4. Set HTML content
        helper.setText(htmlContent, true);

        // 5. Send the email
        mailSender.send(mimeMessage);
    } catch (MessagingException e) {
        throw new RuntimeException("Failed to send password reset email", e);
    }
}
```

**What it does:**
- Creates HTML email with password reset link
- Uses Thymeleaf template (dynamic content)
- Sends via SMTP server
- Handles errors gracefully

**HTML Template** (`templates/reset-password-email.html`):
```html
<!DOCTYPE html>
<html>
<body>
    <h1>Password Reset Request</h1>
    <p>Click the link below to reset your password:</p>
    <a href="[[${resetLink}]]">Reset Password</a>
    <p>This link expires in 24 hours.</p>
</body>
</html>
```

**SMTP Configuration** (in `application.yaml`):
```yaml
spring:
  mail:
    host: smtp.gmail.com          # Email provider SMTP server
    port: 587
    username: ${MAIL_USERNAME}    # Your email
    password: ${MAIL_PASSWORD}    # App-specific password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 2. **sendMembershipUpgradeConfirmation(...)**
```java
public void sendMembershipUpgradeConfirmation(
        String to, 
        String userName, 
        String membershipName,
        String expiryDate) {
    
    // Create email with membership details
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    try {
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject("Membership Upgrade Successful - VOICE");

        // Build HTML email content inline
        String htmlContent = "<html><body>" +
                "<h2>Congratulations " + userName + "!</h2>" +
                "<p>Your membership has been upgraded to <strong>" + membershipName + "</strong>.</p>" +
                "<ul>" +
                "<li>Status: Active/Paid</li>" +
                "<li>Expiry Date: " + expiryDate + "</li>" +
                "</ul>" +
                "<p>Thank you for choosing VOICE!</p>" +
                "</body></html>";

        helper.setText(htmlContent, true);
        mailSender.send(mimeMessage);
    } catch (MessagingException e) {
        e.printStackTrace();
    }
}
```

**What it does:**
- Sends confirmation email after membership purchase
- Includes membership details
- Sends via SMTP server

---

## 3.3 LANDING PAGE SERVICE

**File**: `services/LandingPageService.java`

### Purpose
Initializes and manages landing page content, memberships, and benefits.

### Key Methods (Initialization)

```java
@Service
public class LandingPageService {

    public void initializeDefaultMemberships() {
        // Runs on application startup
        // Creates Free and Premium membership plans if they don't exist
    }

    public void initializeDefaultContent() {
        // Creates default landing page sections
        // (Hero banner, features, testimonials, etc.)
    }

    public void initializeDefaultBenefits() {
        // Creates default membership benefits
        // Displayed on landing page
    }
}
```

**When called:**
- Application starts up
- `ApplicationStartup` bean calls these methods
- Data only created if it doesn't already exist

---

# ⚙️ SECTION 4: CONFIGURATION (Setup & Initialization)

Configuration files set up the application's core infrastructure: security, templates, error handling, and startup tasks.

## What are Configs?

- **Purpose**: Set up frameworks and define application behavior
- **Annotation**: `@Configuration` marks a class for Spring configuration
- **Scope**: Runs once at application startup
- **Role**: Define beans, configure security, setup template engines

---

## 4.1 SECURITY CONFIG

**File**: `config/SecurityConfig.java`

### Purpose
Configures Spring Security for authentication, authorization, and session management.

### Key Configuration: URL Access Rules

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
            .authorizeHttpRequests(auth -> auth
                // PUBLIC ROUTES - No login required
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()  // Static files
                .requestMatchers("/").permitAll()                                // Home page
                .requestMatchers("/login").permitAll()                           // Login form
                .requestMatchers("/register/**").permitAll()                     // Registration (all 4 steps)
                .requestMatchers("/forgot-password").permitAll()                 // Forgot password form
                .requestMatchers("/reset-password").permitAll()                  // Password reset
                .requestMatchers("/api/landing-page/**").permitAll()             // Landing page API

                // ADMIN ONLY ROUTES
                .requestMatchers("/admin/**").hasRole("ADMIN")                   // Admin dashboard

                // USER & ADMIN ROUTES
                .requestMatchers("/profile/**").hasAnyRole("USER", "ADMIN")     // User profiles

                // EVERYTHING ELSE REQUIRES LOGIN
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(authenticationSuccessHandler())
                .permitAll()
            )
            .logout(config -> config
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("VOICE_REMEMBER_ME", "JSESSIONID")
            )
            .rememberMe(remember -> remember
                .key("voiceRememberMeKey")
                .tokenValiditySeconds(604800)        // 7 days
                .rememberMeParameter("remember-me")
                .rememberMeCookieName("VOICE_REMEMBER_ME")
                .useSecureCookie(false)              // Set to true in production
                .alwaysRemember(false)
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"))
            .build();
    }
}
```

### How It Works

**Public Routes (No Login):**
```
/login                    → Anyone can access login page
/register/step1           → Anyone can start registration
/forgot-password          → Anyone can request password reset
/css/**, /js/**, /images/** → Static files accessible to everyone
```

**Protected Routes (Login Required):**
```
/profile/**               → Only USER or ADMIN can view profile
/admin/**                 → Only ADMIN can access admin panel
```

**Authentication Flow:**
```
User tries to access /profile
    ↓
Spring Security checks if authenticated
    ↓
If NOT authenticated → Redirect to /login
If authenticated → Check role
    ↓
If role = USER or ADMIN → Allow access
If role = no access → Show 403 Forbidden
```

### Password Encryption

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**How BCrypt works:**
```
User password: "MyPassword123"
    ↓
BCryptPasswordEncoder.encode("MyPassword123")
    ↓
Hashed: $2a$10$slYQmyNdGzin7olVN3p5aO2qQbXqMFwgkVQGgBKkNjR5vHdPXvvhe
    ↓
Stored in database (NOT plain text!)
    ↓
On login:
    User enters: "MyPassword123"
    ↓
BCrypt compares hash → Match!
    ↓
User authenticated
```

**Benefits:**
- One-way encryption (can't decrypt)
- Same password produces different hash each time (random salt)
- Takes time to compute (slows down brute force attacks)
- Industry standard for password security

### Remember Me Feature

```
User checks "Remember me" on login
    ↓
Spring Security creates token
    ↓
Token stored in cookie: VOICE_REMEMBER_ME
    ↓
Cookie valid for 7 days
    ↓
User returns after 3 days
    ↓
Spring Security reads token
    ↓
User automatically authenticated
    ↓
No login required!
```

### Custom Login Success Handler

```java
@Bean
public AuthenticationSuccessHandler authenticationSuccessHandler() {
    return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(grantedAuthority -> 
                grantedAuthority.getAuthority().equals("ROLE_ADMIN")
            );

        if (isAdmin) {
            response.sendRedirect("/admin/dashboard");  // Admin → dashboard
        } else {
            response.sendRedirect("/profile");          // User → profile
        }
    };
}
```

**What it does:**
- After successful login, check user role
- ADMIN users → redirect to admin dashboard
- Regular users → redirect to profile
- Personalized experience for each role type

---

## 4.2 THYMELEAF CONFIG

**File**: `config/ThymeleafConfig.java`

### Purpose
Configures Thymeleaf template engine for server-side HTML rendering.

```java
@Configuration
public class ThymeleafConfig {
    
    @Bean
    public ClassLoaderTemplateResolver templateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");           // Look for templates in templates/ folder
        resolver.setSuffix(".html");                // All templates are .html files
        resolver.setTemplateMode("HTML");           // Use HTML5 mode
        resolver.setCharacterEncoding("UTF-8");     // Support UTF-8 characters
        resolver.setOrder(1);
        resolver.setCheckExistence(true);
        return resolver;
    }

    @Bean
    public SpringTemplateEngine templateEngine() {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver());
        engine.addDialect(new SpringSecurityDialect());  // Add Spring Security dialect
        engine.addDialect(new Java8TimeDialect());       // Add Java 8 Time support
        return engine;
    }
}
```

### What Thymeleaf Does

**Example HTML with Thymeleaf:**
```html
<h1>Welcome, [[${userName}]]!</h1>

<!-- Loop through children -->
<div th:each="child : ${children}">
    <p th:text="${child.name}"></p>
    <p th:text="${child.age}"></p>
</div>

<!-- Security checks -->
<button sec:authorize="hasRole('ADMIN')">Admin Button</button>

<!-- Conditional display -->
<div th:if="${user.membershipExpiryDate > today}">
    <p>Your membership is active!</p>
</div>
```

**How it works:**
```
Controller returns view + model data
    ↓
Thymeleaf processes HTML template
    ↓
Replaces [[${variable}]] with actual values
    ↓
Loops through th:each collections
    ↓
Checks security permissions
    ↓
Returns HTML to browser
    ↓
Browser displays rendered page
```

---

## 4.3 APPLICATION STARTUP

**File**: `config/ApplicationStartup.java`

### Purpose
Initializes default data when application starts.

```java
@Slf4j
@Component
public class ApplicationStartup implements CommandLineRunner {

    @Autowired
    private LandingPageService landingPageService;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing application data...");

        try {
            // 1. Create default membership plans (Free, Premium)
            landingPageService.initializeDefaultMemberships();
            log.info("Memberships initialized successfully");

            // 2. Create landing page sections (Hero, Features, etc.)
            landingPageService.initializeDefaultContent();
            log.info("Landing page content initialized successfully");

            // 3. Create membership benefits (list of features)
            landingPageService.initializeDefaultBenefits();
            log.info("Benefits initialized successfully");

            log.info("Application startup initialization complete!");
        } catch (Exception e) {
            log.error("Error during startup: {}", e.getMessage(), e);
        }
    }
}
```

### Startup Sequence

```
1. Application starts
2. Spring creates beans (Services, Repositories, Config)
3. CommandLineRunner executes run() method
4. Initialize memberships:
   - Check if Free plan exists → If not, create it
   - Check if Premium plan exists → If not, create it
5. Initialize landing page content:
   - Check if hero banner exists → If not, create it
   - Check if features section exists → If not, create it
6. Initialize membership benefits:
   - Check if benefits exist → If not, create them
7. Application ready to serve requests
```

---

## 4.4 GLOBAL EXCEPTION HANDLER

**File**: `config/GlobalExceptionHandler.java`

### Purpose
Catches exceptions application-wide and returns user-friendly error pages.

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UsernameNotFoundException.class)
    public String handleUsernameNotFound(Exception ex, Model model) {
        model.addAttribute("errorMessage", "User not found");
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        model.addAttribute("errorMessage", "An unexpected error occurred");
        return "error/500";
    }
}
```

**How it works:**
```
Exception thrown anywhere in application
    ↓
GlobalExceptionHandler catches it
    ↓
Checks exception type
    ↓
If UsernameNotFoundException → Show 404 page
If other exception → Show 500 page
    ↓
User sees friendly error instead of stack trace
```

---

# 🔄 COMPLETE DATA FLOW EXAMPLE

## Example: User Registration and Login

```
1. USER REGISTRATION (4-step form)
   ├─ Step 1: Personal info (name, email, password)
   ├─ Step 2: Address & phone
   ├─ Step 3: Select membership (Free or Premium)
   └─ Step 4: Review & submit

2. SUBMISSION FLOW
   └─ RegisterController receives form
      └─ Calls UserService to validate & create user
         └─ UserService calls PasswordEncoder to hash password
            └─ UserService calls UserRepository.save(user)
               └─ Repository executes: INSERT INTO users VALUES (...)
                  └─ User stored in database with encrypted password
                     └─ Show success message

3. USER LOGIN
   └─ User enters email & password on login form
      └─ LoginController receives credentials
         └─ Spring Security intercepts
            └─ Calls UserService.loadUserByUsername(email)
               └─ UserService queries UserRepository.findByEmail(email)
                  └─ Database returns user with encrypted password
                     └─ Spring Security compares passwords (BCrypt)
                        ├─ If match → Create session
                        │  └─ Custom AuthenticationSuccessHandler redirects to profile
                        │  └─ User logged in successfully!
                        └─ If no match → Show error message

4. USER ACCESSES PROFILE
   └─ User navigates to /profile
      └─ Spring Security checks authentication → Allowed
         └─ ProfileController receives request
            └─ Calls ProfileController.viewProfile()
               └─ Retrieves authenticated user from Spring Security
                  └─ Calls ChildRepository.findByUser(user)
                     └─ Database returns all children for this user
                        └─ Calls MembershipRepository.findById(membership_id)
                           └─ Database returns membership details
                              └─ Controller adds to Model
                                 └─ Thymeleaf renders profile.html with user & children data
                                    └─ User sees their profile!
```

---

# 📊 QUICK REFERENCE DIAGRAM

```
REQUEST FLOW:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

User Browser
    ↓ (HTTP Request: POST /register)
    │
Controller Layer
├─ RegisterController
├─ LoginController
├─ ProfileController
├─ AdminController
    ↓ (Process request, validate input)
    │
Service Layer
├─ UserService (business logic)
├─ EmailSenderService (send emails)
├─ LandingPageService (content management)
    ↓ (Call repository methods)
    │
Repository Layer
├─ UserRepository (User queries)
├─ ChildRepository (Child queries)
├─ MembershipRepository (Membership queries)
├─ CartRepository (Cart queries)
    ↓ (Execute SQL queries)
    │
Database Layer
└─ MySQL Database (users, children, memberships, carts, etc.)

RESPONSE FLOW (Reverse):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Database ← Data rows
    ↓
Repository ← Returns entities
    ↓
Service ← Processes data
    ↓
Controller ← Gets results
    ↓
Thymeleaf ← Renders HTML with data
    ↓
Browser ← HTML response displayed
```

---

# 🎯 KEY TAKEAWAYS

## ENTITIES
- Map database tables to Java objects
- Define relationships (One-to-Many, Many-to-One)
- Store data structure information

## REPOSITORIES
- Provide database access without writing SQL
- Extend JpaRepository for CRUD operations
- Use query methods like findByEmail(), findByUser()

## SERVICES
- Contain business logic and complex operations
- Call repositories to access database
- Handle validation and error checking

## CONFIGURATION
- Set up framework behavior (Spring Security, Thymeleaf)
- Run initialization tasks on startup
- Define beans and application-wide rules

---

# 📚 ADDITIONAL RESOURCES

- **Spring Data JPA Docs**: https://spring.io/projects/spring-data-jpa
- **Spring Security Docs**: https://spring.io/projects/spring-security
- **Thymeleaf Docs**: https://www.thymeleaf.org
- **BCrypt**: Why password encryption is important
- **JPA/Hibernate**: Object-Relational Mapping (ORM) framework

