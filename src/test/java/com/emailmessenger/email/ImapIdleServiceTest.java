package com.emailmessenger.email;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImapIdleServiceTest {

    private static final ImapCredentials CREDS =
            ImapCredentials.password("imap.example.com", 993, true, "u@example.com", "pw");

    private final ImapIdleSupport support = mock(ImapIdleSupport.class);
    private final MailboxPollingService polling = mock(MailboxPollingService.class);
    private final FakeIdleClient idleClient = new FakeIdleClient();
    private final ImapIdleService service = new ImapIdleService(support, idleClient, polling);

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void reconcileStartsAWorkerThatPollsOnConnectAndOnNewMail() throws Exception {
        when(support.eligibleAccountIds()).thenReturn(Set.of(7L));
        when(support.credentialsFor(7L)).thenReturn(CREDS);

        service.reconcile();

        assertThat(service.activeWorkerCount()).isEqualTo(1);
        // Connect does a catch-up poll immediately...
        verify(polling, timeout(2000)).pollOne(7L);

        // ...and an IDLE wake-up polls again.
        idleClient.awaitConnection().signalNewMail();
        verify(polling, timeout(2000).times(2)).pollOne(7L);
    }

    @Test
    void reconcileStopsWorkersThatAreNoLongerEligible() throws Exception {
        when(support.eligibleAccountIds()).thenReturn(Set.of(7L));
        when(support.credentialsFor(7L)).thenReturn(CREDS);
        service.reconcile();
        FakeConnection connection = idleClient.awaitConnection();

        when(support.eligibleAccountIds()).thenReturn(Set.of());
        service.reconcile();

        assertThat(service.activeWorkerCount()).isZero();
        assertThat(connection.awaitClosed()).isTrue();
    }

    @Test
    void ineligibleAccountsNeverGetAConnection() {
        when(support.eligibleAccountIds()).thenReturn(Set.of());

        service.reconcile();

        assertThat(service.activeWorkerCount()).isZero();
        assertThat(idleClient.connections).isEmpty();
    }

    @Test
    void quietIdleWindowReconnectsWithFreshCredentials() throws Exception {
        when(support.eligibleAccountIds()).thenReturn(Set.of(7L));
        when(support.credentialsFor(7L)).thenReturn(CREDS);

        service.reconcile();
        idleClient.awaitConnection().signalQuietWindow();

        // A second connection means the worker looped: fresh credentialsFor +
        // fresh open, with no backoff in between.
        assertThat(idleClient.awaitConnection()).isNotNull();
        verify(support, timeout(2000).times(2)).credentialsFor(7L);
    }

    /** Hand-rolled fake: connections block until the test signals them. */
    private static final class FakeIdleClient implements ImapIdleClient {
        final List<FakeConnection> connections = new CopyOnWriteArrayList<>();
        private final BlockingQueue<FakeConnection> opened = new LinkedBlockingQueue<>();

        @Override
        public IdleConnection open(ImapCredentials credentials) {
            FakeConnection connection = new FakeConnection();
            connections.add(connection);
            opened.add(connection);
            return connection;
        }

        FakeConnection awaitConnection() throws InterruptedException {
            FakeConnection connection = opened.poll(2, TimeUnit.SECONDS);
            assertThat(connection).as("expected an IDLE connection to open").isNotNull();
            return connection;
        }
    }

    private static final class FakeConnection implements ImapIdleClient.IdleConnection {
        private final BlockingQueue<Boolean> signals = new LinkedBlockingQueue<>();
        private final CountDownLatch closed = new CountDownLatch(1);

        void signalNewMail() { signals.add(true); }
        void signalQuietWindow() { signals.add(false); }

        @Override
        public boolean awaitNewMail() {
            try {
                return signals.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ImapConnectionException("interrupted", e);
            }
        }

        @Override
        public void close() {
            closed.countDown();
            signals.add(false); // unblock a waiting awaitNewMail
        }

        boolean awaitClosed() throws InterruptedException {
            return closed.await(2, TimeUnit.SECONDS);
        }
    }
}
