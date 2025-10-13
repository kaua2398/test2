FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# LINHA ADICIONADA: Dá permissão de execução ao script do Maven
RUN chmod +x ./mvnw

RUN ./mvnw dependency:go-offline

# Copia o resto do código fonte da aplicação.
COPY src ./src

# Executa o build. O -DskipTests acelera o processo por não rodar os testes.
RUN ./mvnw clean package -DskipTests

# Stage 2: Cria a imagem final para execução
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]