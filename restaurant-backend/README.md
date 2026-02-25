# Restaurant Order System - Backend

Spring Boot 3.x + MySQL + Redis

## 技术栈

- Java 21 (GraalVM)
- Spring Boot 3.2.x
- MyBatis-Plus 3.5.x
- MySQL 8.0
- Redis 7.x
- JWT
- WebSocket

## 项目结构

```
restaurant-backend/
├── src/main/java/com/restaurant/
│   ├── config/         # 配置类
│   ├── common/         # 公共模块
│   ├── auth/           # 认证模块
│   ├── user/           # 用户模块
│   ├── dish/           # 菜品模块
│   ├── order/          # 订单模块
│   ├── table/          # 桌台模块
│   ├── report/         # 统计模块
│   ├── websocket/      # WebSocket
│   └── utils/          # 工具类
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── mapper/         # XML映射
└── pom.xml
```

## 运行

```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 打包
mvn clean package

# 运行jar
java -jar target/restaurant-backend-1.0.0.jar
```

## API文档

启动后访问：http://localhost:8080/swagger-ui.html
