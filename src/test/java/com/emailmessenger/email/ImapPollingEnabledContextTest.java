package com.emailmessenger.email;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.imap.polling.enabled=true")
class ImapPollingEnabledContextTest {

    @Test
    void imapPollingJobBeanRegisteredWhenEnabled(@Autowired ApplicationContext ctx) {
        assertThat(ctx.containsBean("imapPollingJob")).isTrue();
    }

    @Test
    void imapPollingPropertiesDefaultsAreCorrect(@Autowired ImapPollingProperties props) {
        assertThat(props.getPort()).isEqualTo(993);
        assertThat(props.isSsl()).isTrue();
        assertThat(props.getFolder()).isEqualTo("INBOX");
        assertThat(props.getPolling().isEnabled()).isTrue();
        assertThat(props.getPolling().getIntervalMs()).isEqualTo(60000L);
    }
}
