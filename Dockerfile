FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN ./mvnw dependency:go-offline

# Copia o resto do código fonte da aplicação.
COPY src ./src

# Executa o build. O -DskipTests acelera o processo por não rodar os testes.
RUN ./mvnw clean package -DskipTests


# Stage 2: Cria a imagem final para execução
# Usamos uma imagem JRE (Java Runtime Environment), que é muito menor que a imagem JDK,
# pois só precisamos do necessário para rodar a aplicação, não para a compilar.
FROM eclipse-temurin:21-jre

# Define o diretório de trabalho.
WORKDIR /app

# Copia apenas o ficheiro .jar gerado na fase de build para a imagem final.
# Isto resulta numa imagem muito mais pequena e segura.
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta que a aplicação Spring Boot usa por defeito.
EXPOSE 8080

# Define o comando que será executado quando o container iniciar.
ENTRYPOINT ["java","-jar","app.jar"]