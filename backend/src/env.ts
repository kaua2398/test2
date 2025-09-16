import dotenv from 'dotenv';
dotenv.config();

export const PORT = process.env.PORT || 3333;
export const WEBHOOK_URL = process.env.WEBHOOK_URL || 'https://n8n.valeshop.com.br/webhook/oracle-access/form';
