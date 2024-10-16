# Используем образ с Maven для сборки проекта с поддержкой Java 22
FROM maven:3.8.6-openjdk-22 AS build
WORKDIR /app

# Копируем pom.xml и исходный код в контейнер
COPY pom.xml .
COPY src ./src

# Сборка проекта без тестов
RUN mvn clean package -DskipTests

# Используем образ OpenJDK для запуска Spring Boot приложения
FROM openjdk:22-jdk-slim
WORKDIR /app

# Копируем скомпилированный JAR файл
COPY --from=build /app/target/labQueuesBot-0.0.1-SNAPSHOT.jar /app/labQueuesBot.jar

# Открываем порт по умолчанию для Spring Boot (8080)
EXPOSE 8080

# Запускаем Spring Boot приложение
ENTRYPOINT ["java", "-jar", "/app/labQueuesBot.jar"]
