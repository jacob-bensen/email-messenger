package com.emailmessenger;

import com.emailmessenger.billing.BillingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@EnableConfigurationProperties(BillingProperties.class)
public class EmailMessengerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailMessengerApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
