package com.emailmessenger.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "mailbox.polling.enabled=false")
class MailboxPollingFeatureFlagTest {

    @Autowired ApplicationContext ctx;

    @Test
    void serviceBeanIsAbsentWhenFlagIsOff() {
        assertThatThrownBy(() -> ctx.getBean(MailboxPollingService.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }
}
