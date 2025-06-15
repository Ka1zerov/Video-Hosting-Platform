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
    echo "  start [profile]       - Start infrastructure services"
    echo "  stop                 - Stop all services"
    echo "  restart [profile]    - Restart services"
    echo "  logs [service]       - Show logs (all or specific service)"
    echo "  status               - Show services status"
    echo "  clean                - Remove all containers and volumes"
    echo "  backup-auth          - Create auth database backup"
    echo "  backup-video         - Create video database backup"
    echo "  restore-auth         - Restore auth database from backup"
    echo "  restore-video        - Restore video database from backup"
    echo "  shell-auth           - Connect to auth database shell"
    echo "  shell-video          - Connect to video database shell"
    echo "  shell-redis          - Connect to Redis shell"
    echo "  replication-status   - Check PostgreSQL replication status"
    echo "  promote-slave        - Promote slave to master (emergency)"
    echo "  haproxy-stats        - Open HAProxy statistics page"
    echo ""
    echo "Profiles:"
    echo "  auth              - Only authentication services (postgres-auth)"
    echo "  video             - Video services (postgres master+slave, redis, rabbitmq, haproxy)"
    echo "  full              - All services (default)"
    echo "  admin             - All services + admin tools"
    echo ""
    echo "Port allocation:"
    echo "  Auth PostgreSQL:        localhost:5432"
    echo "  Video PostgreSQL Master: localhost:5433"
    echo "  Video PostgreSQL Slave:  localhost:5434"
    echo "  HAProxy Write (Master):  localhost:5435"
    echo "  HAProxy Read (Balanced): localhost:5436"
    echo "  Redis:                  localhost:6379"
    echo "  RabbitMQ:               localhost:5672"
    echo "  RabbitMQ UI:            http://localhost:15672"
    echo "  HAProxy Stats:          http://localhost:8404/stats"
    echo "  pgAdmin Auth:           http://localhost:5050"
    echo "  pgAdmin Video:          http://localhost:5051"
    echo "  Redis Commander:        http://localhost:8081"
    echo ""
    echo "Examples:"
    echo "  ./platform start auth              # Start only auth database"
    echo "  ./platform start video             # Start video services with replication"
    echo "  ./platform start full              # Start all services"
    echo "  ./platform start admin             # Start all + admin tools"
    echo "  ./platform logs postgres-video-master # Show master database logs"
    echo "  ./platform replication-status      # Check replication health"
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
            echo "📊 HAProxy Stats: http://localhost:8404/stats"
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
            echo "  Video PostgreSQL Master: localhost:5433"
            echo "  Video PostgreSQL Slave: localhost:5434"
            echo "  HAProxy Write (Master): localhost:5435"
            echo "  HAProxy Read (Balanced): localhost:5436"
            echo "  Redis: localhost:6379"
            echo "  RabbitMQ: localhost:5672"
            echo "  RabbitMQ UI: http://localhost:15672"
            echo "  HAProxy Stats: http://localhost:8404/stats"
            echo ""
            echo "⏳ Waiting for replication setup..."
            sleep 10
            echo "🔄 Checking replication status..."
            docker compose exec postgres-video-master psql -U upload_user -d video_platform -c "SELECT * FROM pg_stat_replication;" || echo "ℹ️  Replication will be available shortly"
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

    "replication-status")
        echo "🔄 PostgreSQL Replication Status:"
        echo ""
        echo "📊 Master Replication Stats:"
        docker compose exec postgres-video-master psql -U upload_user -d video_platform -c "
        SELECT 
            client_addr as slave_ip,
            usename,
            application_name,
            state,
            pg_wal_lsn_diff(pg_current_wal_lsn(), sent_lsn) as pending_bytes,
            pg_wal_lsn_diff(sent_lsn, write_lsn) as write_lag_bytes,
            pg_wal_lsn_diff(write_lsn, flush_lsn) as flush_lag_bytes,
            pg_wal_lsn_diff(flush_lsn, replay_lsn) as replay_lag_bytes,
            write_lag,
            flush_lag,
            replay_lag
        FROM pg_stat_replication;
        " || echo "❌ Master database not available"
        
        echo ""
        echo "📊 Slave Recovery Status:"
        docker compose exec postgres-video-slave psql -U upload_user -d video_platform -c "
        SELECT 
            CASE 
                WHEN pg_is_in_recovery() THEN 'In Recovery (Slave)'
                ELSE 'Not in Recovery (Master)'
            END as status,
            pg_last_wal_receive_lsn() as last_received_lsn,
            pg_last_wal_replay_lsn() as last_replayed_lsn,
            pg_last_xact_replay_timestamp() as last_replay_time;
        " || echo "❌ Slave database not available"
        
        echo ""
        echo "⚖️  HAProxy Backend Status:"
        curl -s http://localhost:8404/stats | grep -E "(postgres-master|postgres-slave|BACKEND)" | head -10 || echo "❌ HAProxy stats not available"
        ;;

    "promote-slave")
        echo "🚨 EMERGENCY: Promoting slave to master..."
        echo "⚠️  This will stop replication and promote slave as new master"
        echo "⚠️  Use only if master is completely unavailable"
        echo ""
        read -p "Are you sure? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker compose exec postgres-video-slave touch /tmp/postgresql.trigger
            echo "✅ Slave promotion triggered!"
            echo "📝 You'll need to reconfigure replication manually after recovery"
        else
            echo "❌ Promotion cancelled."
        fi
        ;;

    "haproxy-stats")
        echo "📊 Opening HAProxy statistics page..."
        echo "🔗 http://localhost:8404/stats"
        if command -v open >/dev/null 2>&1; then
            open http://localhost:8404/stats
        elif command -v xdg-open >/dev/null 2>&1; then
            xdg-open http://localhost:8404/stats
        else
            echo "💡 Open this URL in your browser: http://localhost:8404/stats"
        fi
        ;;

    "clean")
        echo "🧹 Cleaning up platform infrastructure..."
        echo "⚠️  This will delete ALL data including:"
        echo "   - Auth database data"
        echo "   - Video master database data"
        echo "   - Video slave database data"
        echo "   - Redis cache"
        echo "   - RabbitMQ queues"
        echo ""
        read -p "Are you sure? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            docker compose --profile full --profile admin down -v
            docker compose rm -f
            docker volume rm platform_postgres_auth_data platform_postgres_video_master_data platform_postgres_video_slave_data platform_redis_data platform_rabbitmq_data platform_pgladmin_auth_data platform_pgadmin_video_data 2>/dev/null || true
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
        echo "💾 Creating video database backup from master..."
        timestamp=$(date +%Y%m%d_%H%M%S)
        backup_file="video_backup_${timestamp}.sql"
        docker compose exec postgres-video-master pg_dump -U upload_user video_platform > "$backup_file"
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
        echo "📥 Restoring video database to master from $2..."
        docker compose exec -T postgres-video-master psql -U upload_user video_platform < "$2"
        echo "✅ Video database restored to master!"
        echo "ℹ️  Slave will automatically sync from master"
        ;;

    "shell-auth")
        echo "🐘 Connecting to auth PostgreSQL..."
        docker compose exec postgres-auth psql -U app_user auth_db
        ;;

    "shell-video")
        echo "🐘 Connecting to video PostgreSQL master..."
        docker compose exec postgres-video-master psql -U upload_user video_platform
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
