#!/bin/bash

# =============================================================================
# 服务器环境初始化脚本
# 在目标服务器上运行此脚本来准备部署环境
# =============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查系统
check_system() {
    log_info "检查系统信息..."
    echo "操作系统: $(lsb_release -d | cut -f2)"
    echo "内核版本: $(uname -r)"
    echo "架构: $(uname -m)"
}

# 更新系统
update_system() {
    log_info "更新系统包..."
    sudo apt update && sudo apt upgrade -y
    sudo apt install -y curl wget git unzip software-properties-common
}

# 安装Java
install_java() {
    log_info "安装Java 17..."
    
    if java -version 2>&1 | grep -q "17"; then
        log_success "Java 17 已安装"
        return
    fi
    
    sudo apt install -y openjdk-17-jdk
    
    # 设置JAVA_HOME
    echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' | sudo tee -a /etc/environment
    echo 'export PATH=$PATH:$JAVA_HOME/bin' | sudo tee -a /etc/environment
    source /etc/environment
    
    java -version
    log_success "Java 17 安装完成"
}

# 安装Maven
install_maven() {
    log_info "安装Maven..."
    
    if mvn -version 2>&1 | grep -q "Apache Maven"; then
        log_success "Maven 已安装"
        return
    fi
    
    sudo apt install -y maven
    mvn -version
    log_success "Maven 安装完成"
}

# 安装Node.js
install_nodejs() {
    log_info "安装Node.js 18..."
    
    if node -v 2>&1 | grep -q "v18"; then
        log_success "Node.js 18 已安装"
        return
    fi
    
    # 安装Node.js 18
    curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
    sudo apt-get install -y nodejs
    
    # 安装yarn (可选)
    npm install -g yarn
    
    node -v
    npm -v
    log_success "Node.js 18 安装完成"
}

# 安装Nginx
install_nginx() {
    log_info "安装和配置Nginx..."
    
    if nginx -v 2>&1 | grep -q "nginx"; then
        log_success "Nginx 已安装"
    else
        sudo apt install -y nginx
    fi
    
    # 启动并启用Nginx
    sudo systemctl start nginx
    sudo systemctl enable nginx
    
    # 配置防火墙
    sudo ufw allow 'Nginx Full' 2>/dev/null || true
    sudo ufw allow ssh 2>/dev/null || true
    
    # 创建项目专用配置目录
    sudo mkdir -p /etc/nginx/sites-available
    sudo mkdir -p /etc/nginx/sites-enabled
    
    log_success "Nginx 安装完成"
}

# 安装PM2
install_pm2() {
    log_info "安装PM2..."
    
    if pm2 -v 2>&1 | grep -q "[0-9]"; then
        log_success "PM2 已安装"
        return
    fi
    
    sudo npm install -g pm2
    
    # 设置PM2开机启动
    pm2 startup | grep -E '^sudo' | bash || true
    
    log_success "PM2 安装完成"
}

# 创建项目目录和用户
setup_project_structure() {
    log_info "创建项目目录结构..."
    
    # 创建项目目录
    sudo mkdir -p /opt/AAAA
    sudo mkdir -p /opt/backups
    sudo mkdir -p /var/log/AAAA
    
    # 设置权限
    sudo chown -R $USER:$USER /opt/AAAA
    sudo chown -R $USER:$USER /opt/backups
    
    # 创建日志目录
    sudo mkdir -p /opt/AAAA/logs
    sudo chmod 755 /opt/AAAA/logs
    
    log_success "项目目录结构创建完成"
}

# 配置Git
setup_git() {
    log_info "配置Git..."
    
    # 设置Git全局配置（如果还没有设置）
    if ! git config --global user.name 2>/dev/null; then
        git config --global user.name "Deploy Bot"
        git config --global user.email "deploy@example.com"
    fi
    
    # 克隆项目（如果还没有克隆）
    if [ ! -d "/opt/AAAA/.git" ]; then
        log_info "首次克隆项目..."
        # 注意：请替换为您的实际仓库地址
        # git clone https://github.com/yourusername/AAAA.git /opt/AAAA
        log_warning "请手动克隆项目到 /opt/AAAA"
    fi
    
    log_success "Git 配置完成"
}

# 配置系统服务
setup_systemd_services() {
    log_info "配置系统服务..."
    
    # 创建AAAA后端服务文件
    sudo tee /etc/systemd/system/aaaa-backend.service > /dev/null <<EOF
[Unit]
Description=AAAA Spring Boot Backend
After=network.target

[Service]
Type=simple
User=$USER
WorkingDirectory=/opt/AAAA/spring-boot-backend
ExecStart=/usr/bin/java -jar target/spring-boot-backend-1.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=aaaa-backend

[Install]
WantedBy=multi-user.target
EOF
    
    # 重新加载systemd
    sudo systemctl daemon-reload
    
    log_success "系统服务配置完成"
}

# 配置防火墙
setup_firewall() {
    log_info "配置防火墙..."
    
    # 启用ufw（如果还没有启用）
    sudo ufw --force enable 2>/dev/null || true
    
    # 允许必要的端口
    sudo ufw allow ssh
    sudo ufw allow 80/tcp
    sudo ufw allow 443/tcp
    sudo ufw allow 8080/tcp
    
    sudo ufw status
    log_success "防火墙配置完成"
}

# 优化系统性能
optimize_system() {
    log_info "优化系统性能..."
    
    # 增加文件描述符限制
    echo "* soft nofile 65536" | sudo tee -a /etc/security/limits.conf
    echo "* hard nofile 65536" | sudo tee -a /etc/security/limits.conf
    
    # JVM优化
    echo 'export JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"' | sudo tee -a /etc/environment
    
    log_success "系统优化完成"
}

# 安装监控工具
install_monitoring() {
    log_info "安装监控工具..."
    
    # 安装htop
    sudo apt install -y htop iotop nethogs
    
    # 安装logrotate配置
    sudo tee /etc/logrotate.d/aaaa > /dev/null <<EOF
/opt/AAAA/logs/*.log {
    daily
    missingok
    rotate 30
    compress
    delaycompress
    notifempty
    copytruncate
}
EOF
    
    log_success "监控工具安装完成"
}

# 验证安装
verify_installation() {
    log_info "验证安装..."
    
    echo "=== 软件版本信息 ==="
    echo "Java: $(java -version 2>&1 | head -1)"
    echo "Maven: $(mvn -version 2>&1 | head -1)"
    echo "Node.js: $(node -v)"
    echo "npm: $(npm -v)"
    echo "Nginx: $(nginx -v 2>&1)"
    echo "PM2: $(pm2 -v)"
    echo
    
    echo "=== 服务状态 ==="
    echo "Nginx: $(sudo systemctl is-active nginx)"
    echo
    
    echo "=== 目录结构 ==="
    ls -la /opt/
    echo
    
    log_success "环境验证完成"
}

# 显示后续步骤
show_next_steps() {
    echo
    echo "=========================================="
    echo "           环境初始化完成"
    echo "=========================================="
    echo "接下来的步骤："
    echo "1. 在GitHub仓库中设置以下Secrets："
    echo "   - HOST: $(curl -s ifconfig.me 2>/dev/null || echo '您的服务器IP')"
    echo "   - USERNAME: $USER"
    echo "   - PASSWORD: 您的服务器密码"
    echo "   - PORT: 22"
    echo
    echo "2. 克隆您的项目到 /opt/AAAA"
    echo "   git clone https://github.com/DOCWHOI/Eveindex1.0.git"
    echo
    echo "3. 推送代码到main分支触发自动部署"
    echo
    echo "4. 访问您的应用："
    echo "   前端: http://$(curl -s ifconfig.me 2>/dev/null || echo '您的服务器IP')"
    echo "   后端: http://$(curl -s ifconfig.me 2>/dev/null || echo '您的服务器IP'):8080"
    echo "=========================================="
}

# 主函数
main() {
    log_info "开始初始化服务器环境..."
    
    check_system
    update_system
    install_java
    install_maven
    install_nodejs
    install_nginx
    install_pm2
    setup_project_structure
    setup_git
    setup_systemd_services
    setup_firewall
    optimize_system
    install_monitoring
    verify_installation
    show_next_steps
    
    log_success "服务器环境初始化完成！"
}

# 执行主函数
main "$@"
