# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

使用中文回复

## Project Overview
Spring Boot 3.5.4 web application with MyBatis-Plus ORM and Hutool utilities. Uses MySQL database and follows standard Spring Boot conventions.

有涉及MySQL交互尽量使用MyBatisPlus和Hutool工具类

所有的类,属性,方法上都使用JavaDoc 描述功能,目的

设计新的表，需要生成相应的MySQL语句，

Java属性使用小驼峰命名,使用mybatisplus的 @TableName标记表名, @TableField指定下划线命名

## Core Commands
```bash
# Development
./mvnw spring-boot:run                    # Start application
./mvnw clean package                      # Build JAR
./mvnw test                              # Run all tests
./mvnw test -Dtest=DemoApplicationTests  # Run single test

# Database
# Configure MySQL connection in application.properties before running
```

## Key Dependencies
- **MyBatis-Plus 3.5.5**: ORM with pagination, logic delete, auto-fill
- **Hutool 5.8.25**: Utility library for string, date, file operations
- **MySQL**: Primary database
- **Lombok**: Code generation

## Architecture
Standard Spring Boot layered architecture:
- `src/main/java/com/example/demo/` - Main application code
- `src/main/resources/application.properties` - Configuration including MyBatis-Plus settings
- Entity classes expected in `com.example.demo.entity` package
- Mapper XML files expected in `classpath*:/mapper/**/*Mapper.xml`

## Configuration Highlights
- Database: MySQL with standard connection settings
- MyBatis-Plus: camelCase mapping, SQL logging, pagination enabled
- Logic delete: `deleted` field with values 0/1