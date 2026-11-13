# syntax=docker/dockerfile:1.7
# ============================================================
# Huixuetong 应用镜像（多阶段构建）
# 构建：docker build -t huixuetong-app:latest .
# 说明：项目 java.version=18，这里用 JDK 21 编译（--release 18），
#       运行用 JRE 21（向下兼容 Java 18 字节码）。
# ============================================================

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 阿里云镜像加速（deploy/maven/settings.xml 里已放行 spring milestone）
COPY deploy/maven/settings.xml /root/.m2/settings.xml

# 先拷 pom，利用层缓存；依赖走 BuildKit 缓存，避免每次全量下载
COPY pom.xml .
COPY src ./src
# 缓存只挂 repository 子目录：挂 /root/.m2 会把上面的 settings.xml 一起遮蔽掉
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -Dmaven.test.skip=true package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC"

# curl 仅用于 HEALTHCHECK
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/huixuetong-lms-*.jar /app/app.jar
RUN mkdir -p /app/logs /app/uploads/knowledge

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=Asia/Shanghai -jar /app/app.jar"]
