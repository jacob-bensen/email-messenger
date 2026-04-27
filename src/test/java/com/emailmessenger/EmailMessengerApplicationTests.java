package com.emailmessenger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class EmailMessengerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void imapPollingJobNotRegisteredWhenPollingDisabled(@Autowired ApplicationContext ctx) {
        assertThat(ctx.containsBean("imapPollingJob")).isFalse();
    }
}
