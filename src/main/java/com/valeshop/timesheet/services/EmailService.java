package com.valeshop.timesheet.services;

import com.valeshop.timesheet.exceptions.CannotSendEmailCorrectlyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Async // Para não bloquear a thread principal durante o envio de e-mail
    public void sendVerificationEmail(String to, String token) {
        String subject = "Validação de E-mail - Timesheet API";
        // ATENÇÃO: Num projeto real, a URL base deve vir de um ficheiro de configuração.
        String verificationUrl = "http://localhost:8080/users/verify-email?token=" + token;
        String text = "Bem-vindo à Timesheet API! Por favor, clique no link abaixo para validar o seu e-mail:\n" + verificationUrl;

        sendEmail(to, subject, text);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Redefinição de Senha - Timesheet API";
        // ATENÇÃO: Esta URL deve apontar para a sua PÁGINA DE FRONTEND, não para a API.
        // O frontend irá receber o token e depois fazer a chamada à API.
        String resetUrl = "http://localhost:8080/users/reset-password?token=" + token;
        String text = "Você solicitou uma redefinição de senha. Clique no link abaixo para criar uma nova senha:\n" + resetUrl;

        sendEmail(to, subject, text);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("no-reply@timesheetapi.com"); // Pode ser o seu e-mail de envio

            emailSender.send(message);
        } catch (Exception e) {
            System.err.println("Erro ao enviar e-mail para " + to + ": " + e.getMessage());
            throw new CannotSendEmailCorrectlyException();
        }
    }
}
