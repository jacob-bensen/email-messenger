package com.emailmessenger;

import com.emailmessenger.email.ImapPollingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ImapPollingProperties.class)
public class EmailMessengerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailMessengerApplication.class, args);
    }
}
