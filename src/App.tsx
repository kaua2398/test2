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
    w-full pr-3 sm:pr-4 py-2 sm:py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all duration-200
    ${errors[fieldName] 
      ? 'border-red-500 bg-red-50' 
      : 'border-gray-300 hover:border-blue-400 focus:border-blue-500'
    }
  `;

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100 py-4 sm:py-8 px-3 sm:px-4">
      <div className="max-w-4xl mx-auto">
        {/* Header Section */}
        <div className="text-center mb-6 sm:mb-12">
          <div className="flex items-center justify-center mb-6">
            <div className="w-16 h-16 sm:w-20 sm:h-20 bg-gradient-to-br from-blue-600 to-indigo-700 rounded-2xl flex items-center justify-center shadow-lg">
              <Database className="w-8 h-8 sm:w-10 sm:h-10 text-white" />
            </div>
          </div>
          <h1 className="text-2xl sm:text-4xl font-bold text-gray-900 mb-4">
            Solicitação de Acesso
          </h1>
          <p className="text-lg sm:text-xl text-gray-600 mb-2">
            Banco de Produção Oracle
          </p>
          <div className="flex items-center justify-center text-xs sm:text-sm text-gray-500">
            <Shield className="w-4 h-4 mr-2" />
            <span>Sistema seguro de solicitação de acesso</span>
          </div>
        </div>

        <div className="bg-white rounded-3xl shadow-2xl border border-gray-200 overflow-hidden">
          <div className="bg-gradient-to-r from-blue-600 to-indigo-700 px-4 sm:px-8 py-4 sm:py-6">
            <h2 className="text-xl sm:text-2xl font-semibold text-white">Dados da Solicitação</h2>
            <p className="text-blue-100 mt-1 text-sm sm:text-base">Preencha todos os campos obrigatórios abaixo</p>
          </div>
          
          <div className="p-4 sm:p-8">

          {/* Status Messages */}
          {status.type === 'success' && (
            <div className="mb-6 sm:mb-8 p-4 sm:p-6 bg-gradient-to-r from-green-50 to-emerald-50 border border-green-200 rounded-xl flex items-start sm:items-center shadow-sm">
              <CheckCircle className="w-5 h-5 sm:w-6 sm:h-6 text-green-600 mr-3 mt-0.5 sm:mt-0 flex-shrink-0" />
              <span className="text-green-800 font-medium text-sm sm:text-base">{status.message}</span>
            </div>
          )}

          {status.type === 'error' && (
            <div className="mb-6 sm:mb-8 p-4 sm:p-6 bg-gradient-to-r from-red-50 to-rose-50 border border-red-200 rounded-xl flex items-start sm:items-center shadow-sm">
              <AlertCircle className="w-5 h-5 sm:w-6 sm:h-6 text-red-600 mr-3 mt-0.5 sm:mt-0 flex-shrink-0" />
              <span className="text-red-800 font-medium text-sm sm:text-base">{status.message}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-6 sm:space-y-8">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 sm:gap-8">
              {/* Nome */}
              <div>
                <label className="block text-sm sm:text-sm font-semibold text-gray-700 mb-2 sm:mb-3">
                Nome do Solicitante *
                </label>
                <div className="relative">
                  <User className="absolute left-3 sm:left-4 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4 sm:w-5 sm:h-5" />
                <input
                  type="text"
                  value={formData.requester_name}
                  onChange={(e) => handleInputChange('requester_name', e.target.value)}
                    className={`${inputClasses('requester_name')} h-12 sm:h-14 text-base sm:text-lg pl-10 sm:pl-10`}
                  placeholder="Digite seu nome completo"
                />
                </div>
                {errors.requester_name && (
                  <p className="mt-2 text-sm text-red-600 font-medium">{errors.requester_name}</p>
                )}
              </div>

              {/* Email */}
              <div>
                <label className="block text-sm sm:text-sm font-semibold text-gray-700 mb-2 sm:mb-3">
                E-mail do Solicitante *
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 sm:left-4 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4 sm:w-5 sm:h-5" />
                <input
                  type="email"
                  value={formData.requester_email}
                  onChange={(e) => handleInputChange('requester_email', e.target.value)}
                    className={`${inputClasses('requester_email')} h-12 sm:h-14 text-base sm:text-lg pl-10 sm:pl-10`}
                  placeholder="Digite seu e-mail"
                />
                </div>
                {errors.requester_email && (
                  <p className="mt-2 text-sm text-red-600 font-medium">{errors.requester_email}</p>
                )}
              </div>
            </div>

            {/* CORREÇÃO: Nova estrutura de grid para alinhar os campos */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 sm:gap-8">
              {/* Coluna 1: Descrição/Motivo */}
              <div className="lg:col-span-1">
                <label className="block text-sm sm:text-sm font-semibold text-gray-700 mb-2 sm:mb-3">
                  Descrição/Motivo *
                </label>
                <div className="relative">
                    <FileText className="absolute left-3 sm:left-4 top-3 sm:top-4 text-gray-400 w-4 h-4 sm:w-5 sm:h-5" />
                    <textarea
                      value={formData.reason}
                      onChange={(e) => handleInputChange('reason', e.target.value)}
                      className={`${inputClasses('reason')} w-full min-h-[180px] sm:min-h-[200px] pt-3 sm:pt-4 pb-3 sm:pb-4 text-base sm:text-lg resize-none pl-10 sm:pl-10`}
                      placeholder="Descreva o motivo da solicitação de acesso"
                      rows={8}
                    />
                </div>
                {errors.reason && (
                  <p className="mt-2 text-sm text-red-600 font-medium">{errors.reason}</p>
                )}
              </div>
              
              {/* Coluna 2: Aplicação */}
              <div className="lg:col-span-1">
                <label className="block text-sm font-semibold text-gray-700 mb-2 sm:mb-3">Aplicação *</label>
                <div className="flex flex-col gap-2">
                  {[
                    { value: 'VSLBank', label: 'VSLBank' },
                    { value: 'Portal ValeShop', label: 'Portal ValeShop' },
                    { value: 'Sistema Interno/Forms', label: 'Sistema Interno/Forms' },
                    { value: 'App Benefícios', label: 'App Benefícios' },
                    { value: 'Frotas', label: 'Frotas' }
                  ].map(app => (
                    <label key={app.value} className={`flex items-center px-3 py-2.5 rounded-lg border cursor-pointer transition-all duration-150 text-sm sm:text-base font-medium
                      ${formData.application === app.value
                        ? 'bg-blue-600 text-white border-blue-600 shadow-sm'
                        : errors.application
                          ? 'border-red-500 bg-red-50 text-red-700'
                          : 'bg-white border-gray-300 hover:border-blue-400'}
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
                  <p className="mt-2 text-sm text-red-600 font-medium">{errors.application}</p>
                )}
              </div>
              
              {/* Coluna 3: Duração */}
              <div className="lg:col-span-1">
                <label className="block text-sm sm:text-sm font-semibold text-gray-700 mb-2 sm:mb-3">
                Duração (horas) *
                </label>
                <div className="relative">
                  <Clock className="absolute left-3 sm:left-4 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4 sm:w-5 sm:h-5" />
                <input
                  type="number"
                  min="1"
                  value={formData.duration_hours}
                  onChange={(e) => handleInputChange('duration_hours', e.target.value)}
                    className={`${inputClasses('duration_hours')} h-12 sm:h-14 text-base sm:text-lg pl-10 sm:pl-10`}
                  placeholder="Ex: 8"
                />
                </div>
                {errors.duration_hours && (
                  <p className="mt-2 text-sm text-red-600 font-medium">{errors.duration_hours}</p>
                )}
                <p className="mt-2 text-xs text-gray-500">
                  Tempo necessário de acesso
                </p>
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-4 sm:pt-6 border-t border-gray-200">
              <button
                type="submit"
                disabled={status.type === 'loading'}
                className="w-full bg-gradient-to-r from-blue-600 to-indigo-700 hover:from-blue-700 hover:to-indigo-800 disabled:from-blue-400 disabled:to-indigo-500 text-white font-semibold py-3 sm:py-4 px-6 sm:px-8 rounded-xl transition-all duration-300 flex items-center justify-center space-x-2 sm:space-x-3 disabled:cursor-not-allowed transform hover:scale-[1.02] disabled:hover:scale-100 shadow-lg hover:shadow-xl text-base sm:text-lg"
              >
                {status.type === 'loading' ? (
                  <>
                    <div className="w-5 h-5 sm:w-6 sm:h-6 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Enviando Solicitação...</span>
                  </>
                ) : (
                  <>
                    <Send className="w-5 h-5 sm:w-6 sm:h-6" />
                    <span>Enviar Solicitação</span>
                  </>
                )}
              </button>
            </div>
          </form>

            {/* Footer */}
            <div className="mt-6 sm:mt-8 pt-4 sm:pt-6 border-t border-gray-200 text-center">
              <div className="flex flex-col sm:flex-row items-center justify-center space-y-2 sm:space-y-0 sm:space-x-6 text-xs sm:text-sm text-gray-500">
                <div className="flex items-center">
                  <Shield className="w-4 h-4 mr-2" />
                  <span>Análise de segurança</span>
                </div>
                <div className="flex items-center">
                  <Clock className="w-4 h-4 mr-2" />
                  <span>Resposta em até 24h</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        {/* Additional Info */}
        <div className="mt-6 sm:mt-8 text-center">
          <p className="text-xs sm:text-sm text-gray-600">
            Em caso de dúvidas, entre em contato.
          </p>
        </div>
      </div>
    </div>
  );
}

export default App;
