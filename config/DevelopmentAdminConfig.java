package com.hospitalarchive.hospitalarchivebackend.config;

import com.hospitalarchive.hospitalarchivebackend.model.User;
import com.hospitalarchive.hospitalarchivebackend.service.UserService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevelopmentAdminConfig {

  
    @Bean
    public CommandLineRunner createDevelopmentAdmin(UserService userService) {

        return args -> {
            String username = System.getenv("HOSPITAL_ADMIN_USERNAME");
            String password = System.getenv("HOSPITAL_ADMIN_PASSWORD");

            if (username == null || username.isBlank()
                    || password == null || password.isBlank()) {
                throw new IllegalStateException(
                        "Set HOSPITAL_ADMIN_USERNAME and "
                                + "HOSPITAL_ADMIN_PASSWORD for the dev profile."
                );
            }

            if (userService.getUserByUsername(username).isPresent()) {
                throw new IllegalStateException(
                        "Development administrator username already exists."
                );
            }

            User admin = new User();
            admin.setFullName("Development Administrator");
            admin.setUsername(username);
            admin.setPassword(password);
            admin.setRole("ADMIN");

            userService.saveUser(admin);
        };
    }
}
