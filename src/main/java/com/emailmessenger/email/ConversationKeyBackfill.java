package com.emailmessenger.email;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.ConversationKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backfills {@code conversation_key} for threads that predate the column (added
 * in V32). Runs once at startup, in-app rather than in SQL so it reuses
 * {@link ConversationKeyService}'s hashing. Idempotent: only null-keyed threads
 * are touched, so a second boot is a no-op.
 */
@Component
class ConversationKeyBackfill {

    private static final Logger log = LoggerFactory.getLogger(ConversationKeyBackfill.class);

    private final EmailThreadRepository threads;
    private final ConversationKeyService conversationKeyService;
    private final OwnerAddressService ownerAddressService;

    ConversationKeyBackfill(EmailThreadRepository threads,
                            ConversationKeyService conversationKeyService,
                            OwnerAddressService ownerAddressService) {
        this.threads = threads;
        this.conversationKeyService = conversationKeyService;
        this.ownerAddressService = ownerAddressService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfill() {
        List<EmailThread> pending = threads.findByConversationKeyIsNull();
        if (pending.isEmpty()) {
            return;
        }
        for (EmailThread thread : pending) {
            thread.setConversationKey(conversationKeyService.compute(
                    thread, ownerAddressService.addressesFor(thread.getOwner())));
        }
        threads.saveAll(pending);
        log.info("Backfilled conversation_key for {} thread(s)", pending.size());
    }
}
