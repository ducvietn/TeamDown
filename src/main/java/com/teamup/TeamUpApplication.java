package com.teamup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TeamUpApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamUpApplication.class, args);
    }
}
