# Zdravdom AWS ECS Fargate Terraform Skeleton
# Requires Terraform >= 1.5

terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket = "zdravdom-terraform-state"
    key    = "production/terraform.tfstate"
    region = "eu-central-1"
    # Enable encryption at rest
    encrypt = true
  }
}

provider "aws" {
  region = "eu-central-1" # GDPR-compliant EU region (Frankfurt)

  default_tags {
    tags = {
      Project     = "Zdravdom"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# -----------------------------------------------------------------------------
# Variables
# -----------------------------------------------------------------------------

variable "environment" {
  description = "Deployment environment"
  type        = string
  validation {
    condition     = contains(["staging", "production"], var.environment)
    error_message = "Environment must be staging or production."
  }
}

variable "aws_region" {
  description = "AWS region (GDPR: must be EU)"
  type        = string
  default     = "eu-central-1"
}

# -----------------------------------------------------------------------------
# Data Sources
# -----------------------------------------------------------------------------

data "aws_caller_identity" "current" {}

data "aws_vpc" "existing" {
  # TODO: Replace with your VPC ID after running `terraform import`
  # This is a placeholder - real deployment would import existing VPC
  # or create a new one
}

data "aws_subnets" "private" {
  # TODO: Import private subnet IDs from your VPC setup
}

data "aws_subnets" "public" {
  # TODO: Import public subnet IDs for ALB
}

# -----------------------------------------------------------------------------
# Networking
# -----------------------------------------------------------------------------

# VPC (uncomment if creating new VPC)
# resource "aws_vpc" "main" {
#   cidr_block           = "10.0.0.0/16"
#   enable_dns_hostnames = true
#   enable_dns_support   = true
#
#   tags = {
#     Name = "zdravdom-${var.environment}-vpc"
#   }
# }
#
# resource "aws_subnet" "private_a" {
#   vpc_id                  = aws_vpc.main.id
#   cidr_block              = "10.0.1.0/24"
#   availability_zone       = "${var.aws_region}a"
#   map_public_ip_on_launch = false
# }
#
# resource "aws_subnet" "private_b" {
#   vpc_id                  = aws_vpc.main.id
#   cidr_block              = "10.0.2.0/24"
#   availability_zone       = "${var.aws_region}b"
#   map_public_ip_on_launch = false
# }
#
# resource "aws_subnet" "private_c" {
#   vpc_id                  = aws_vpc.main.id
#   cidr_block              = "10.0.3.0/24"
#   availability_zone       = "${var.aws_region}c"
#   map_public_ip_on_launch = false
# }

# -----------------------------------------------------------------------------
# Security Groups
# -----------------------------------------------------------------------------

resource "aws_security_group" "ecs_tasks" {
  name        = "zdravdom-${var.environment}-ecs-tasks"
  description = "Security group for ECS Fargate tasks"
  vpc_id      = data.aws_vpc.existing.id

  # Allow outbound only
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "zdravdom-${var.environment}-ecs-tasks"
  }
}

resource "aws_security_group_rule" "ecs_to_postgres" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  description              = "ECS tasks to PostgreSQL"

  # This requires the SG to exist first
  # TODO: After importing VPC, uncomment and reference aws_security_group.postgres.id
}

resource "aws_security_group_rule" "ecs_to_redis" {
  type                     = "ingress"
  from_port                = 6379
  to_port                  = 6379
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  description              = "ECS tasks to Redis"
}

resource "aws_security_group_rule" "ecs_to_kafka" {
  type                     = "ingress"
  from_port                = 9092
  to_port                  = 9092
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  description              = "ECS tasks to Kafka"
}

resource "aws_security_group" "alb" {
  name        = "zdravdom-${var.environment}-alb"
  description = "Security group for Application Load Balancer"
  vpc_id      = data.aws_vpc.existing.id

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "zdravdom-${var.environment}-alb"
  }
}

# -----------------------------------------------------------------------------
# IAM Roles
# -----------------------------------------------------------------------------

# ECS Task Execution Role
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "zdravdom-${var.environment}-ecs-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Project = "Zdravdom"
  }
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# ECS Task Role (application permissions)
resource "aws_iam_role" "ecs_task_role" {
  name = "zdravdom-${var.environment}-ecs-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Project = "Zdravdom"
  }
}

# S3 access for documents/reports
resource "aws_iam_policy" "s3_access" {
  name = "zdravdom-${var.environment}-s3-access"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject"
        ]
        Resource = [
          "arn:aws:s3:::zdravdom-documents-${var.environment}/*",
          "arn:aws:s3:::zdravdom-reports-${var.environment}/*",
          "arn:aws:s3:::zdravdom-media-${var.environment}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "s3:ListBucket"
        ]
        Resource = [
          "arn:aws:s3:::zdravdom-documents-${var.environment}",
          "arn:aws:s3:::zdravdom-reports-${var.environment}",
          "arn:aws:s3:::zdravdom-media-${var.environment}"
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_s3_access" {
  role       = aws_iam_role.ecs_task_role.name
  policy_arn = aws_iam_policy.s3_access.arn
}

# Secrets Manager access (for database passwords, JWT secret)
resource "aws_iam_policy" "secrets_access" {
  name = "zdravdom-${var.environment}-secrets-access"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:zdravdom/${var.environment}/*"
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_secrets_access" {
  role       = aws_iam_role.ecs_task_role.name
  policy_arn = aws_iam_policy.secrets_access.arn
}

# -----------------------------------------------------------------------------
# S3 Buckets
# -----------------------------------------------------------------------------

resource "aws_s3_bucket" "documents" {
  bucket = "zdravdom-documents-${var.environment}"

  tags = {
    Project     = "Zdravdom"
    Environment = var.environment
    DataType    = "patient-documents"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "documents" {
  bucket = aws_s3_bucket.documents.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
      # TODO: Create and reference a KMS key
      # kms_master_key_id = aws_kms_key.zdravdom.arn
    }
  }
}

resource "aws_s3_bucket" "reports" {
  bucket = "zdravdom-reports-${var.environment}"

  tags = {
    Project     = "Zdravdom"
    Environment = var.environment
    DataType    = "visit-reports"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "reports" {
  bucket = aws_s3_bucket.reports.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
  }
}

resource "aws_s3_bucket" "media" {
  bucket = "zdravdom-media-${var.environment}"

  tags = {
    Project     = "Zdravdom"
    Environment = var.environment
    DataType    = "media"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
  }
}

# S3 Lifecycle policies (GDPR: 10-year retention for health data)
resource "aws_s3_bucket_lifecycle_policy" "documents" {
  bucket = aws_s3_bucket.documents.id

  rule {
    id     = "legal-hold"
    status = "Enabled"

    expiration {
      days = 3650 # 10 years for health data
    }
  }
}

# -----------------------------------------------------------------------------
# ECR Registry
# -----------------------------------------------------------------------------

resource "aws_ecr_repository" "backend" {
  name         = "zdravdom-backend"
  image_tag_mutability = "MUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Project = "Zdravdom"
  }
}

# -----------------------------------------------------------------------------
# ECS Cluster
# -----------------------------------------------------------------------------

resource "aws_ecs_cluster" "main" {
  name = "zdravdom-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Project = "Zdravdom"
  }
}

resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name = aws_ecs_cluster.main.name

  capacity_providers = ["FARGATE"]

  default_capacity_provider_strategy {
    base              = 1
    weight            = 100
    capacity_provider = "FARGATE"
  }
}

# -----------------------------------------------------------------------------
# ECS Task Definition
# -----------------------------------------------------------------------------

locals {
  backend_image = "${aws_ecr_repository.backend.repository_url}:latest"

  environment_variables = {
    SPRING_PROFILES_ACTIVE    = var.environment
    DATABASE_HOST             = aws_rds_cluster.postgres.endpoint
    DATABASE_PORT             = 5432
    DATABASE_NAME             = "zdravdom"
    REDIS_HOST                = aws_elasticache_cluster.redis.cache_host
    REDIS_PORT                = 6379
    KAFKA_BOOTSTRAP_SERVERS   = aws_msk_cluster.kafka.bootstrap_brokers
    AWS_REGION                = var.aws_region
    AWS_S3_ENDPOINT           = "https://s3.${var.aws_region}.amazonaws.com"
  }
}

resource "aws_ecs_task_definition" "backend" {
  family                   = "zdravdom-backend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.environment == "production" ? 1024 : 512
  memory                   = var.environment == "production" ? 2048 : 1024
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name      = "backend"
      image     = local.backend_image
      essential = true

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        for key, value in local.environment_variables :
        {
          name  = key
          value = value
        }
      ]

      # Secrets from Secrets Manager
      secrets = [
        {
          name      = "DATABASE_PASSWORD"
          valueFrom = "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:zdravdom/${var.environment}/database:password::"
        },
        {
          name      = "JWT_SECRET"
          valueFrom = "arn:aws:secretsmanager:${var.aws_region}:${data.aws_caller_identity.current.account_id}:secret:zdravdom/${var.environment}/jwt:secret::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/zdravdom-backend"
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget --no-standalone -qO- http://localhost:8080/api/v1/actuator/health || exit 1"]
        interval    = 30
        timeout     = 10
        retries     = 3
        startPeriod = 60
      }
    }
  ])
}

# -----------------------------------------------------------------------------
# ECS Service
# -----------------------------------------------------------------------------

resource "aws_ecs_service" "backend" {
  name            = "zdravdom-backend"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.backend.arn
  desired_count   = var.environment == "production" ? 2 : 2
  launch_type     = "FARGATE"

  deployment_controller {
    type = "ECS"
  }

  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200

  load_balancer {
    target_group_arn = aws_lb_target_group.backend.arn
    container_name   = "backend"
    container_port   = 8080
  }

  network_configuration {
    subnets          = data.aws_subnets.private.ids
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  depends_on = [
    aws_lb.backend
  ]
}

# -----------------------------------------------------------------------------
# Application Load Balancer
# -----------------------------------------------------------------------------

resource "aws_lb" "backend" {
  name               = "zdravdom-${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups   = [aws_security_group.alb.id]
  subnets           = data.aws_subnets.public.ids

  enable_deletion_protection = var.environment == "production"

  tags = {
    Project = "Zdravdom"
  }
}

resource "aws_lb_target_group" "backend" {
  name     = "zdravdom-${var.environment}-tg"
  port     = 8080
  protocol = "HTTP"
  vpc_id   = data.aws_vpc.existing.id

  deregistration_delay = 30
  slow_start           = var.environment == "production" ? 30 : 0

  health_check {
    enabled = true
    path    = "/api/v1/actuator/health"
    matcher = "200"
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.backend.arn
  port              = 443
  protocol          = "HTTPS"

  # TODO: Add ACM certificate
  # certificate_arn = aws_acm_certificate.zdravdom.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

# HTTP to HTTPS redirect
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.backend.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"
    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# -----------------------------------------------------------------------------
# RDS Aurora PostgreSQL
# -----------------------------------------------------------------------------

resource "aws_rds_cluster" "postgres" {
  cluster_identifier        = "zdravdom-${var.environment}-postgres"
  engine                    = "aurora-postgresql"
  engine_version            = "16.3"
  engine_mode               = var.environment == "production" ? "provisioned" : "serverless"
  database_name             = "zdravdom"
  master_username           = "postgres"
  master_password           = "" # Set via Secrets Manager
  skip_final_snapshot       = var.environment == "staging"
  final_snapshot_identifier = var.environment == "production" ? "zdravdom-${var.environment}-final" : null

  # Serverless scaling (staging)
  # serverlessv2_scaling_configuration = var.environment == "staging" ? {
  #   min_capacity = 2
  #   max_capacity = 8
  # } : null

  # Multi-AZ for production
  multi_az               = var.environment == "production"
  db_subnet_group_name   = aws_rds_subnet_group.main.name
  vpc_security_group_ids  = [] # TODO: Add security group
  storage_encrypted       = true
  # kms_key_id             = "" # TODO: Add KMS key for production

  # Performance insights
  performance_insights_enabled = var.environment == "production"
  # performance_insights_kms_key_id = "" # TODO: Add KMS key

  tags = {
    Project = "Zdravdom"
  }
}

resource "aws_rds_cluster_instance" "postgres" {
  count         = var.environment == "production" ? 2 : 1
  identifier    = "zdravdom-${var.environment}-postgres-${count.index}"
  cluster_identifier = aws_rds_cluster.postgres.id
  instance_class      = var.environment == "production" ? "db.r6g.large" : "db.t3.medium"
  engine                = aws_rds_cluster.postgres.engine
  engine_version        = aws_rds_cluster.postgres.engine_version

  tags = {
    Project = "Zdravdom"
  }
}

resource "aws_rds_subnet_group" "main" {
  name       = "zdravdom-${var.environment}-postgres-subnet"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Project = "Zdravdom"
  }
}

# -----------------------------------------------------------------------------
# ElastiCache Redis
# -----------------------------------------------------------------------------

resource "aws_elasticache_cluster" "redis" {
  cluster_id           = "zdravdom-${var.environment}-redis"
  engine               = "redis"
  engine_version       = "7.1"
  node_type            = var.environment == "production" ? "cache.r6g.large" : "cache.t3.micro"
  num_cache_nodes       = 1
  parameter_group_name  = "default.redis7"
  port                 = 6379
  security_group_ids   = [] # TODO: Add security group
  subnet_group_name    = aws_elasticache_subnet_group.main.name

  snapshot_retention_limit   = 7  # 7 days backup
  snapshot_window            = "03:00-04:00"
  maintenance_window         = "mon:04:00-mon:05:00"

  at_rest_encryption_enabled = true
  transit_encryption_enabled = false # Fargate doesn't support TLS easily

  tags = {
    Project = "Zdravdom"
  }
}

resource "aws_elasticache_subnet_group" "main" {
  name       = "zdravdom-${var.environment}-redis-subnet"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Project = "Zdravdom"
  }
}

# -----------------------------------------------------------------------------
# Amazon MSK Kafka
# -----------------------------------------------------------------------------

resource "aws_msk_cluster" "kafka" {
  cluster_name           = "zdravdom-${var.environment}-kafka"
  kafka_version          = "3.6.0"
  number_of_broker_nodes = var.environment == "production" ? 6 : 3

  broker_node_group_info {
    instance_type   = var.environment == "production" ? "kafka.m5.large" : "kafka.t3.small"
    client_subnets  = data.aws_subnets.private.ids
    security_groups = [] # TODO: Add security group
    storage_info {
      ebs_storage_info {
        volume_size = var.environment == "production" ? 500 : 100
      }
    }
  }

  # Encryption
  encryption_info {
    encryption_at_rest_kms_key_arn = "" # TODO: Add KMS key
    data_volume_encryption          = true
  }

  # Logging
  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = "/aws/msk/${var.environment}"
      }
    }
  }

  tags = {
    Project = "Zdravdom"
  }
}

# -----------------------------------------------------------------------------
# CloudWatch Logs
# -----------------------------------------------------------------------------

resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/zdravdom-backend"
  retention_in_days = var.environment == "production" ? 30 : 7

  tags = {
    Project = "Zdravdom"
  }
}

resource "aws_cloudwatch_log_group" "msk" {
  name              = "/aws/msk/${var.environment}"
  retention_in_days = 7

  tags = {
    Project = "Zdravdom"
  }
}

# -----------------------------------------------------------------------------
# CloudWatch Alarm for High CPU
# -----------------------------------------------------------------------------

resource "aws_cloudwatch_metric_alarm" "ecs_cpu_high" {
  alarm_name          = "zdravdom-${var.environment}-ecs-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 2
  metric_name         = "CPUUtilization"
  namespace          = "AWS/ECS"
  period             = 300
  statistic          = "Average"
  threshold          = 80

  dimensions = {
    ClusterName = aws_ecs_cluster.main.name
    ServiceName = aws_ecs_service.backend.name
  }

  alarm_actions = [] # TODO: Add SNS topic ARN for alerting
  ok_actions    = []
}

# -----------------------------------------------------------------------------
# Secrets Manager (Database passwords, JWT secret)
# -----------------------------------------------------------------------------

# Note: Run this AFTER initial deployment to create secrets
# resource "aws_secretsmanager_secret" "database_password" {
#   name        = "zdravdom/${var.environment}/database"
#   description = "Zdravdom ${var.environment} database password"
#
#   recovery_window_in_days = 0 # Cannot be deleted (GDPR)
# }

# resource "aws_secretsmanager_secret" "jwt_secret" {
#   name        = "zdravdom/${var.environment}/jwt"
#   description = "Zdravdom ${var.environment} JWT secret"
#
#   recovery_window_in_days = 0
# }

# -----------------------------------------------------------------------------
# Outputs
# -----------------------------------------------------------------------------

output "alb_dns_name" {
  description = "Load Balancer DNS name"
  value       = aws_lb.backend.dns_name
}

output "ecs_cluster_name" {
  description = "ECS Cluster name"
  value       = aws_ecs_cluster.main.name
}

output "ecs_service_name" {
  description = "ECS Service name"
  value       = aws_ecs_service.backend.name
}

output "postgres_endpoint" {
  description = "PostgreSQL cluster endpoint"
  value       = aws_rds_cluster.postgres.endpoint
}

output "postgres_reader_endpoint" {
  description = "PostgreSQL read replica endpoint"
  value       = aws_rds_cluster.postgres.reader_endpoint
}

output "redis_endpoint" {
  description = "Redis cluster endpoint"
  value       = aws_elasticache_cluster.redis.cache_host
}

output "kafka_bootstrap_brokers" {
  description = "Kafka bootstrap brokers"
  value       = aws_msk_cluster.kafka.bootstrap_brokers
}

output "s3_documents_bucket" {
  description = "Documents S3 bucket name"
  value       = aws_s3_bucket.documents.bucket
}

output "s3_reports_bucket" {
  description = "Reports S3 bucket name"
  value       = aws_s3_bucket.reports.bucket
}

output "s3_media_bucket" {
  description = "Media S3 bucket name"
  value       = aws_s3_bucket.media.bucket
}
