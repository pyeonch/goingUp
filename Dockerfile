# Java 런타임 환경 이미지 사용
FROM openjdk:17-jdk-slim

# 작업 디렉터리 설정
WORKDIR /app

# JAR 파일 복사
COPY target/GoingUp2-1.0-SNAPSHOT.jar app.jar
COPY src/main/resources /app/resources/

# 실행 명령어 설정
ENTRYPOINT ["java", "-jar", "app.jar"]