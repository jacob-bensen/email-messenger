package com.emailmessenger.reengagement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "reengagement.enabled=false")
class ReengagementSchedulerFeatureFlagTest {

    @Autowired ApplicationContext ctx;

    @Test
    void schedulerBeanIsAbsentWhenFlagIsOff() {
        assertThatThrownBy(() -> ctx.getBean(ReengagementScheduler.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void reengagementServiceBeanIsAlwaysPresent() {
        // The service is always wired so admin tools / tests can invoke it
        // even when the recurring schedule is off.
        assertThat(ctx.getBean(ReengagementService.class)).isNotNull();
    }
}
