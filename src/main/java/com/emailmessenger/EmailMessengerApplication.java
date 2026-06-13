package com.emailmessenger;

import com.emailmessenger.admin.AdminProperties;
import com.emailmessenger.auth.GoogleOAuthProperties;
import com.emailmessenger.billing.BillingProperties;
import com.emailmessenger.web.LandingProperties;
import com.emailmessenger.web.LegalProperties;
import com.emailmessenger.web.SiteProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@SpringBootApplication
@EnableConfigurationProperties({BillingProperties.class, SiteProperties.class, LegalProperties.class,
        LandingProperties.class, AdminProperties.class, GoogleOAuthProperties.class})
@EnableScheduling
public class EmailMessengerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailMessengerApplication.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
