package com.valeshop.timesheet.controllers;

import com.valeshop.timesheet.entities.user.User;
import com.valeshop.timesheet.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/users")
public class UserController {

    @Autowired
    private UserService userService;


    @GetMapping(value = "/{userId}")
    public ResponseEntity<User> findById(@PathVariable Long userId) {
        User obj = userService.findById(userId);
        return ResponseEntity.ok().body(obj);
    }

}
