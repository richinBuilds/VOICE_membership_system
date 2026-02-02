# VOICE Membership System

A comprehensive Spring Boot web application for managing memberships for VOICE (Vocal Outreach Initiative for Children's Education), an organization supporting children who are deaf and hard of hearing.

## 🌟 Features

- **Multi-step Registration** with validation
- **User Authentication** with Spring Security
- **Profile & Child Management**
- **Admin Dashboard** with advanced filtering (city, province, age, etc.)
- **Membership System** with free and paid tiers
- **Email Notifications** for welcome and upgrades
- **Export to Excel** functionality
- **Password Reset** system

## 🛠️ Technology Stack

- Spring Boot 3.4.5
- Java 21
- MySQL 8.0
- Hibernate/JPA
- Spring Security 6
- Thymeleaf
- Bootstrap 5
- Maven

## 🚀 Quick Start

### 1. Clone & Database Setup

```bash
git clone https://github.com/yourusername/VOICE-Membership-System.git
cd VOICE-Membership-System

# Create MySQL database
mysql -u root -p
CREATE DATABASE web_registration CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure Environment

Copy `.env.example` to `.env` and update:

```properties
DB_URL=jdbc:mysql://localhost:3306/web_registration?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=your_password

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

### 3. Build & Run

```bash
mvn clean install
mvn spring-boot:run
```

Access at `http://localhost:8080`

### Default Admin Login

- Email: `tarparakrimy@gmail.com`
- Password: `Admin@123`

⚠️ Change admin credentials after first login!

## 📁 Project Structure

```
src/main/
├── java/org/voice/membership/
│   ├── config/         # Security & startup config
│   ├── controllers/    # MVC Controllers
│   ├── entities/       # JPA Entities
│   ├── repositories/   # Data access
│   └── services/       # Business logic
└── resources/
    ├── application.yaml
    ├── static/         # CSS, images
    └── templates/      # Thymeleaf HTML
```

## 🔑 Key Endpoints

**Public**: `/`, `/register`, `/login`, `/forgot-password`

**User**: `/profile`, `/profile/edit`, `/profile/child/*`

**Admin**: `/admin/dashboard`, `/admin/export-users`

## 🎨 Admin Dashboard Features

Filter users by:

- Address, City, Province
- Child age range
- Hearing loss type
- Equipment type
- Registration date range

Export filtered data to Excel

## 🔒 Security

- BCrypt password encryption
- CSRF protection
- Role-based access (USER, ADMIN)
- Secure session management
- Password reset with token expiration

## 📊 Database Tables

- `users` - User accounts
- `children` - Child records
- `membership_options` - Membership tiers
- `carts` & `cart_items` - Shopping cart
- `membership_benefits` - Benefit details
- `landing_page_content` - Dynamic content

## 🧪 Testing

```bash
mvn test
```

## 📦 Production Build

```bash
mvn clean package -DskipTests
java -jar target/voice-membership-0.0.1-SNAPSHOT.jar
```

## CI & Docker

This project uses GitHub Actions for continuous integration and Docker for containerization.

### CI Pipeline

- Java 21
- Maven build & test
- Docker image build

### Docker

To build locally:

```bash
docker build -t voice-membership .
docker run -p 8080:8080 voice-membership
```

With environment variables:

```bash
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host.docker.internal:3306/web_registration \
  -e DB_USERNAME=root \
  -e DB_PASSWORD=your_password \
  -e MAIL_USERNAME=your-email@gmail.com \
  -e MAIL_PASSWORD=your-app-password \
  voice-membership
```

## 📝 Environment Variables

| Variable             | Default        | Description                 |
| -------------------- | -------------- | --------------------------- |
| `DB_URL`             | localhost:3306 | MySQL connection URL        |
| `DB_USERNAME`        | root           | Database user               |
| `DB_PASSWORD`        | root           | Database password           |
| `MAIL_HOST`          | smtp.gmail.com | SMTP server                 |
| `MAIL_PORT`          | 587            | SMTP port                   |
| `MAIL_USERNAME`      | -              | Email username              |
| `MAIL_PASSWORD`      | -              | Email password/app password |
| `SERVER_PORT`        | 8080           | Application port            |
| `HIBERNATE_DDL_AUTO` | update         | DDL mode                    |
| `JPA_SHOW_SQL`       | false          | Show SQL logs               |

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/NewFeature`)
3. Commit changes (`git commit -m 'Add NewFeature'`)
4. Push to branch (`git push origin feature/NewFeature`)
5. Open Pull Request

## 📄 License

This project is licensed under the MIT License.

---

**Built with ❤️ for VOICE - Supporting children who are deaf and hard of hearing**
