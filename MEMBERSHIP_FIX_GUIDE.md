# Membership Display Issue Fix

## Problem
When running the application with Docker, the registration step 3 page only shows the Free membership option, not the Premium (paid) membership option.

## Root Cause
The original membership initialization logic only checked if the membership table was empty (`count() == 0`). If the database persisted between Docker runs with corrupted or incomplete data, it wouldn't re-create missing memberships.

## Solution Applied

### 1. Enhanced Membership Initialization
Modified `LandingPageService.initializeDefaultMemberships()` to:
- Check for Free and Premium memberships individually by name
- Create any missing membership type
- Ensure existing memberships are set to active
- Add detailed logging to track initialization status

### 2. Added Repository Methods
Added to `MembershipRepository`:
```java
List<Membership> findByNameAndIsFreeTrue(String name);
List<Membership> findByNameAndIsFreeFalse(String name);
```

### 3. Docker Volume Configuration
Added persistent volume to `docker-compose.yaml` for the database to maintain data consistency across restarts.

## How to Fix Your Running System

### Option 1: Fresh Start (Recommended)
```bash
# Stop and remove all containers and volumes
docker-compose down -v

# Rebuild and start
docker-compose up --build
```

### Option 2: Keep Existing Data
If you want to keep existing user data:

```bash
# Stop the application
docker-compose down

# Rebuild and start
docker-compose up --build
```

The enhanced initialization will detect the missing Premium membership and create it automatically.

### Option 3: Manual Database Fix
If you want to manually verify/fix the database:

```bash
# Connect to the MySQL container
docker exec -it voice-db mysql -u root -p

# Use the database
USE web_registration;

# Check existing memberships
SELECT id, name, is_free, active, price, display_order FROM membership_options;

# If Premium membership is missing, it will be created automatically on next startup
# If Premium membership exists but is inactive, activate it:
UPDATE membership_options SET active = 1 WHERE name = 'Premium';

# Exit MySQL
exit;
```

## Verification

After restarting, check the logs for these messages:
```
Initializing default memberships...
Creating Premium membership...
Premium membership created successfully
Total active memberships: 2
  - Free (isFree=true, active=true, price=null)
  - Premium (isFree=false, active=true, price=20.00)
```

Then test:
1. Navigate to registration step 3: http://localhost:8080/register/step3
2. You should see both Free and Premium membership options
3. Both should be selectable with their respective prices displayed

## Prevention
This fix ensures that:
- Each membership type is checked independently
- Missing memberships are created automatically
- Inactive memberships are reactivated
- Detailed logs help diagnose any future issues
