#!/bin/bash

# 认证监控预警系统 Docker 部署脚本
# 使用方法: ./scripts/docker-deploy.sh [dev|prod] [start|stop|restart|logs|clean]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置变量
PROJECT_NAME="certification-monitor"
DEV_COMPOSE_FILE="docker-compose.dev.yml"
PROD_COMPOSE_FILE="docker-compose.prod.yml"

# 打印带颜色的消息
print_message() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# 显示帮助信息
show_help() {
    echo "认证监控预警系统 Docker 部署脚本"
    echo ""
    echo "使用方法:"
    echo "  $0 [环境] [操作]"
    echo ""
    echo "环境:"
    echo "  dev     开发环境"
    echo "  prod    生产环境"
    echo ""
    echo "操作:"
    echo "  start   启动服务"
    echo "  stop    停止服务"
    echo "  restart 重启服务"
    echo "  logs    查看日志"
    echo "  clean   清理资源"
    echo "  status  查看状态"
    echo ""
    echo "示例:"
    echo "  $0 dev start     # 启动开发环境"
    echo "  $0 prod stop     # 停止生产环境"
    echo "  $0 dev logs      # 查看开发环境日志"
}

# 检查Docker和Docker Compose
check_requirements() {
    print_message $BLUE "检查系统要求..."
    
    if ! command -v docker &> /dev/null; then
        print_message $RED "错误: Docker 未安装"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_message $RED "错误: Docker Compose 未安装"
        exit 1
    fi
    
    print_message $GREEN "✓ Docker 和 Docker Compose 已安装"
}

# 检查环境配置文件
check_env_file() {
    local env=$1
    local env_file=".env.${env}"
    
    if [ ! -f "$env_file" ]; then
        print_message $YELLOW "警告: 环境配置文件 $env_file 不存在"
        print_message $YELLOW "请复制 docker-env-example.txt 为 $env_file 并配置相应参数"
        
        if [ "$env" = "dev" ]; then
            print_message $BLUE "开发环境可以使用默认配置，继续执行..."
        else
            print_message $RED "生产环境必须配置 $env_file 文件"
            exit 1
        fi
    else
        print_message $GREEN "✓ 环境配置文件 $env_file 存在"
    fi
}

# 启动服务
start_services() {
    local env=$1
    local compose_file=$2
    
    print_message $BLUE "启动 $env 环境服务..."
    
    # 加载环境变量
    if [ -f ".env.${env}" ]; then
        export $(cat .env.${env} | grep -v '^#' | xargs)
    fi
    
    # 启动服务
    docker-compose -f "$compose_file" -p "${PROJECT_NAME}-${env}" up -d
    
    print_message $GREEN "✓ $env 环境服务启动完成"
    print_message $BLUE "查看服务状态: $0 $env status"
    print_message $BLUE "查看服务日志: $0 $env logs"
}

# 停止服务
stop_services() {
    local env=$1
    local compose_file=$2
    
    print_message $BLUE "停止 $env 环境服务..."
    
    docker-compose -f "$compose_file" -p "${PROJECT_NAME}-${env}" down
    
    print_message $GREEN "✓ $env 环境服务已停止"
}

# 重启服务
restart_services() {
    local env=$1
    local compose_file=$2
    
    print_message $BLUE "重启 $env 环境服务..."
    
    stop_services "$env" "$compose_file"
    sleep 2
    start_services "$env" "$compose_file"
    
    print_message $GREEN "✓ $env 环境服务重启完成"
}

# 查看日志
show_logs() {
    local env=$1
    local compose_file=$2
    
    print_message $BLUE "查看 $env 环境服务日志..."
    
    docker-compose -f "$compose_file" -p "${PROJECT_NAME}-${env}" logs -f
}

# 查看状态
show_status() {
    local env=$1
    local compose_file=$2
    
    print_message $BLUE "查看 $env 环境服务状态..."
    
    docker-compose -f "$compose_file" -p "${PROJECT_NAME}-${env}" ps
}

# 清理资源
clean_resources() {
    local env=$1
    local compose_file=$2
    
    print_message $YELLOW "清理 $env 环境资源..."
    
    # 停止并删除容器
    docker-compose -f "$compose_file" -p "${PROJECT_NAME}-${env}" down -v
    
    # 删除镜像
    docker images | grep "${PROJECT_NAME}" | awk '{print $3}' | xargs -r docker rmi -f
    
    # 清理未使用的资源
    docker system prune -f
    
    print_message $GREEN "✓ $env 环境资源清理完成"
}

# 主函数
main() {
    local env=$1
    local action=$2
    
    # 检查参数
    if [ -z "$env" ] || [ -z "$action" ]; then
        show_help
        exit 1
    fi
    
    # 设置compose文件
    local compose_file=""
    case $env in
        "dev")
            compose_file=$DEV_COMPOSE_FILE
            ;;
        "prod")
            compose_file=$PROD_COMPOSE_FILE
            ;;
        *)
            print_message $RED "错误: 无效的环境 '$env'"
            show_help
            exit 1
            ;;
    esac
    
    # 检查compose文件是否存在
    if [ ! -f "$compose_file" ]; then
        print_message $RED "错误: Docker Compose 文件 '$compose_file' 不存在"
        exit 1
    fi
    
    # 检查系统要求
    check_requirements
    
    # 检查环境配置文件
    check_env_file "$env"
    
    # 执行操作
    case $action in
        "start")
            start_services "$env" "$compose_file"
            ;;
        "stop")
            stop_services "$env" "$compose_file"
            ;;
        "restart")
            restart_services "$env" "$compose_file"
            ;;
        "logs")
            show_logs "$env" "$compose_file"
            ;;
        "status")
            show_status "$env" "$compose_file"
            ;;
        "clean")
            clean_resources "$env" "$compose_file"
            ;;
        *)
            print_message $RED "错误: 无效的操作 '$action'"
            show_help
            exit 1
            ;;
    esac
}

# 执行主函数
main "$@"
