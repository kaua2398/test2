package com.valeshop.timesheet.services;

import com.valeshop.timesheet.entities.user.User;
import com.valeshop.timesheet.entities.user.UserRegisterDTO;
import com.valeshop.timesheet.entities.user.UserType;
import com.valeshop.timesheet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User registerUser(UserRegisterDTO dataUser) {
        User newUser;
        String userType = dataUser.userType();

        if ("Administrador".equalsIgnoreCase(userType)) {
            newUser = new User(null, dataUser.email(), dataUser.password(), UserType.Administrador);

        } else if ("Normal".equalsIgnoreCase(userType)) {
            newUser = new User(null, dataUser.email(), dataUser.password(), UserType.Normal);

        } else {
            throw new IllegalArgumentException("Tipo de usuário inválido: " + userType);
        }
        userRepository.save(newUser);
        return newUser;
    }
}