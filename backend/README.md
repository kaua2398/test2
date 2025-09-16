# Backend - Solicitação de Acesso

## Como rodar

1. Copie o arquivo `.env.example` para `.env` e ajuste as variáveis se necessário.
2. Instale as dependências:
   ```sh
   npm install
   ```
3. Rode em modo desenvolvimento:
   ```sh
   npm run dev
   ```

O backend ficará disponível em `http://localhost:3333` (ou porta definida no `.env`).

## Endpoint

- `POST /api/solicitacao`
  - Body (JSON):
    ```json
    {
      "requester_name": "Nome",
      "requester_email": "email@exemplo.com",
      "reason": "Motivo",
      "duration_hours": "8"
    }
    ```

- O backend repassa os dados ao webhook do n8n, protegendo a URL.
