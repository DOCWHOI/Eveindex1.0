# 构建阶段
FROM node:18-alpine AS builder

# 设置构建参数
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=1.0.0

# 设置标签
LABEL maintainer="certification-monitor-team" \
      org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="certification-monitor-frontend" \
      org.label-schema.description="认证监控预警系统前端服务" \
      org.label-schema.version=$VERSION \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.schema-version="1.0"

# 设置工作目录
WORKDIR /app

# 复制package文件
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production --silent && \
    npm cache clean --force

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# 生产阶段 - 使用Nginx
FROM nginx:alpine

# 设置构建参数
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=1.0.0

# 设置标签
LABEL maintainer="certification-monitor-team" \
      org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="certification-monitor-frontend" \
      org.label-schema.description="认证监控预警系统前端服务" \
      org.label-schema.version=$VERSION \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.schema-version="1.0"

# 安装必要工具
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制自定义nginx配置
COPY nginx.conf /etc/nginx/nginx.conf

# 复制构建的文件到nginx目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 创建nginx用户
RUN addgroup -g 101 -S nginx && \
    adduser -S -D -H -u 101 -h /var/cache/nginx -s /sbin/nologin -G nginx -g nginx nginx

# 设置权限
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d

# 创建日志目录
RUN mkdir -p /var/log/nginx && \
    touch /var/log/nginx/access.log /var/log/nginx/error.log && \
    chown -R nginx:nginx /var/log/nginx

# 切换到nginx用户
USER nginx

# 暴露端口
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/ || exit 1

# 启动nginx
CMD ["nginx", "-g", "daemon off;"]