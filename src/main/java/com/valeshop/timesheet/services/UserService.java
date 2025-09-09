package com.valeshop.timesheet.services;

import com.valeshop.timesheet.entities.user.User;
import com.valeshop.timesheet.exceptions.ResourceNotFoundException;
import com.valeshop.timesheet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User findById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException(userId));
    }
}