# Footer Management Guide

## Solution Overview
Your footer is now **centralized** in a single Thymeleaf fragment file. Any changes made there will automatically apply to all pages across your entire application.

## Footer Fragment Location
**File:** `src/main/resources/templates/fragments/footer.html`

## Current Footer Content
```html
<footer th:fragment="footer" class="site-footer" data-testid="site-footer">
    <div class="container text-center">
        <p class="mb-0">
            © 2026 VOICE for Children who are Deaf and Hard of Hearing. All Rights Reserved.
        </p>
    </div>
</footer>
```

## How to Update Footer
Simply edit `fragments/footer.html` and your changes will appear on all pages immediately. Examples:

### Change copyright year
```html
© 2027 VOICE for Children who are Deaf and Hard of Hearing. All Rights Reserved.
```

### Add contact info
```html
<footer th:fragment="footer" class="site-footer">
    <div class="container text-center">
        <p class="mb-0">
            © 2026 VOICE for Children who are Deaf and Hard of Hearing. All Rights Reserved.
        </p>
        <p class="small text-muted mt-2">
            <a href="/contact">Contact Us</a> | <a href="/privacy">Privacy Policy</a>
        </p>
    </div>
</footer>
```

## Updated Files (10 total)
The following HTML files now use the footer fragment:

1. ✅ `profile.html`
2. ✅ `admin.html`
3. ✅ `admin-add-admin.html`
4. ✅ `admin-add-member.html`
5. ✅ `admin-edit-member.html`
6. ✅ `admin-edit-memberships.html`
7. ✅ `admin-landing-page.html`
8. ✅ `admin-renewal-email.html`
9. ✅ `upgrade-membership.html`
10. ✅ `admin-notifications.html`

## How the Fragment Works
Each updated file now includes:
```html
<div th:insert="~{fragments/footer}"></div>
```

This Thymeleaf directive tells Spring Boot to:
1. Find the `footer.html` file in the `fragments/` folder
2. Look for the fragment named `footer` (defined by `th:fragment="footer"`)
3. Insert that content at that exact location

## If You Need to Add Footer to Other Pages
Simply add this line before the closing `</body>` tag:
```html
<div th:insert="~{fragments/footer}"></div>
```

## Best Practices
✓ Keep footer changes in `fragments/footer.html` only  
✓ Use Thymeleaf expressions to dynamically add content  
✓ Test changes in one browser before deploying  
✓ Keep footer styling in `main.css` (don't add inline styles)  

## Related CSS Classes
The footer uses these CSS classes (in `main.css`):
- `.site-footer` - Main footer styling
- `.site-footer p` - Footer text color and sizing
