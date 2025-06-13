#!/bin/bash

# Video Hosting Platform Infrastructure Management Script

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$(dirname "$SCRIPT_DIR")"

cd "$INFRA_DIR"

show_help() {
    echo "🎥 Video Hosting Platform Infrastructure"
    echo ""
    echo "Usage: ./platform {command} [profile]"
    echo ""
    echo "Commands:"
    echo "  start [profile]    - Start infrastructure services"
    echo "  stop              - Stop all services"
    echo "  restart [profile] - Restart services"
    echo "  logs [service]    - Show logs (all or specific service)"
    echo "  status            - Show services status"
    echo "  clean             - Remove all containers and volumes"
    echo "  backup-auth       - Create auth database backup"
    echo "  backup-video      - Create video database backup"
    echo "  restore-auth      - Restore auth database from backup"
    echo "  restore-video     - Restore video database from backup"
    echo "  shell-auth        - Connect to auth database shell"
    echo "  shell-video       - Connect to video database shell"
    echo "  shell-redis       - Connect to Redis shell"
    echo ""
    echo "Profiles:"
    echo "  auth              - Only authentication services (postgres-auth)"
    echo "  video             - Video services (postgres-video, redis, rabbitmq)"
    echo "  full              - All services (default)"
    echo "  admin             - All services + admin tools"
    echo ""
    echo "Port allocation:"
    echo "  Auth PostgreSQL:    localhost:5432"
    echo "  Video PostgreSQL:   localhost:5433"
    echo "  Redis:              localhost:6379"
    echo "  RabbitMQ:           localhost:5672"
    echo "  RabbitMQ UI:        http://localhost:15672"
    echo "  pgAdmin Auth:       http://localhost:5050"
    echo "  pgAdmin Video:      http://localhost:5051"
    echo "  Redis Commander:    http://localhost:8081"
    echo ""
    echo "Examples:"
    echo "  ./platform start auth              # Start only auth database"
    echo "  ./platform start video             # Start video services"
    echo "  ./platform start full              # Start all services"
    echo "  ./platform start admin             # Start all + admin tools"
    echo "  ./platform logs postgres-video     # Show video database logs"
    echo "  ./platform backup-video           # Backup video database"
}

case "$1" in
    "start")
        profile=${2:-"full"}
        echo "🚀 Starting platform infrastructure (profile: $profile)..."

        if [ "$profile" = "admin" ]; then
            docker compose --profile full --profile admin up -d
            echo "✅ All services with admin tools started!"
            echo "🎛️  pgAdmin Auth: http://localhost:5050 (admin@auth.local / admin)"
            echo "🎛️  pgAdmin Video: http://localhost:5051 (admin@video.local / admin)"
            echo "🎮 Redis Commander: http://localhost:8081"
            echo "🐰 RabbitMQ UI: http://localhost:15672 (guest / guest)"
        else
            docker compose --profile "$profile" up -d
            echo "✅ Infrastructure started (profile: $profile)!"
        fi

        echo ""
        echo "📊 Services:"
        if [ "$profile" = "auth" ] || [ "$profile" = "full" ]; then
            echo "  Auth PostgreSQL: localhost:5432"
        fi
        if [ "$profile" = "video" ] || [ "$profile" = "full" ]; then
            echo "  Video PostgreSQL: localhost:5433"
            echo "  Redis: localhost:6379"
            echo "  RabbitMQ: localhost:5672"
            echo "  RabbitMQ UI: http://localhost:15672"
        fi
        ;;

    "stop")
        echo "🛑 Stopping platform infrastructure..."
        docker compose --profile full --profile admin down
        echo "✅ All services stopped!"
        ;;

    "restart")
        profile=${2:-"full"}
        echo "🔄 Restarting platform infrastructure (profile: $profile)..."
        docker compose --profile "$profile" down
        docker compose --profile "$profile" up -d
        echo "✅ Services restarted!"
        ;;

    "logs")
        service=${2:-""}
        if [ -z "$service" ]; then
            docker compose logs -f
        else
            docker compose logs -f "$service"
        fi
        ;;

    "status")
        echo "📋 Platform infrastructure status:"
        docker compose ps
        ;;

    "clean")
        echo "🧹 Cleaning up platform infrastructure..."
        echo "⚠️  This will delete ALL data including:"
        echo "   - Auth database data"
        echo "   - Video database data"
        echo "   - Redis cache"
        echo "   - RabbitMQ queues"
        echo ""
        read -p "Are you sure? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker compose --profile full --profile admin down -v
            docker compose rm -f
            docker volume rm platform_postgres_auth_data platform_postgres_video_data platform_redis_data platform_rabbitmq_data platform_pgadmin_auth_data platform_pgadmin_video_data 2>/dev/null || true
            echo "✅ Cleanup complete!"
        else
            echo "❌ Cleanup cancelled."
        fi
        ;;

    "backup-auth")
        echo "💾 Creating auth database backup..."
        timestamp=$(date +%Y%m%d_%H%M%S)
        backup_file="auth_backup_${timestamp}.sql"
        docker compose exec postgres-auth pg_dump -U app_user auth_db > "$backup_file"
        echo "✅ Auth backup created: $backup_file"
        ;;

    "backup-video")
        echo "💾 Creating video database backup..."
        timestamp=$(date +%Y%m%d_%H%M%S)
        backup_file="video_backup_${timestamp}.sql"
        docker compose exec postgres-video pg_dump -U upload_user video_platform > "$backup_file"
        echo "✅ Video backup created: $backup_file"
        ;;

    "restore-auth")
        if [ -z "$2" ]; then
            echo "❌ Please specify backup file: ./platform restore-auth backup_file.sql"
            exit 1
        fi
        echo "📥 Restoring auth database from $2..."
        docker compose exec -T postgres-auth psql -U app_user auth_db < "$2"
        echo "✅ Auth database restored!"
        ;;

    "restore-video")
        if [ -z "$2" ]; then
            echo "❌ Please specify backup file: ./platform restore-video backup_file.sql"
            exit 1
        fi
        echo "📥 Restoring video database from $2..."
        docker compose exec -T postgres-video psql -U upload_user video_platform < "$2"
        echo "✅ Video database restored!"
        ;;

    "shell-auth")
        echo "🐘 Connecting to auth PostgreSQL..."
        docker compose exec postgres-auth psql -U app_user auth_db
        ;;

    "shell-video")
        echo "🐘 Connecting to video PostgreSQL..."
        docker compose exec postgres-video psql -U upload_user video_platform
        ;;

    "shell-redis")
        echo "🔴 Connecting to Redis..."
        docker compose exec redis redis-cli
        ;;

    *)
        show_help
        exit 1
        ;;
esac
