import express from 'express';
import cors from 'cors';
import axios from 'axios';
import { PORT, WEBHOOK_URL } from './env';

const app = express();

// Configurar CORS para aceitar requisições do frontend
app.use(cors());
app.use(express.json());

// Endpoint para receber o formulário
app.post('/api/solicitacao', async (req, res) => {
  // CORREÇÃO: Adicionado o campo "application" na desestruturação
  const { requester_name, requester_email, reason, duration_hours, application } = req.body;

  // CORREÇÃO: Adicionado "application" na validação
  if (!requester_name || !requester_email || !reason || !duration_hours || !application) {
    return res.status(400).json({ error: 'Campos obrigatórios faltando.' });
  }

  try {
    // Repassa para o webhook do n8n
    const response = await axios.post(WEBHOOK_URL, {
      requester_name,
      requester_email,
      reason,
      duration_hours,
      application // CORREÇÃO: Adicionado "application" para ser enviado ao webhook
    }, {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      }
    });

    return res.status(200).json({ success: true, message: 'Solicitação enviada com sucesso!' });
  } catch (error: any) {
    console.error('Erro ao enviar para o webhook:', error?.response?.data || error.message);
    return res.status(500).json({ error: 'Erro ao processar a solicitação.' });
  }
});

app.listen(PORT, () => {
  console.log(`Servidor backend rodando na porta ${PORT}`);
});
