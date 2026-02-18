# Docker and GitHub Integration Guide
# some changes
## Overview
This document explains how Docker containerization and GitHub CI/CD automation work together for the VOICE Membership System project.

---

## Docker Implementation

### 1. Dockerfile (Multi-Stage Build)

The project uses a **multi-stage Docker build** to optimize the final image size and security:

#### Stage 1: Build Stage
```dockerfile
FROM maven:3.9.6-eclipse-temurin-21 AS build
```
- Uses Maven 3.9.6 with Java 21 (Eclipse Temurin)
- Compiles the Spring Boot application
- Runs `mvn clean package -DskipTests` to create the JAR file
- This stage is discarded in the final image, keeping only the built artifact

#### Stage 2: Runtime Stage
```dockerfile
FROM eclipse-temurin:21-jre
```
- Uses a lightweight Java Runtime Environment (JRE) only
- Copies only the compiled JAR file from the build stage
- Exposes port 8080 for the web application
- Runs the application with `java -jar app.jar`

**Benefits:**
- Smaller final image size (JRE vs full JDK + Maven)
- Faster deployment and reduced security surface
- Separation of build and runtime dependencies

---

### 2. Docker Compose (docker-compose.yaml)

Docker Compose orchestrates multiple containers to run the complete application stack:

#### Services

**Database Service (db):**
```yaml
db:
  image: mysql:8.0
  container_name: voice-db
```
- Runs MySQL 8.0 database
- Uses environment variables for configuration:
  - `MYSQL_DATABASE`: Creates the `web_registration` database
  - `MYSQL_ROOT_PASSWORD`: Set via `.env` file for security
- Exposes port 3306 for database connections
- Includes health checks to ensure DB is ready before app starts

**Application Service (app):**
```yaml
app:
  build: .
  depends_on:
    db:
      condition: service_healthy
```
- Builds from the local Dockerfile
- Waits for database health check before starting
- Connects to database using internal Docker network (jdbc:mysql://db:3306)
- Exposes port 8080 for web access
- Configures Spring Boot properties via environment variables
- Restarts automatically on failure

#### Environment Variables
The system uses the following environment variables (stored in `.env` file):
- `DB_PASSWORD`: MySQL root password
- `MAIL_USERNAME`: Email service username for notifications
- `MAIL_PASSWORD`: Email service password

#### Running the Application

**Start all services:**
```bash
docker-compose up -d
```

**View logs:**
```bash
docker-compose logs -f
```

**Stop all services:**
```bash
docker-compose down
```

**Rebuild and restart:**
```bash
docker-compose up -d --build
```

---

## GitHub CI/CD Pipeline

### Workflow File (.github/workflows/ci-cd.yml)

The GitHub Actions workflow automates the build, test, and deployment process.

#### Trigger
```yaml
on:
  push:
    branches: [ "main" ]
```
- Automatically runs when code is pushed to the `main` branch
- Can be extended to run on pull requests for additional validation

#### Workflow Steps

**1. Checkout Code**
```yaml
- uses: actions/checkout@v4
```
- Clones the repository code into the GitHub Actions runner

**2. Set up Java Development Kit**
```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: maven
```
- Installs Java 21 (Eclipse Temurin distribution)
- Caches Maven dependencies for faster subsequent builds

**3. Build with Maven**
```yaml
- name: Build with Maven
  run: mvn clean package -DskipTests
```
- Compiles the application
- Creates the executable JAR file
- Skips tests for faster deployment (tests can be enabled)

**4. Login to Docker Hub**
```yaml
- name: Login to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKERHUB_USERNAME }}
    password: ${{ secrets.DOCKERHUB_TOKEN }}
```
- Authenticates with Docker Hub using encrypted secrets
- Required to push images to your Docker Hub repository

**5. Build and Push Docker Image**
```yaml
- name: Build and push Docker image
  uses: docker/build-push-action@v5
  with:
    context: .
    push: true
    tags: ${{ secrets.DOCKERHUB_USERNAME }}/voice-membership:latest
```
- Builds the Docker image using the Dockerfile
- Pushes the image to Docker Hub with the `latest` tag
- Makes the image available for deployment on any server

---

## Complete Workflow

### Development to Production Flow

1. **Developer makes changes**
   - Code changes pushed to `main` branch

2. **GitHub Actions triggers automatically**
   - Checks out the code
   - Sets up Java environment
   - Builds the application with Maven
   - Creates JAR file

3. **Docker image creation**
   - Builds multi-stage Docker image
   - Optimizes for production use
   - Tags image as `latest`

4. **Push to Docker Hub**
   - Uploads image to Docker Hub registry
   - Makes image accessible from anywhere
   - Version tagged for deployment

5. **Deployment (Manual or Automated)**
   - Pull latest image: `docker pull <username>/voice-membership:latest`
   - Run with docker-compose on production server
   - Application and database start automatically

---

## Key Benefits

### Docker Benefits
✅ **Consistency**: Same environment in development, testing, and production  
✅ **Isolation**: Application and database run in isolated containers  
✅ **Portability**: Run anywhere Docker is installed  
✅ **Scalability**: Easy to scale services independently  
✅ **Quick Setup**: New developers can start with one command  

### GitHub Actions Benefits
✅ **Automation**: No manual build and deployment steps  
✅ **Continuous Integration**: Code is built and tested on every push  
✅ **Version Control**: Every change is tracked and reversible  
✅ **Collaboration**: Team members can see build status  
✅ **Speed**: Fast feedback on code changes  

---

## Prerequisites

### For Local Development
1. Docker Desktop installed
2. Docker Compose installed (included with Docker Desktop)
3. `.env` file with required environment variables

### For GitHub CI/CD
1. Docker Hub account
2. GitHub repository with the code
3. GitHub Secrets configured:
   - `DOCKERHUB_USERNAME`
   - `DOCKERHUB_TOKEN`

---

## Troubleshooting

### Common Issues

**Database connection fails:**
- Ensure `.env` file exists with `DB_PASSWORD`
- Check if MySQL container is healthy: `docker-compose ps`
- Wait for health check to pass before app starts

**Port already in use:**
- Change ports in `docker-compose.yaml`
- Or stop conflicting services

**GitHub Actions fails:**
- Verify Docker Hub credentials in GitHub Secrets
- Check build logs in GitHub Actions tab
- Ensure Dockerfile syntax is correct

**Image not updating:**
- Pull latest image: `docker pull <username>/voice-membership:latest`
- Rebuild: `docker-compose up -d --build`

---

## Monitoring and Maintenance

### View Application Logs
```bash
docker-compose logs app -f
```

### View Database Logs
```bash
docker-compose logs db -f
```

### Access Database
```bash
docker exec -it voice-db mysql -uroot -p
```

### Check Container Status
```bash
docker-compose ps
```

### Remove All Containers and Volumes
```bash
docker-compose down -v
```

---

## Security Best Practices

1. **Never commit sensitive data**: Use `.env` files (add to `.gitignore`)
2. **Use secrets**: Store credentials in GitHub Secrets and `.env` files
3. **Regular updates**: Keep base images updated (mysql:8.0, eclipse-temurin:21)
4. **Minimal images**: Use JRE instead of JDK in production
5. **Network isolation**: Containers communicate through internal Docker network

---

## Future Enhancements

- Add automated testing in CI/CD pipeline
- Implement staging environment
- Add health check endpoints
- Set up automatic deployment to cloud platforms (AWS, Azure, GCP)
- Configure image vulnerability scanning
- Add version tagging strategy (instead of just `latest`)
- Implement blue-green deployment strategy

---

## Summary

This project leverages **Docker** for containerization and **GitHub Actions** for continuous integration and deployment. Docker ensures the application runs consistently across all environments, while GitHub Actions automates the build and deployment process, pushing production-ready images to Docker Hub on every code change to the main branch.
