# Sentinel Production Deployment Guide

## Prerequisites
- Docker Engine 24+ & Docker Compose v2+
- Or Java 21 JRE, MySQL 8.4, and Redis 7.x

## Production Deployment with Docker Compose

1. **Clone & Configure Environment**:
```bash
cp .env.example .env
# Edit .env with production database credentials and strong JWT_SECRET
```

2. **Launch Cluster**:
```bash
docker compose up -d --build
```

3. **Check Service Health**:
```bash
docker compose ps
curl http://localhost:8080/actuator/health
```

4. **Access Applications**:
- **Sentinel Web Console**: `http://localhost:3000`
- **Gateway Ingress**: `http://localhost:8080/api/v1/gateway/...`
- **Control Plane API**: `http://localhost:8080/api/v1/...`

## Bare Metal / Virtual Machine Setup

### Backend
```bash
cd backend/sentinel-api
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://localhost:3306/sentinel
export DB_USERNAME=sentinel
export DB_PASSWORD=your_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
export JWT_SECRET=your_minimum_256_bit_secret
java -jar target/sentinel-api-0.0.1-SNAPSHOT.jar
```

### Frontend
```bash
cd frontend
npm ci
npm run build
# Serve dist/ using Nginx or Caddy
```
