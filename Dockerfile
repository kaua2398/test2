# Estágio 1: Build da aplicação React
FROM node:18-alpine AS build
WORKDIR /app
# Corrigido para copiar package.json e package-lock.json
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Estágio 2: Servir os arquivos com Nginx
FROM nginx:stable-alpine
COPY --from=build /app/dist /usr/share/nginx/html
# LINHA ADICIONADA: Copia o arquivo de configuração do Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]