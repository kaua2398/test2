# Stage 1: Build da aplicação
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia arquivos essenciais
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline

# Copia o código-fonte
COPY src ./src

# Compila o projeto sem rodar testes
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia o artefato gerado
COPY --from=build /app/target/*.jar app.jar

# Copia o arquivo .env gerado pelo pipeline
COPY .env .env

# Exporta as variáveis do .env como variáveis de ambiente
RUN export $(cat .env | xargs)

# Porta padrão da aplicação
EXPOSE 8080

# Inicializa o Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
