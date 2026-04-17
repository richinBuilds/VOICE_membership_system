# VOICE Membership Management System
# Overview

The VOICE Membership Management System is a full-stack web application designed to manage memberships for the VOICE organization. It allows users to register, manage memberships, process payments, and handle authentication securely.

This project is deployed using Docker to ensure consistent and easy setup across environments.

# Tech Stack

Java (Spring Boot)
Thymeleaf
MySQL
Docker & Docker Compose
Maven
GitHub Actions (CI/CD)

# Prerequisites

Before running the application, ensure you have:

Docker installed (Docker Desktop or Docker Engine)
Docker Compose installed
Git (optional, for cloning repository)

## Getting Started

1. Clone the Repository

git clone https://github.com/richinBuilds/VOICE_membership_system.git
cd voice-membership-system

2. Setup Environment Variables
- Create a .env file based on the provided template:

cp .env.example .env

- Update the .env file with your actual production values:

DB_URL=jdbc:mysql://db:3306/web_registration?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC

DB_PASSWORD=change-me
APP_BASE_URL=https://your-domain.example.com

MAIL_HOST = replace with correct host address
MAIL_USERNAME=replace-with-mail-username
MAIL_PASSWORD=replace-with-mail-password

GOOGLE_CLIENT_ID=replace-with-google-client-id
GOOGLE_CLIENT_SECRET=replace-with-google-client-secret
GOOGLE_REDIRECT_URI=https://your-domain.example.com/login/oauth2/code/google

PAYPAL_CLIENT_ID=replace-with-paypal-client-id
PAYPAL_CLIENT_SECRET=replace-with-paypal-client-secret

ADMIN_EMAIL=admin@your-domain.example.com
ADMIN_PASSWORD=change-this-admin-password

SESSION_COOKIE_SECURE=true
TZ=America/Toronto

- Configure the DB_URL based on your database setup (Docker container or external database server)

3. Run the Application

docker compose up -d

4. Access the Application

Open your browser and go to: http://localhost:8080
or your configured domain.

# Docker Image

The application is available as a pre-built image:
richinlearns/voice-membership:latest

# Common Commands

# Start services 
docker compose up -d

# Stop services
docker compose down

# Restart services
docker compose restart

# View logs
docker compose logs -f app
docker compose logs -f db

# Database Backup & Restore
# Backup
docker exec voice-db mysqldump -uroot -p web_registration > backup.sql

# Restore
docker exec -i voice-db mysql -uroot -p web_registration < backup.sql

# ⚠️ Reset Database (Non-production only)
docker compose down -v
docker compose up -d

 - Warning: This will delete all database data.

# Deployment Notes
Ensure all environment variables are set correctly
Do not use default credentials in production
Enable secure cookies (SESSION_COOKIE_SECURE=true)
Replace test services (Mailtrap, PayPal Sandbox) with production services

Third-Party Requirements

# The client must configure and own:

Google OAuth credentials
PayPal account (live credentials)
Email service provider
Hosting server with Docker support

## Troubleshooting

# Application not starting
# Run:
docker ps

# Check logs:
docker compose logs -f app

## Database issues
-  Verify .env credentials
# Check DB logs:
docker compose logs -f db

## Login / OAuth issues
- Ensure redirect URIs match production domain
- Verify API credentials

## Maintenance
 - Regularly back up the database
 - Rotate credentials periodically
 - Pull updated images when new versions are released:
 - docker pull richinlearns/voice-membership:latest

 ## Support

For initial deployment support, refer to the provided documentation or contact the development team.