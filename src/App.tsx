import React, { useState } from 'react';
import { User, Mail, FileText, Clock, Send, CheckCircle, AlertCircle, Database, Shield } from 'lucide-react';

interface FormData {
  requester_name: string;
  requester_email: string;
  reason: string;
  duration_hours: string;
  application: string;
}

interface FormStatus {
  type: 'idle' | 'loading' | 'success' | 'error';
  message?: string;
}

function App() {
  const [formData, setFormData] = useState<FormData>({
    requester_name: '',
    requester_email: '',
    reason: '',
    duration_hours: '',
    application: ''
  });

  const [errors, setErrors] = useState<Partial<FormData>>({});
  const [status, setStatus] = useState<FormStatus>({ type: 'idle' });

  const validateEmail = (email: string) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  };

  const validateForm = () => {
    const newErrors: Partial<FormData> = {};

    if (!formData.requester_name.trim()) {
      newErrors.requester_name = 'Nome é obrigatório';
    }

    if (!formData.requester_email.trim()) {
      newErrors.requester_email = 'E-mail é obrigatório';
    } else if (!validateEmail(formData.requester_email)) {
      newErrors.requester_email = 'E-mail inválido';
    }

    if (!formData.reason.trim()) {
      newErrors.reason = 'Descrição/Motivo é obrigatório';
    }

    if (!formData.duration_hours.trim()) {
      newErrors.duration_hours = 'Duração é obrigatória';
    } else if (isNaN(Number(formData.duration_hours)) || Number(formData.duration_hours) <= 0) {
      newErrors.duration_hours = 'Duração deve ser um número maior que 0';
    }

    if (!formData.application) {
      newErrors.application = 'Selecione a aplicação';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleInputChange = (field: keyof FormData, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    
    if (errors[field]) {
      setErrors(prev => ({ ...prev, [field]: undefined }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setStatus({ type: 'loading' });

    try {
      const response = await fetch('/api/solicitacao', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
        body: JSON.stringify(formData)
      });

      if (response.ok || response.status === 200) {
        setStatus({ 
          type: 'success', 
          message: 'Solicitação enviada com sucesso! Você receberá uma resposta em breve.' 
        });
        setFormData({
          requester_name: '',
          requester_email: '',
          reason: '',
          duration_hours: '',
          application: ''
        });
      } else {
        const errorText = await response.text();
        console.error('Erro do servidor:', response.status, errorText);
        throw new Error(`Erro ${response.status}: ${response.statusText}`);
      }
    } catch (error) {
      console.error('Erro detalhado:', error);
      setStatus({ 
        type: 'error', 
        message: error instanceof Error && error.message.includes('fetch') 
          ? 'Erro de conexão. Verifique sua internet e tente novamente.'
          : 'Erro ao enviar solicitação. Tente novamente.' 
      });
    }
  };

  const inputClasses = (fieldName: keyof FormData) => `
    w-full pr-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200 text-sm sm:text-base
    ${errors[fieldName] 
      ? 'border-red-500 bg-red-50' 
      : 'border-gray-300 hover:border-blue-400 focus:border-blue-500'
    }
  `;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 py-6 sm:py-8 px-4">
      <div className="max-w-3xl mx-auto">
        {/* Header Section */}
        <div className="text-center mb-6 sm:mb-10">
          <div className="flex items-center justify-center mb-5">
            <div className="w-16 h-16 bg-gradient-to-br from-blue-600 to-indigo-700 rounded-2xl flex items-center justify-center shadow-lg">
              <Database className="w-8 h-8 text-white" />
            </div>
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 mb-3">
            Solicitação de Acesso
          </h1>
          <p className="text-base sm:text-lg text-gray-600 mb-2">
            Banco de Produção Oracle
          </p>
          <div className="flex items-center justify-center text-xs text-gray-500">
            <Shield className="w-4 h-4 mr-2" />
            <span>Sistema seguro de solicitação de acesso</span>
          </div>
        </div>

        <div className="bg-white rounded-2xl shadow-xl border border-gray-200 overflow-hidden">
          <div className="bg-gradient-to-r from-blue-600 to-indigo-700 px-6 py-4">
            <h2 className="text-lg sm:text-xl font-semibold text-white">Dados da Solicitação</h2>
            <p className="text-blue-100 mt-1 text-sm">Preencha todos os campos obrigatórios abaixo</p>
          </div>
          
          <div className="p-6">

          {/* Status Messages */}
          {status.type !== 'idle' && (
            <div className={`mb-6 p-4 rounded-lg flex items-start shadow-sm
              ${status.type === 'success' ? 'bg-green-50 border border-green-200' : ''}
              ${status.type === 'error' ? 'bg-red-50 border border-red-200' : ''}
            `}>
              {status.type === 'success' && <CheckCircle className="w-5 h-5 text-green-600 mr-3 flex-shrink-0" />}
              {status.type === 'error' && <AlertCircle className="w-5 h-5 text-red-600 mr-3 flex-shrink-0" />}
              <span className={`font-medium text-sm
                ${status.type === 'success' ? 'text-green-800' : ''}
                ${status.type === 'error' ? 'text-red-800' : ''}
              `}>{status.message}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
              {/* Nome */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                Nome do Solicitante *
                </label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                  <input
                    type="text"
                    value={formData.requester_name}
                    onChange={(e) => handleInputChange('requester_name', e.target.value)}
                    className={`${inputClasses('requester_name')} h-11 pl-10`}
                    placeholder="Digite seu nome completo"
                  />
                </div>
                {errors.requester_name && (
                  <p className="mt-1.5 text-xs text-red-600 font-medium">{errors.requester_name}</p>
                )}
              </div>

              {/* Email */}
              <div>
                <label className="block text-sm font-semibold text-gray-700 mb-2">
                E-mail do Solicitante *
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                  <input
                    type="email"
                    value={formData.requester_email}
                    onChange={(e) => handleInputChange('requester_email', e.target.value)}
                    className={`${inputClasses('requester_email')} h-11 pl-10`}
                    placeholder="Digite seu e-mail"
                  />
                </div>
                {errors.requester_email && (
                  <p className="mt-1.5 text-xs text-red-600 font-medium">{errors.requester_email}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                {/* Coluna 1: Descrição/Motivo */}
                <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-2">
                    Descrição/Motivo *
                    </label>
                    <div className="relative">
                        <FileText className="absolute left-3 top-3 text-gray-400 w-5 h-5" />
                        <textarea
                          value={formData.reason}
                          onChange={(e) => handleInputChange('reason', e.target.value)}
                          className={`${inputClasses('reason')} min-h-[140px] pt-2.5 resize-none pl-10`}
                          placeholder="Descreva o motivo"
                          rows={6}
                        />
                    </div>
                    {errors.reason && (
                      <p className="mt-1.5 text-xs text-red-600 font-medium">{errors.reason}</p>
                    )}
                </div>

                {/* Coluna 2: Aplicação e Duração */}
                <div className="space-y-5">
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">Aplicação *</label>
                        <div className="grid grid-cols-2 gap-2">
                          {[
                            { value: 'VSLBank', label: 'VSLBank' },
                            { value: 'Portal ValeShop', label: 'Portal ValeShop' },
                            { value: 'Sistema Interno/Forms', label: 'Sistema Interno/Forms' },
                            { value: 'App Benefícios', label: 'App Benefícios' },
                            { value: 'Frotas', label: 'Frotas' },
                            { value: 'Sankya', label: 'Sankya' },
                            { value: 'Autorizador', label: 'Autorizador' }
                          ].map(app => (
                            <label key={app.value} className={`flex items-center px-3 py-1.5 rounded-md border cursor-pointer transition-all duration-150 text-sm font-medium
                              ${formData.application === app.value ? 'bg-blue-600 text-white border-blue-600' : errors.application ? 'border-red-500 bg-red-50 text-red-700' : 'bg-white border-gray-300 hover:border-blue-400'}
                            `}>
                              <input
                                type="radio"
                                name="application"
                                value={app.value}
                                checked={formData.application === app.value}
                                onChange={() => handleInputChange('application', app.value)}
                                className="mr-2 accent-blue-600"
                              />
                              {app.label}
                            </label>
                          ))}
                        </div>
                        {errors.application && (
                          <p className="mt-1.5 text-xs text-red-600 font-medium">{errors.application}</p>
                        )}
                    </div>
                    <div>
                        <label className="block text-sm font-semibold text-gray-700 mb-2">
                        Duração (horas) *
                        </label>
                        <div className="relative">
                          <Clock className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                          <input
                            type="number"
                            min="1"
                            value={formData.duration_hours}
                            onChange={(e) => handleInputChange('duration_hours', e.target.value)}
                            className={`${inputClasses('duration_hours')} h-11 pl-10`}
                            placeholder="Ex: 8"
                          />
                        </div>
                        {errors.duration_hours && (
                          <p className="mt-1.5 text-xs text-red-600 font-medium">{errors.duration_hours}</p>
                        )}
                         <p className="mt-1.5 text-xs text-gray-500">
                           Tempo necessário de acesso
                         </p>
                    </div>
                </div>
            </div>

            {/* Submit Button */}
            <div className="pt-4 border-t border-gray-100">
              <button
                type="submit"
                disabled={status.type === 'loading'}
                className="w-full bg-gradient-to-r from-blue-600 to-indigo-700 hover:from-blue-700 hover:to-indigo-800 disabled:from-blue-400 disabled:to-indigo-500 text-white font-semibold py-3 px-6 rounded-lg transition-all duration-300 flex items-center justify-center space-x-2 disabled:cursor-not-allowed transform hover:scale-[1.01] shadow-md hover:shadow-lg text-base"
              >
                {status.type === 'loading' ? (
                  <>
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Enviando...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-5 h-5" />
                    <span>Enviar Solicitação</span>
                  </>
                )}
              </button>
            </div>
          </form>

            {/* Footer */}
            <div className="mt-6 pt-4 border-t border-gray-100 text-center">
              <div className="flex items-center justify-center space-x-6 text-xs text-gray-500">
                <div className="flex items-center">
                  <Shield className="w-4 h-4 mr-1.5" />
                  <span>Análise de segurança</span>
                </div>
                <div className="flex items-center">
                  <Clock className="w-4 h-4 mr-1.5" />
                  <span>Resposta em até 24h</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        {/* Additional Info */}
        <div className="mt-6 text-center">
          <p className="text-xs text-gray-600">
            Em caso de dúvidas, entre em contato.
          </p>
        </div>
      </div>
    </div>
  );
}

export default App;
