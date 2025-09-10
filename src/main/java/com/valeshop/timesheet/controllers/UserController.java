package com.valeshop.timesheet.controllers;

import com.valeshop.timesheet.entities.user.*;
import com.valeshop.timesheet.exceptions.InvalidPasswordException;
import com.valeshop.timesheet.infra.security.TokenService;
import com.valeshop.timesheet.schemas.UserSchema;
import com.valeshop.timesheet.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/users")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;


    @PostMapping
    public ResponseEntity<User> registerUser(@Valid @RequestBody UserSchema userSchema) {
        UserRegisterDTO userRegisterDTO = new UserRegisterDTO(userSchema.getEmail(), userSchema.getPassword(), userSchema.getUserType());
        User savedUser = userService.registerUser(userRegisterDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    @PostMapping(value = "/login")
    public ResponseEntity<?> login(@RequestBody AutenticationDTO data) {
        try{
            UsernamePasswordAuthenticationToken usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.password());
            Authentication auth = this.authenticationManager.authenticate(usernamePassword);
            User users = (User) auth.getPrincipal();
            String token = tokenService.generateToken(users);
            UserResponseDTO userResponse = new UserResponseDTO(users);


            return ResponseEntity.ok(new UserLoginDTO(token, userResponse));
        } catch (BadCredentialsException e){
            throw new InvalidPasswordException();
        }
    }
}
