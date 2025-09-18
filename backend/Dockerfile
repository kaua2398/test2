FROM node:18-alpine
WORKDIR /app
# Copia apenas os arquivos de dependência do backend
COPY backend/package*.json ./
RUN npm install
# Copia o restante do código do backend
COPY backend/ .
RUN npm run build
EXPOSE 3333
CMD ["npm", "start"]