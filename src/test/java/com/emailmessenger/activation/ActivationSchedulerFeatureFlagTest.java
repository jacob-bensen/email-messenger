package com.emailmessenger.activation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "activation.enabled=false")
class ActivationSchedulerFeatureFlagTest {

    @Autowired ApplicationContext ctx;

    @Test
    void schedulerBeanIsAbsentWhenFlagIsOff() {
        assertThatThrownBy(() -> ctx.getBean(ActivationScheduler.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void activationServiceBeanIsAlwaysPresent() {
        assertThat(ctx.getBean(ActivationService.class)).isNotNull();
    }
}
