package com.valeshop.timesheet.services;

import com.valeshop.timesheet.entities.user.User;
import com.valeshop.timesheet.entities.user.UserForgotPasswordDTO;
import com.valeshop.timesheet.entities.user.UserRegisterDTO;
import com.valeshop.timesheet.entities.user.UserType;
import com.valeshop.timesheet.exceptions.UserAlreadyExistsException;
import com.valeshop.timesheet.exceptions.UserNotFoundException;
import com.valeshop.timesheet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    public User registerUser(UserRegisterDTO dataUser) {
        if (this.userRepository.findByEmail(dataUser.email()).isPresent()) {
            throw new UserAlreadyExistsException("O email fornecido já está em uso.");
        }
        String encryptedPassword = passwordEncoder.encode(dataUser.password());

        User newUser;
        String userType = dataUser.userType();

        if ("Administrador".equalsIgnoreCase(userType)) {
            newUser = new User(null, dataUser.email(), encryptedPassword, UserType.Administrador);
        } else {
            newUser = new User(null, dataUser.email(), encryptedPassword, UserType.Normal);
        }

        // Gera e define o token de verificação
        String token = UUID.randomUUID().toString();
        newUser.setVerificationToken(token);

        userRepository.save(newUser);

        emailService.sendVerificationEmail(newUser.getEmail(), token);

        return newUser;
    }

    public boolean verifyUser(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElse(null);

        if (user == null || user.isEnabled()) {
            return false; // Utilizador não encontrado ou já verificado
        }

        user.setEnabled(true);
        user.setVerificationToken(null); // Limpa o token após a verificação
        userRepository.save(user);

        return true;
    }

    public void requestPasswordReset(UserForgotPasswordDTO dataUser) {
        User user = userRepository.findByEmail(dataUser.email())
                .orElseThrow(() -> new UserNotFoundException("Utilizador não encontrado com o e-mail: " + dataUser.email()));

        user.setPasswordResetToken(UUID.randomUUID().toString());
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getPasswordResetToken());
    }

    public boolean resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElse(null);

        if (user == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return false; // Token inválido ou expirado
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        return true;
    }
}

