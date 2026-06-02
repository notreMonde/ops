#!/bin/bash
set -e

SERVICE_NAME="maintenance-decision-service"
COMPOSE_FILE="docker-compose.yml"

build() {
    echo ">>> 构建 Docker 镜像..."
    docker-compose -f $COMPOSE_FILE build
    echo ">>> 构建完成"
}

start() {
    echo ">>> 启动服务..."
    docker-compose -f $COMPOSE_FILE up -d
    echo ">>> 服务已启动"
    status
}

stop() {
    echo ">>> 停止服务..."
    docker-compose -f $COMPOSE_FILE down
    echo ">>> 服务已停止"
}

restart() {
    stop
    start
}

logs() {
    docker-compose -f $COMPOSE_FILE logs -f --tail=100
}

status() {
    echo ">>> 服务状态："
    docker-compose -f $COMPOSE_FILE ps
    echo ""
    echo ">>> 健康检查："
    curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:8080/actuator/health
}

case "${1:-help}" in
    build)   build ;;
    start)   start ;;
    stop)    stop ;;
    restart) restart ;;
    logs)    logs ;;
    status)  status ;;
    *)
        echo "用法: $0 {build|start|stop|restart|logs|status}"
        exit 1
        ;;
esac
