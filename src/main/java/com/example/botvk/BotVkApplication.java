package com.example.botvk;

import com.example.botvk.service.LongPollService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BotVkZApplication implements CommandLineRunner {

    private final LongPollService longPollService;

    public BotVkZApplication(LongPollService longPollService) {
        this.longPollService = longPollService;
    }

    public static void main(String[] args) {
        SpringApplication.run(BotVkZApplication.class, args);
    }

    @Override
    public void run(String[] args) {
        longPollService.start();
    }
}