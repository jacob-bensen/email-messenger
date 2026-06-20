package com.emailmessenger.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "mailbox.polling.enabled=false")
class MailboxPollingFeatureFlagTest {

    @Autowired ApplicationContext ctx;

    @Test
    void schedulerBeanIsAbsentWhenFlagIsOff() {
        assertThatThrownBy(() -> ctx.getBean(MailboxPollingScheduler.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void pollingServiceBeanIsPresentEvenWhenFlagIsOff() {
        // Manual "Sync now" needs to invoke pollOne regardless of the flag.
        assertThat(ctx.getBean(MailboxPollingService.class)).isNotNull();
    }
}
