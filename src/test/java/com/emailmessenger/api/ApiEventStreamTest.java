package com.emailmessenger.api;

import com.emailmessenger.domain.User;
import com.emailmessenger.email.NewMailEvent;
import com.emailmessenger.repository.UserRepository;
import com.emailmessenger.service.ConversationListItem;
import com.emailmessenger.web.ConversationListService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiEventStreamTest {

    private final UserRepository users = mock(UserRepository.class);
    private final ConversationListService conversations = mock(ConversationListService.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build(); // as Spring Boot configures it
    private final List<SseEmitter> created = new ArrayList<>();
    private final User ada = mock(User.class);

    // Spied emitters: SseEmitter buffers sends until a real async request
    // attaches, so the spy is how the test sees them.
    private final ApiEventStream stream = new ApiEventStream(users, conversations, objectMapper,
            mock(PlatformTransactionManager.class)) {
        @Override
        protected SseEmitter createEmitter() {
            SseEmitter spy = Mockito.spy(new SseEmitter(1000L));
            created.add(spy);
            return spy;
        }
    };

    @Test
    void subscribingSendsReady() throws IOException {
        stream.subscribe(1L);

        assertThat(sentData(created.get(0))).containsExactly("event:ready\ndata:{}");
        assertThat(stream.activeCount(1L)).isEqualTo(1);
    }

    @Test
    void newMailBecomesChangedRowsAndCountsForEveryStreamOfThatUser() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.of(ada));
        when(conversations.items(ada, Set.of("k1"))).thenReturn(List.of(new ConversationListItem(
                "k1", "Bob", "B", false, 1, "hello", LocalDateTime.of(2026, 11, 20, 8, 0),
                true, 1, List.of("ada@example.com"), "bob@acme.com")));
        when(conversations.counts(eq(ada), isNull()))
                .thenReturn(new ConversationListService.ChatCounts(4, 2, 1, 0));
        stream.subscribe(1L);
        stream.subscribe(1L);
        stream.subscribe(2L);

        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));

        for (SseEmitter emitter : created.subList(0, 2)) {
            List<String> sent = sentData(emitter);
            assertThat(sent).hasSize(2);
            assertThat(sent.get(1)).startsWith("event:conversations-changed\ndata:");
            JsonNode body = objectMapper.readTree(sent.get(1).substring(sent.get(1).indexOf("data:") + 5));
            assertThat(body.get("imported").asInt()).isEqualTo(1);
            assertThat(body.get("conversations").get(0).get("key").asText()).isEqualTo("k1");
            assertThat(body.get("conversations").get(0).get("lastActivityAt").asText())
                    .isEqualTo("2026-11-20T08:00:00Z");
            assertThat(body.get("conversations").get(0).get("accounts").get(0).asText())
                    .isEqualTo("ada@example.com");
            assertThat(body.get("counts").get("unread").asLong()).isEqualTo(2);
        }
        assertThat(sentData(created.get(2))).hasSize(1); // other user: ready only
        verify(conversations, times(1)).items(any(), any()); // built once, not per stream
    }

    @Test
    void noStreamMeansNoWork() {
        stream.onNewMail(new NewMailEvent(1L, 1, Set.of("k1")));

        verify(users, never()).findById(any());
    }

    @Test
    void aFailedBuildStillNotifiesWithNullCountsSoTheClientRefetches() throws Exception {
        when(users.findById(1L)).thenThrow(new IllegalStateException("db down"));

        JsonNode body = objectMapper.readTree(stream.payloadFor(new NewMailEvent(1L, 3, Set.of("k1"))));

        assertThat(body.get("imported").asInt()).isEqualTo(3);
        assertThat(body.get("conversations")).isEmpty();
        assertThat(body.get("counts").isNull()).isTrue();
    }

    @Test
    void deadStreamIsDropped() throws IOException {
        stream.subscribe(1L);
        doThrow(new IOException("broken pipe"))
                .when(created.get(0)).send(any(SseEmitter.SseEventBuilder.class));

        stream.keepalive();

        assertThat(stream.activeCount(1L)).isZero();
    }

    private static List<String> sentData(SseEmitter emitter) throws IOException {
        ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
                ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        verify(emitter, atLeastOnce()).send(captor.capture());
        return captor.getAllValues().stream()
                .map(b -> b.build().stream().map(d -> d.getData().toString()).collect(Collectors.joining()).trim())
                .toList();
    }
}
