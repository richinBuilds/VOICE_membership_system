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
