package com.valeshop.timesheet.config;

import com.valeshop.timesheet.entities.user.User;
import com.valeshop.timesheet.entities.user.UserType;
import com.valeshop.timesheet.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;

@Configuration
@Profile("test")
public class TestConfig implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        User admin = new User(null,"hazevedo@valeshop.com.br", "123456", UserType.Administrador);
        User normal = new User(null,"smota@valeshop.com.br", "123456", UserType.Normal);

        userRepository.saveAll(Arrays.asList(admin,normal));
    }
}
