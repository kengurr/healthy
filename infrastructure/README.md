# Zdravdom Infrastructure

## Overview

This directory contains infrastructure-as-code for deploying Zdravdom MVP.

**GDPR Note**: All data must remain within EU region. AWS region: `eu-central-1` (Frankfurt). Hetzner: `nbg1` (Nuremberg) or `hel1` (Helsinki).

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              AWS ECS Fargate                                │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────────────┐ │
│  │  Health Check   │    │  Prometheus     │    │  Sentry Error           │ │
│  │  /actuator/health│    │  /prometheus    │    │  Tracking                │ │
│  └────────┬────────┘    └────────┬────────┘    └─────────────────────────┘ │
│           │                       │                       │                  │
│  ┌────────▼───────────────────────▼───────────────────────▼───────────────┐ │
│  │                     ALB (Application Load Balancer)                    │ │
│  │                    (HTTPS 443, HTTP→HTTPS redirect)                    │ │
│  └────────────────────────────────┬───────────────────────────────────────┘ │
│                                   │                                          │
│  ┌────────────────────────────────▼───────────────────────────────────────┐ │
│  │                    ECS Fargate Task (Backend)                          │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ │ │
│  │  │ auth/       │  │ booking/    │  │ visit/      │  │ billing/        │ │ │
│  │  │ user/       │  │ matching/   │  │ notification│  │ cms/            │ │ │
│  │  │ analytics/  │  │ integration/│  │             │  │                 │ │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘ │ │
│  │              Modular Monolith (Java 21 / Spring Boot 3)               │ │
│  └────────────────────────────────┬───────────────────────────────────────┘ │
└───────────────────────────────────┼────────────────────────────────────────┘
                                    │
        ┌────────────────────────────┼────────────────────────────┐
        │                            │                            │
┌───────▼───────┐           ┌────────▼────────┐          ┌───────▼───────┐
│ PostgreSQL    │           │    Redis        │          │     S3       │
│ (RDS Aurora)  │           │  (ElastiCache)  │          │  (Documents  │
│ 10 Schemas    │           │  Slot locking   │          │   Reports    │
│ per module    │           │  Sessions       │          │   Media)     │
│ eu-central-1  │           │  Cache          │          │  SSE EU      │
└───────────────┘           └─────────────────┘          └──────────────┘
        │
┌───────▼───────┐
│ Kafka (MSK)   │
│ Async events  │
│ eu-central-1  │
└───────────────┘
```

## Component Details

### Compute
- **ECS Fargate**: Backend service, no EC2 management required
  - Staging: 2 tasks (0.5 vCPU / 1 GB each)
  - Production: 2-4 tasks (1 vCPU / 2 GB each), auto-scaling on CPU/memory

### Data Layer
| Service | Purpose | Configuration |
|---------|---------|---------------|
| PostgreSQL (RDS Aurora) | 10 schemas per domain module | Staging: db.t3.medium, 100GB. Prod: db.r6g.large, 200GB, Multi-AZ |
| Redis (ElastiCache) | Slot locking, session caching | Staging: cache.t3.micro. Prod: cache.r6g.large, cluster mode |
| S3 | patient-docs/, visit-reports/, exports/ | SSE-KMS encryption, lifecycle policies |
| Kafka (MSK) | Spring Cloud Stream async events | Staging: 3 brokers, kafka.t3.small. Prod: 6 brokers, kafka.m5.large |

### Networking
- **VPC**: Single VPC with 3 AZs (staging), 3 AZs (prod)
- **Subnets**: Public ALB, Private ECS tasks, Private RDS/Redis
- **Security Groups**: Minimal blast radius (task SG → only DB/Redis/Kafka allowed)

### Monitoring
- **Grafana + Prometheus**: Metrics collection, alerting
- **Sentry**: Error tracking with source maps
- **CloudWatch**: Native logs, LB metrics

---

## Cost Estimates

### Staging (EUR/month)
| Service | Instance | Hours | EUR |
|---------|----------|-------|-----|
| ECS Fargate (0.5 vCPU/1GB) | 2 tasks | 730 | ~25 |
| RDS Aurora PostgreSQL | db.t3.medium | 730 | ~80 |
| ElastiCache Redis | cache.t3.micro | 730 | ~15 |
| MSK Kafka | 3x kafka.t3.small | 730 | ~180 |
| S3 | 100GB | - | ~10 |
| ALB | Basic | 730 | ~20 |
| Data Transfer | ~50GB | - | ~10 |
| **Total** | | | | **~340-440** |

### Production (EUR/month)
| Service | Instance | Hours | EUR |
|---------|----------|-------|-----|
| ECS Fargate (1 vCPU/2GB) | 4 tasks (autoscale) | 730 | ~120 |
| RDS Aurora PostgreSQL | db.r6g.large Multi-AZ | 730 | ~400 |
| ElastiCache Redis | cache.r6g.large cluster | 730 | ~120 |
| MSK Kafka | 6x kafka.m5.large | 730 | ~600 |
| S3 | 500GB | - | ~40 |
| ALB | Application | 730 | ~30 |
| Data Transfer | ~500GB | - | ~50 |
| **Total** | | | **~1360-2500** |

*Prices are estimates. Actual costs depend on usage. EU region (Frankfurt) pricing as of 2026-04.*

---

## File Structure
```
infrastructure/
├── README.md              # This file
├── main.tf                # AWS ECS Fargate Terraform skeleton
├── .env.example          # Environment variables template
└── backend/
    └── docker-compose.yml # Local development (enhancement)
```

---

## Getting Started

### Prerequisites
- Terraform >= 1.5
- AWS CLI configured with appropriate credentials
- Docker (local dev)

### Deploy to Staging
```bash
cd infrastructure
cp .env.example .env
# Edit .env with staging values
terraform init
terraform plan -var-file=vars/staging.tfvars
terraform apply -var-file=vars/staging.tfvars
```

### Local Development
```bash
cd backend
docker-compose up -d
# Backend available at http://localhost:8080/api/v1
```
