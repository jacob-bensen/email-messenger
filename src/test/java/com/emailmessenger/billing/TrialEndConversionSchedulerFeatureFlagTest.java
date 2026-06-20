package com.emailmessenger.billing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@TestPropertySource(properties = "trial-end.enabled=false")
class TrialEndConversionSchedulerFeatureFlagTest {

    @Autowired ApplicationContext ctx;

    @Test
    void schedulerBeanIsAbsentWhenFlagIsOff() {
        assertThatThrownBy(() -> ctx.getBean(TrialEndConversionScheduler.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void trialEndConversionServiceBeanIsAlwaysPresent() {
        assertThat(ctx.getBean(TrialEndConversionService.class)).isNotNull();
    }
}
