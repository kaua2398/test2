# ---------- deps (com dev) ----------
FROM node:18-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm install

# ---------- build (gera dist) ----------
FROM deps AS build
WORKDIR /app
COPY . .            # <--- contexto já é backend/
RUN npm run build   # precisa do "build" no package.json (tsc)

# ---------- runtime (prod only) ----------
FROM node:18-alpine
WORKDIR /app
ENV NODE_ENV=production
# Só deps de produção
COPY package*.json ./
RUN npm install --omit=dev

# Copia o build final
COPY --from=build /app/dist ./dist

# Se você precisa de assets/config do runtime, copie aqui:
# COPY --from=build /app/.env ./.env

EXPOSE 3000
CMD ["node", "dist/index.js"]
