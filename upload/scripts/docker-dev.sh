#!/bin/bash

# Docker management script for Upload Service

set -e

case "$1" in
    "start")
        echo "🚀 Starting Upload Service infrastructure..."
        docker-compose up -d postgres redis
        echo "✅ Services started!"
        echo "📊 PostgreSQL: localhost:5433"
        echo "🔴 Redis: localhost:6380"
        ;;
    
    "start-admin")
        echo "🚀 Starting Upload Service with admin tools..."
        docker-compose --profile admin up -d
        echo "✅ Services with admin tools started!"
        echo "📊 PostgreSQL: localhost:5433"
        echo "🔴 Redis: localhost:6380"
        echo "🎛️  pgAdmin: http://localhost:5051"
        echo "🎮 Redis Commander: http://localhost:8085"
        ;;
    
    "stop")
        echo "🛑 Stopping Upload Service infrastructure..."
        docker-compose down
        echo "✅ Services stopped!"
        ;;
    
    "restart")
        echo "🔄 Restarting Upload Service infrastructure..."
        docker-compose down
        docker-compose up -d postgres redis
        echo "✅ Services restarted!"
        ;;
    
    "logs")
        service=${2:-""}
        if [ -z "$service" ]; then
            docker-compose logs -f
        else
            docker-compose logs -f $service
        fi
        ;;
    
    "status")
        echo "📋 Upload Service status:"
        docker-compose ps
        ;;
    
    "clean")
        echo "🧹 Cleaning up Upload Service containers and volumes..."
        read -p "Are you sure? This will delete all data! (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker-compose down -v
            docker-compose rm -f
            docker volume rm upload_postgres_data upload_redis_data upload_pgadmin_data 2>/dev/null || true
            echo "✅ Cleanup complete!"
        else
            echo "❌ Cleanup cancelled."
        fi
        ;;
    
    "backup-db")
        echo "💾 Creating database backup..."
        timestamp=$(date +%Y%m%d_%H%M%S)
        docker-compose exec postgres pg_dump -U upload_user video_platform > "backup_${timestamp}.sql"
        echo "✅ Backup created: backup_${timestamp}.sql"
        ;;
    
    "restore-db")
        if [ -z "$2" ]; then
            echo "❌ Please specify backup file: ./scripts/docker-dev.sh restore-db backup_file.sql"
            exit 1
        fi
        echo "📥 Restoring database from $2..."
        docker-compose exec -T postgres psql -U upload_user video_platform < "$2"
        echo "✅ Database restored!"
        ;;
    
    "shell-db")
        echo "🐘 Connecting to PostgreSQL..."
        docker-compose exec postgres psql -U upload_user video_platform
        ;;
    
    "shell-redis")
        echo "🔴 Connecting to Redis..."
        docker-compose exec redis redis-cli
        ;;
    
    *)
        echo "🎯 Upload Service Docker Management"
        echo ""
        echo "Usage: $0 {command}"
        echo ""
        echo "Commands:"
        echo "  start          - Start PostgreSQL and Redis"
        echo "  start-admin    - Start all services including admin tools"
        echo "  stop           - Stop all services"
        echo "  restart        - Restart core services"
        echo "  logs [service] - Show logs (all or specific service)"
        echo "  status         - Show services status"
        echo "  clean          - Remove all containers and volumes"
        echo "  backup-db      - Create database backup"
        echo "  restore-db     - Restore database from backup"
        echo "  shell-db       - Connect to PostgreSQL shell"
        echo "  shell-redis    - Connect to Redis shell"
        echo ""
        echo "Port allocation:"
        echo "  PostgreSQL:     localhost:5433 (auth service uses 5432)"
        echo "  Redis:          localhost:6380"
        echo "  pgAdmin:        http://localhost:5051"
        echo "  Redis Commander: http://localhost:8085 (upload service uses 8082)"
        echo ""
        echo "Examples:"
        echo "  $0 start"
        echo "  $0 logs postgres"
        echo "  $0 backup-db"
        exit 1
        ;;
esac 