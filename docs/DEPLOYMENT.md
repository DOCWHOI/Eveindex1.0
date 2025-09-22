# 部署配置文档

## GitHub Actions 自动部署配置

### 1. 设置GitHub Secrets

在您的GitHub仓库中，进入 `Settings` > `Secrets and variables` > `Actions`，添加以下secrets：

#### 生产环境 (Production)
- `HOST`: `47.122.94.222` (您的服务器IP)
- `USERNAME`: `root` (服务器用户名)
- `PASSWORD`: `您的服务器密码`
- `PORT`: `22` (SSH端口，默认22)

#### 测试环境 (Staging) - 可选
- `STAGING_HOST`: 测试服务器IP
- `STAGING_USERNAME`: 测试服务器用户名
- `STAGING_PASSWORD`: 测试服务器密码
- `STAGING_PORT`: 测试服务器SSH端口

### 2. 服务器环境准备

#### 方法一：自动初始化
```bash
# 在服务器上运行
wget https://raw.githubusercontent.com/yourusername/AAAA/main/scripts/setup-server.sh
chmod +x setup-server.sh
./setup-server.sh
```

#### 方法二：手动安装
```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装Java 17
sudo apt install -y openjdk-17-jdk

# 安装Maven
sudo apt install -y maven

# 安装Node.js 18
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt-get install -y nodejs

# 安装Nginx
sudo apt install -y nginx
sudo systemctl start nginx
sudo systemctl enable nginx

# 创建项目目录
sudo mkdir -p /opt/AAAA
sudo chown -R $USER:$USER /opt/AAAA

# 克隆项目
git clone https://github.com/DOCWHOI/Eveindex1.0.git /opt/AAAA
```

### 3. 部署流程

#### 自动部署
1. 推送代码到 `main` 分支
2. GitHub Actions 自动触发部署
3. 系统自动构建、测试、部署

#### 手动部署
1. 进入 GitHub 仓库的 `Actions` 页面
2. 选择 `Deploy to Production Server` 工作流
3. 点击 `Run workflow` 手动触发

#### 回滚部署
1. 进入 GitHub 仓库的 `Actions` 页面
2. 选择 `Rollback Deployment` 工作流
3. 选择环境和备份时间戳
4. 点击 `Run workflow` 执行回滚

### 4. 监控和日志

#### 查看应用日志
```bash
# 后端日志
tail -f /opt/AAAA/logs/backend.log

# Nginx访问日志
tail -f /var/log/nginx/AAAA_access.log

# Nginx错误日志
tail -f /var/log/nginx/AAAA_error.log
```

#### 检查服务状态
```bash
# 检查Java进程
ps aux | grep java

# 检查端口占用
netstat -tlnp | grep :8080

# 检查Nginx状态
sudo systemctl status nginx

# 健康检查
curl http://localhost:8080/actuator/health
curl http://localhost/
```

### 5. 故障排除

#### 常见问题

**1. 后端服务启动失败**
```bash
# 查看启动日志
cat /opt/AAAA/logs/backend-startup.log

# 检查端口占用
sudo lsof -i :8080

# 手动启动测试
cd /opt/AAAA/spring-boot-backend
java -jar target/*.jar --spring.profiles.active=prod
```

**2. 前端页面无法访问**
```bash
# 检查Nginx配置
sudo nginx -t

# 重启Nginx
sudo systemctl restart nginx

# 检查文件权限
ls -la /var/www/html/
```

**3. GitHub Actions部署失败**
- 检查Secrets配置是否正确
- 查看Actions日志中的错误信息
- 验证服务器SSH连接
- 检查服务器磁盘空间和内存

#### 紧急恢复
```bash
# 快速回滚到上一个版本
cd /opt/AAAA
git reset --hard HEAD~1

# 重新构建和启动
cd spring-boot-backend
mvn clean package -DskipTests
pkill -f "spring-boot-backend"
nohup java -jar target/*.jar --spring.profiles.active=prod > ../logs/emergency.log 2>&1 &
```

### 6. 性能优化

#### JVM参数优化
```bash
# 在启动命令中添加JVM参数
java -Xms512m -Xmx2g -XX:+UseG1GC -jar target/*.jar
```

#### Nginx优化
```nginx
# 在Nginx配置中添加
gzip on;
gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

# 静态资源缓存
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### 7. 安全配置

#### 防火墙设置
```bash
sudo ufw enable
sudo ufw allow ssh
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

#### SSL证书配置 (可选)
```bash
# 使用Let's Encrypt
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d yourdomain.com
```

### 8. 备份策略

#### 自动备份脚本
```bash
#!/bin/bash
# backup.sh
BACKUP_DIR="/opt/backups"
PROJECT_DIR="/opt/AAAA"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份
tar -czf "$BACKUP_DIR/aaaa-backup-$DATE.tar.gz" -C "$PROJECT_DIR" .

# 保留最近10个备份
ls -t "$BACKUP_DIR"/aaaa-backup-*.tar.gz | tail -n +11 | xargs -r rm

echo "备份完成: aaaa-backup-$DATE.tar.gz"
```

#### 设置定时备份
```bash
# 添加到crontab
crontab -e

# 每天凌晨2点备份
0 2 * * * /opt/AAAA/scripts/backup.sh
```

## 联系信息

如果遇到部署问题，请：
1. 查看GitHub Actions日志
2. 检查服务器日志文件
3. 参考故障排除章节
4. 提交Issue到GitHub仓库
