# 使用多阶段构建优化镜像大小
FROM maven:3.9.5-eclipse-temurin-17 AS builder

# 设置构建参数
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=1.0.0

# 设置标签
LABEL maintainer="certification-monitor-team" \
      org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="certification-monitor-backend" \
      org.label-schema.description="认证监控预警系统后端服务" \
      org.label-schema.version=$VERSION \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.schema-version="1.0"

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 下载依赖（利用Docker层缓存）
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B && \
    ./mvnw dependency:resolve-sources -B && \
    ./mvnw dependency:resolve-plugins -B

# 复制源代码
COPY src ./src

# 构建应用
RUN ./mvnw clean package -DskipTests -Dmaven.test.skip=true && \
    ./mvnw dependency:purge-local-repository -DmanualInclude="org.springframework.boot:spring-boot-maven-plugin"

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 设置构建参数
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=1.0.0

# 设置标签
LABEL maintainer="certification-monitor-team" \
      org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="certification-monitor-backend" \
      org.label-schema.description="认证监控预警系统后端服务" \
      org.label-schema.version=$VERSION \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.schema-version="1.0"

# 安装必要的工具
RUN apk add --no-cache \
    curl \
    tzdata \
    dumb-init \
    && rm -rf /var/cache/apk/*

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用用户和组
RUN addgroup -g 1001 -S spring && \
    adduser -S spring -u 1001 -G spring -h /app -s /bin/sh

# 设置工作目录
WORKDIR /app

# 从构建阶段复制JAR文件
COPY --from=builder /app/target/certification-monitor-*.jar app.jar

# 创建必要的目录
RUN mkdir -p /app/logs /app/data /app/config && \
    chown -R spring:spring /app

# 切换到应用用户
USER spring

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# 启动命令
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=docker -jar app.jar"]