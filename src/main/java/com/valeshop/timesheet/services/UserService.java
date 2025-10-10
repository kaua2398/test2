package com.valeshop.timesheet.services;

import com.valeshop.timesheet.entities.user.*;
import com.valeshop.timesheet.exceptions.UserAlreadyExistsException;
import com.valeshop.timesheet.exceptions.UserNotFoundException;
import com.valeshop.timesheet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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
            return false;
        }

        user.setEnabled(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return true;
    }

    public void requestPasswordReset(UserForgotPasswordDTO dataUser) {
        User user = userRepository.findByEmail(dataUser.email())
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o e-mail: " + dataUser.email()));

        user.setPasswordResetToken(UUID.randomUUID().toString());
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(20));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getPasswordResetToken());
    }

    public boolean resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElse(null);

        if (user == null || user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        return true;
    }

    public UserResponseDTO getAuthenticatedUserProfile() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado no contexto de segurança."));
        return new UserResponseDTO(user);
    }
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado com o e-mail: " + email));

        if (user.isEnabled()) {
            throw new IllegalStateException();
        }

        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiry(LocalDateTime.now().plusMinutes(20));
        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
    }
}

