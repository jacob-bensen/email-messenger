package com.emailmessenger.repository;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Message;
import com.emailmessenger.domain.RecipientType;
import com.emailmessenger.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmailThreadRepository extends JpaRepository<EmailThread, Long> {

    Optional<EmailThread> findByRootMessageIdAndOwner(String rootMessageId, User owner);

    Page<EmailThread> findByOwnerOrderByUpdatedAtDesc(User owner, Pageable pageable);

    Optional<EmailThread> findByIdAndOwner(Long id, User owner);

    // Threads whose conversation key hasn't been computed yet — drives the
    // one-time in-app backfill after the V32 migration adds the column.
    List<EmailThread> findByConversationKeyIsNull();

    long countByOwner(User owner);

    long countByOwnerAndUnreadTrue(User owner);

    /**
     * Onboarding-funnel slice: which users in the supplied cohort have at
     * least {@code threshold} threads. Returns the matching owner IDs so
     * the caller can take {@code .size()} without paying for a derived-
     * table {@code COUNT}-over-{@code GROUP BY HAVING} (which JPQL doesn't
     * portably support). Empty cohort returns an empty list.
     */
    @Query("""
            SELECT t.owner.id FROM EmailThread t
            WHERE t.owner.id IN :userIds
            GROUP BY t.owner.id
            HAVING COUNT(t) >= :threshold
            """)
    List<Long> findOwnerIdsWithAtLeastThreadsAmong(@Param("userIds") Collection<Long> userIds,
                                                   @Param("threshold") long threshold);

    // Filtered no-search listing — powers `/threads` when no `?q=` / `?from=` is set
    // but one or more of the filter chips (since / unread / attachments) is on.
    @Query("""
            SELECT DISTINCT t FROM EmailThread t
            WHERE t.owner = :owner
              AND (CAST(:since AS timestamp) IS NULL OR t.updatedAt >= :since)
              AND (:requireUnread = false OR t.unread = true)
              AND (:requireAttachments = false OR EXISTS (
                SELECT 1 FROM Message m JOIN m.attachments a WHERE m.thread = t
              ))
            ORDER BY t.updatedAt DESC
            """)
    Page<EmailThread> findByOwnerFiltered(@Param("owner") User owner,
                                          @Param("since") LocalDateTime since,
                                          @Param("requireUnread") boolean requireUnread,
                                          @Param("requireAttachments") boolean requireAttachments,
                                          Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM EmailThread t
            WHERE t.owner = :owner
              AND (CAST(:senderEmail AS string) IS NULL OR EXISTS (
                SELECT 1 FROM Message ss
                WHERE ss.thread = t AND LOWER(ss.sender.email) = LOWER(CAST(:senderEmail AS string))
              ))
              AND (CAST(:since AS timestamp) IS NULL OR t.updatedAt >= :since)
              AND (:requireUnread = false OR t.unread = true)
              AND (:requireAttachments = false OR EXISTS (
                SELECT 1 FROM Message ma JOIN ma.attachments att WHERE ma.thread = t
              ))
              AND (
                LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                OR EXISTS (
                  SELECT 1 FROM Message m
                  WHERE m.thread = t
                    AND (
                      LOWER(m.sender.email) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(m.sender.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    )
                )
              )
            ORDER BY t.updatedAt DESC
            """)
    Page<EmailThread> search(@Param("owner") User owner,
                             @Param("q") String query,
                             @Param("senderEmail") String senderEmail,
                             @Param("since") LocalDateTime since,
                             @Param("requireUnread") boolean requireUnread,
                             @Param("requireAttachments") boolean requireAttachments,
                             Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM EmailThread t
            WHERE t.owner = :owner
              AND (CAST(:senderEmail AS string) IS NULL OR EXISTS (
                SELECT 1 FROM Message ss
                WHERE ss.thread = t AND LOWER(ss.sender.email) = LOWER(CAST(:senderEmail AS string))
              ))
              AND (CAST(:since AS timestamp) IS NULL OR t.updatedAt >= :since)
              AND (:requireUnread = false OR t.unread = true)
              AND (:requireAttachments = false OR EXISTS (
                SELECT 1 FROM Message ma JOIN ma.attachments att WHERE ma.thread = t
              ))
              AND (
                LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                OR EXISTS (
                  SELECT 1 FROM Message m
                  WHERE m.thread = t
                    AND (
                      LOWER(m.sender.email) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(m.sender.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(m.bodyPlain, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    )
                )
              )
            ORDER BY t.updatedAt DESC
            """)
    Page<EmailThread> searchIncludingBody(@Param("owner") User owner,
                                          @Param("q") String query,
                                          @Param("senderEmail") String senderEmail,
                                          @Param("since") LocalDateTime since,
                                          @Param("requireUnread") boolean requireUnread,
                                          @Param("requireAttachments") boolean requireAttachments,
                                          Pageable pageable);

    // Did the query match any thread by body content that the subject/participant
    // search did NOT already surface? Drives the Free-tier upgrade nag.
    @Query("""
            SELECT (COUNT(t) > 0) FROM EmailThread t
            WHERE t.owner = :owner
              AND (CAST(:senderEmail AS string) IS NULL OR EXISTS (
                SELECT 1 FROM Message ss
                WHERE ss.thread = t AND LOWER(ss.sender.email) = LOWER(CAST(:senderEmail AS string))
              ))
              AND (CAST(:since AS timestamp) IS NULL OR t.updatedAt >= :since)
              AND (:requireUnread = false OR t.unread = true)
              AND (:requireAttachments = false OR EXISTS (
                SELECT 1 FROM Message ma JOIN ma.attachments att WHERE ma.thread = t
              ))
              AND LOWER(t.subject) NOT LIKE LOWER(CONCAT('%', :q, '%'))
              AND NOT EXISTS (
                SELECT 1 FROM Message ms
                WHERE ms.thread = t
                  AND (
                    LOWER(ms.sender.email) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(ms.sender.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                  )
              )
              AND EXISTS (
                SELECT 1 FROM Message mb
                WHERE mb.thread = t
                  AND LOWER(COALESCE(mb.bodyPlain, '')) LIKE LOWER(CONCAT('%', :q, '%'))
              )
            """)
    boolean hasBodyOnlyMatch(@Param("owner") User owner,
                             @Param("q") String query,
                             @Param("senderEmail") String senderEmail,
                             @Param("since") LocalDateTime since,
                             @Param("requireUnread") boolean requireUnread,
                             @Param("requireAttachments") boolean requireAttachments);

    @Query("""
            SELECT DISTINCT t FROM EmailThread t
            WHERE t.owner = :owner
              AND EXISTS (
                SELECT 1 FROM Message m
                WHERE m.thread = t AND LOWER(m.sender.email) = LOWER(CAST(:senderEmail AS string))
              )
              AND (CAST(:since AS timestamp) IS NULL OR t.updatedAt >= :since)
              AND (:requireUnread = false OR t.unread = true)
              AND (:requireAttachments = false OR EXISTS (
                SELECT 1 FROM Message ma JOIN ma.attachments att WHERE ma.thread = t
              ))
            ORDER BY t.updatedAt DESC
            """)
    Page<EmailThread> findByOwnerAndSender(@Param("owner") User owner,
                                           @Param("senderEmail") String senderEmail,
                                           @Param("since") LocalDateTime since,
                                           @Param("requireUnread") boolean requireUnread,
                                           @Param("requireAttachments") boolean requireAttachments,
                                           Pageable pageable);

    // The full back-and-forth with one address, oldest first: every message in a
    // thread that the address took part in, where the message is either from that
    // address (received) or one of the owner's own outbound replies. Powers the
    // per-sender chat so both sides show in chronological order.
    @Query("""
            SELECT m FROM Message m
            WHERE m.thread.owner = :owner
              AND EXISTS (
                SELECT 1 FROM Message x
                WHERE x.thread = m.thread AND LOWER(x.sender.email) = LOWER(:senderEmail)
              )
              AND (LOWER(m.sender.email) = LOWER(:senderEmail) OR m.outbound = true)
            ORDER BY m.sentAt ASC
            """)
    List<Message> findConversationWithSender(@Param("owner") User owner,
                                             @Param("senderEmail") String senderEmail);

    // ── Conversation (participant-set) grouping — powers the texting-style chats ──

    // One row per conversation: the threads sharing a conversationKey collapsed,
    // ordered by the most recent message's actual timestamp (sentAt), newest
    // first — texting-style. We deliberately order by MAX(msg.sentAt), NOT
    // t.updatedAt, so marking a chat read (or any other row touch) never
    // reshuffles the list and a freshly-imported old email sorts by when it was
    // sent, not when it was pulled in. The join fans threads out per message, so
    // thread/unread counts use COUNT(DISTINCT t.id) to stay per-thread.
    @Query(value = """
            SELECT t.conversationKey AS conversationKey,
                   MAX(msg.sentAt) AS lastActivity,
                   COUNT(DISTINCT t.id) AS threadCount,
                   COUNT(DISTINCT CASE WHEN t.unread = true THEN t.id END) AS unreadCount
            FROM EmailThread t JOIN t.messages msg
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
            GROUP BY t.conversationKey
            ORDER BY MAX(msg.sentAt) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT t.conversationKey) FROM EmailThread t
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
            """)
    Page<ConversationSummaryRow> conversationSummaries(@Param("owner") User owner, Pageable pageable);

    interface ConversationSummaryRow {
        String getConversationKey();
        LocalDateTime getLastActivity();
        long getThreadCount();
        long getUnreadCount();
    }

    // Per-mailbox variant of conversationSummaries: only conversations one of
    // whose threads involves the given owner-side address (:mailbox, already
    // lowercased) — as a message sender or a non-deleted recipient. This keeps
    // each connected account's chats separate; they are never merged into one
    // unified list. See ConversationListService#list(owner, mailbox, …).
    @Query(value = """
            SELECT t.conversationKey AS conversationKey,
                   MAX(msg.sentAt) AS lastActivity,
                   COUNT(DISTINCT t.id) AS threadCount,
                   COUNT(DISTINCT CASE WHEN t.unread = true THEN t.id END) AS unreadCount
            FROM EmailThread t JOIN t.messages msg
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
              AND (
                EXISTS (SELECT 1 FROM Message sm WHERE sm.thread = t
                        AND LOWER(sm.sender.email) = :mailbox)
                OR EXISTS (SELECT 1 FROM MessageRecipient rr JOIN rr.message rm JOIN rr.participant rp
                           WHERE rm.thread = t AND LOWER(rp.email) = :mailbox)
              )
            GROUP BY t.conversationKey
            ORDER BY MAX(msg.sentAt) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT t.conversationKey) FROM EmailThread t
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
              AND (
                EXISTS (SELECT 1 FROM Message sm WHERE sm.thread = t
                        AND LOWER(sm.sender.email) = :mailbox)
                OR EXISTS (SELECT 1 FROM MessageRecipient rr JOIN rr.message rm JOIN rr.participant rp
                           WHERE rm.thread = t AND LOWER(rp.email) = :mailbox)
              )
            """)
    Page<ConversationSummaryRow> conversationSummariesForMailbox(@Param("owner") User owner,
                                                                @Param("mailbox") String mailbox,
                                                                Pageable pageable);

    // Conversation list filtered by a free-text query: matches the subject, any
    // message's sender/body, or any participant's name/email. Grouped by key so
    // a match in any thread surfaces the whole conversation.
    @Query(value = """
            SELECT t.conversationKey AS conversationKey,
                   MAX(msg.sentAt) AS lastActivity,
                   COUNT(DISTINCT t.id) AS threadCount,
                   COUNT(DISTINCT CASE WHEN t.unread = true THEN t.id END) AS unreadCount
            FROM EmailThread t JOIN t.messages msg
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
              AND (
                LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                OR EXISTS (SELECT 1 FROM Message m WHERE m.thread = t AND (
                    LOWER(m.sender.email) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(m.sender.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(m.bodyPlain, '')) LIKE LOWER(CONCAT('%', :q, '%'))))
                OR EXISTS (SELECT 1 FROM MessageRecipient r JOIN r.message rm JOIN r.participant p
                    WHERE rm.thread = t AND (
                        LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))))
              )
            GROUP BY t.conversationKey
            ORDER BY MAX(msg.sentAt) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT t.conversationKey) FROM EmailThread t
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
              AND (
                LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                OR EXISTS (SELECT 1 FROM Message m WHERE m.thread = t AND (
                    LOWER(m.sender.email) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(m.sender.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(m.bodyPlain, '')) LIKE LOWER(CONCAT('%', :q, '%'))))
                OR EXISTS (SELECT 1 FROM MessageRecipient r JOIN r.message rm JOIN r.participant p
                    WHERE rm.thread = t AND (
                        LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))))
              )
            """)
    Page<ConversationSummaryRow> conversationSummariesMatching(@Param("owner") User owner,
                                                              @Param("q") String q, Pageable pageable);

    // Per-mailbox variant of conversationSummariesMatching: the same free-text
    // search, scoped to conversations involving :mailbox (already lowercased).
    @Query(value = """
            SELECT t.conversationKey AS conversationKey,
                   MAX(msg.sentAt) AS lastActivity,
                   COUNT(DISTINCT t.id) AS threadCount,
                   COUNT(DISTINCT CASE WHEN t.unread = true THEN t.id END) AS unreadCount
            FROM EmailThread t JOIN t.messages msg
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
              AND (
                EXISTS (SELECT 1 FROM Message sm WHERE sm.thread = t
                        AND LOWER(sm.sender.email) = :mailbox)
                OR EXISTS (SELECT 1 FROM MessageRecipient rr JOIN rr.message rm JOIN rr.participant rp
                           WHERE rm.thread = t AND LOWER(rp.email) = :mailbox)
              )
              AND (
                LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                OR EXISTS (SELECT 1 FROM Message m WHERE m.thread = t AND (
                    LOWER(m.sender.email) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(m.sender.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(m.bodyPlain, '')) LIKE LOWER(CONCAT('%', :q, '%'))))
                OR EXISTS (SELECT 1 FROM MessageRecipient r JOIN r.message rm JOIN r.participant p
                    WHERE rm.thread = t AND (
                        LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))))
              )
            GROUP BY t.conversationKey
            ORDER BY MAX(msg.sentAt) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT t.conversationKey) FROM EmailThread t
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
              AND (
                EXISTS (SELECT 1 FROM Message sm WHERE sm.thread = t
                        AND LOWER(sm.sender.email) = :mailbox)
                OR EXISTS (SELECT 1 FROM MessageRecipient rr JOIN rr.message rm JOIN rr.participant rp
                           WHERE rm.thread = t AND LOWER(rp.email) = :mailbox)
              )
              AND (
                LOWER(t.subject) LIKE LOWER(CONCAT('%', :q, '%'))
                OR EXISTS (SELECT 1 FROM Message m WHERE m.thread = t AND (
                    LOWER(m.sender.email) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(m.sender.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(COALESCE(m.bodyPlain, '')) LIKE LOWER(CONCAT('%', :q, '%'))))
                OR EXISTS (SELECT 1 FROM MessageRecipient r JOIN r.message rm JOIN r.participant p
                    WHERE rm.thread = t AND (
                        LOWER(p.email) LIKE LOWER(CONCAT('%', :q, '%'))
                        OR LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', :q, '%'))))
              )
            """)
    Page<ConversationSummaryRow> conversationSummariesMatchingForMailbox(@Param("owner") User owner,
                                                                        @Param("mailbox") String mailbox,
                                                                        @Param("q") String q,
                                                                        Pageable pageable);

    // Flexible conversation summary powering the Dashboard/Hub: optional account
    // scope (:mailbox null = all connected accounts merged into one feed),
    // optional free-text (:q null = no search), and optional triage filters
    // evaluated at the conversation level — unread (any thread unread),
    // attachments (any message has one), awaiting (the conversation's latest
    // message is inbound, i.e. they replied last and you haven't).
    @Query(value = """
            SELECT t.conversationKey AS conversationKey,
                   MAX(msg.sentAt) AS lastActivity,
                   COUNT(DISTINCT t.id) AS threadCount,
                   COUNT(DISTINCT CASE WHEN t.unread = true THEN t.id END) AS unreadCount
            FROM EmailThread t JOIN t.messages msg
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
              AND (CAST(:mailbox AS string) IS NULL OR (
                    EXISTS (SELECT 1 FROM Message sm WHERE sm.thread = t
                            AND LOWER(sm.sender.email) = CAST(:mailbox AS string))
                    OR EXISTS (SELECT 1 FROM MessageRecipient rr JOIN rr.message rm JOIN rr.participant rp
                               WHERE rm.thread = t AND LOWER(rp.email) = CAST(:mailbox AS string))))
              AND (:requireUnread = false OR EXISTS (
                    SELECT 1 FROM EmailThread ut WHERE ut.owner = :owner
                      AND ut.conversationKey = t.conversationKey AND ut.unread = true))
              AND (:requireAttachments = false OR EXISTS (
                    SELECT 1 FROM Message am JOIN am.attachments aa JOIN am.thread at
                    WHERE at.owner = :owner AND at.conversationKey = t.conversationKey))
              AND (:requireAwaiting = false OR EXISTS (
                    SELECT 1 FROM Message lm JOIN lm.thread lt
                    WHERE lt.owner = :owner AND lt.conversationKey = t.conversationKey
                      AND lm.outbound = false
                      AND lm.sentAt = (SELECT MAX(m3.sentAt) FROM Message m3 JOIN m3.thread t3
                                       WHERE t3.owner = :owner AND t3.conversationKey = t.conversationKey)))
              AND (CAST(:q AS string) IS NULL OR (
                    LOWER(t.subject) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                    OR EXISTS (SELECT 1 FROM Message m WHERE m.thread = t AND (
                        LOWER(m.sender.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                        OR LOWER(COALESCE(m.sender.displayName, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                        OR LOWER(COALESCE(m.bodyPlain, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))))
                    OR EXISTS (SELECT 1 FROM MessageRecipient r JOIN r.message rm2 JOIN r.participant p
                        WHERE rm2.thread = t AND (
                            LOWER(p.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                            OR LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))))))
            GROUP BY t.conversationKey
            ORDER BY MAX(msg.sentAt) DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT t.conversationKey) FROM EmailThread t
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
              AND (CAST(:mailbox AS string) IS NULL OR (
                    EXISTS (SELECT 1 FROM Message sm WHERE sm.thread = t
                            AND LOWER(sm.sender.email) = CAST(:mailbox AS string))
                    OR EXISTS (SELECT 1 FROM MessageRecipient rr JOIN rr.message rm JOIN rr.participant rp
                               WHERE rm.thread = t AND LOWER(rp.email) = CAST(:mailbox AS string))))
              AND (:requireUnread = false OR EXISTS (
                    SELECT 1 FROM EmailThread ut WHERE ut.owner = :owner
                      AND ut.conversationKey = t.conversationKey AND ut.unread = true))
              AND (:requireAttachments = false OR EXISTS (
                    SELECT 1 FROM Message am JOIN am.attachments aa JOIN am.thread at
                    WHERE at.owner = :owner AND at.conversationKey = t.conversationKey))
              AND (:requireAwaiting = false OR EXISTS (
                    SELECT 1 FROM Message lm JOIN lm.thread lt
                    WHERE lt.owner = :owner AND lt.conversationKey = t.conversationKey
                      AND lm.outbound = false
                      AND lm.sentAt = (SELECT MAX(m3.sentAt) FROM Message m3 JOIN m3.thread t3
                                       WHERE t3.owner = :owner AND t3.conversationKey = t.conversationKey)))
              AND (CAST(:q AS string) IS NULL OR (
                    LOWER(t.subject) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                    OR EXISTS (SELECT 1 FROM Message m WHERE m.thread = t AND (
                        LOWER(m.sender.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                        OR LOWER(COALESCE(m.sender.displayName, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                        OR LOWER(COALESCE(m.bodyPlain, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))))
                    OR EXISTS (SELECT 1 FROM MessageRecipient r JOIN r.message rm2 JOIN r.participant p
                        WHERE rm2.thread = t AND (
                            LOWER(p.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                            OR LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))))))
            """)
    Page<ConversationSummaryRow> conversationSummariesFiltered(@Param("owner") User owner,
                                                              @Param("mailbox") String mailbox,
                                                              @Param("q") String q,
                                                              @Param("requireUnread") boolean requireUnread,
                                                              @Param("requireAttachments") boolean requireAttachments,
                                                              @Param("requireAwaiting") boolean requireAwaiting,
                                                              Pageable pageable);

    // Dashboard tile count for a given scope + triage filter (no free-text).
    @Query("""
            SELECT COUNT(DISTINCT t.conversationKey) FROM EmailThread t
            WHERE t.owner = :owner AND t.conversationKey IS NOT NULL
              AND (CAST(:mailbox AS string) IS NULL OR (
                    EXISTS (SELECT 1 FROM Message sm WHERE sm.thread = t
                            AND LOWER(sm.sender.email) = CAST(:mailbox AS string))
                    OR EXISTS (SELECT 1 FROM MessageRecipient rr JOIN rr.message rm JOIN rr.participant rp
                               WHERE rm.thread = t AND LOWER(rp.email) = CAST(:mailbox AS string))))
              AND (:requireUnread = false OR EXISTS (
                    SELECT 1 FROM EmailThread ut WHERE ut.owner = :owner
                      AND ut.conversationKey = t.conversationKey AND ut.unread = true))
              AND (:requireAttachments = false OR EXISTS (
                    SELECT 1 FROM Message am JOIN am.attachments aa JOIN am.thread at
                    WHERE at.owner = :owner AND at.conversationKey = t.conversationKey))
              AND (:requireAwaiting = false OR EXISTS (
                    SELECT 1 FROM Message lm JOIN lm.thread lt
                    WHERE lt.owner = :owner AND lt.conversationKey = t.conversationKey
                      AND lm.outbound = false
                      AND lm.sentAt = (SELECT MAX(m3.sentAt) FROM Message m3 JOIN m3.thread t3
                                       WHERE t3.owner = :owner AND t3.conversationKey = t.conversationKey)))
            """)
    long countConversationsFiltered(@Param("owner") User owner,
                                    @Param("mailbox") String mailbox,
                                    @Param("requireUnread") boolean requireUnread,
                                    @Param("requireAttachments") boolean requireAttachments,
                                    @Param("requireAwaiting") boolean requireAwaiting);

    // The latest message in each of the given conversations, for the list preview.
    // A sentAt tie can yield >1 row per key; the caller keeps the first.
    @Query("""
            SELECT t.conversationKey AS conversationKey, m.id AS messageId,
                   m.bodyPlain AS bodyPlain, m.bodyHtml AS bodyHtml,
                   m.sentAt AS sentAt, m.outbound AS outbound,
                   m.sender.email AS senderEmail, m.sender.displayName AS senderDisplayName
            FROM Message m JOIN m.thread t
            WHERE t.owner = :owner AND t.conversationKey IN :keys
              AND m.sentAt = (SELECT MAX(m2.sentAt) FROM Message m2 JOIN m2.thread t2
                              WHERE t2.owner = :owner AND t2.conversationKey = t.conversationKey)
            """)
    List<ConversationPreviewRow> latestMessagePerConversation(@Param("owner") User owner,
                                                              @Param("keys") Collection<String> keys);

    interface ConversationPreviewRow {
        String getConversationKey();
        Long getMessageId();
        String getBodyPlain();
        String getBodyHtml();
        LocalDateTime getSentAt();
        boolean getOutbound();
        String getSenderEmail();
        String getSenderDisplayName();
    }

    // Senders and (non-Bcc) recipients across each conversation's threads, used
    // to label the chat list and the group member panel. Merged + owner-filtered
    // in Java; two queries avoid a JPQL UNION.
    @Query("""
            SELECT t.conversationKey AS conversationKey,
                   p.email AS email, p.displayName AS displayName
            FROM Message m JOIN m.thread t JOIN m.sender p
            WHERE t.owner = :owner AND t.conversationKey IN :keys
            """)
    List<ConversationParticipantRow> sendersForConversations(@Param("owner") User owner,
                                                             @Param("keys") Collection<String> keys);

    @Query("""
            SELECT t.conversationKey AS conversationKey,
                   p.email AS email, p.displayName AS displayName
            FROM MessageRecipient r JOIN r.message m JOIN m.thread t JOIN r.participant p
            WHERE t.owner = :owner AND t.conversationKey IN :keys
              AND r.recipientType <> :excludeType
            """)
    List<ConversationParticipantRow> recipientsForConversations(@Param("owner") User owner,
                                                               @Param("keys") Collection<String> keys,
                                                               @Param("excludeType") RecipientType excludeType);

    interface ConversationParticipantRow {
        String getConversationKey();
        String getEmail();
        String getDisplayName();
    }

    // Every message in a conversation (all threads sharing the key), oldest first
    // — the unified timeline. Generalizes findConversationWithSender to a set.
    @Query("""
            SELECT m FROM Message m JOIN m.thread t
            WHERE t.owner = :owner AND t.conversationKey = :key
            ORDER BY m.sentAt ASC
            """)
    List<Message> findMessagesByConversationKey(@Param("owner") User owner,
                                                @Param("key") String key);

    // Per-mailbox variant: the unified timeline for a conversation, but only the
    // threads that involve :mailbox (already lowercased). Keeps an account's chat
    // history separate from the same correspondent reached via another account.
    @Query("""
            SELECT m FROM Message m JOIN m.thread t
            WHERE t.owner = :owner AND t.conversationKey = :key
              AND (
                EXISTS (SELECT 1 FROM Message sm WHERE sm.thread = t
                        AND LOWER(sm.sender.email) = :mailbox)
                OR EXISTS (SELECT 1 FROM MessageRecipient rr JOIN rr.message rm JOIN rr.participant rp
                           WHERE rm.thread = t AND LOWER(rp.email) = :mailbox)
              )
            ORDER BY m.sentAt ASC
            """)
    List<Message> findMessagesByConversationKeyForMailbox(@Param("owner") User owner,
                                                          @Param("mailbox") String mailbox,
                                                          @Param("key") String key);

    // The conversation's threads, most-recent first — used to pick the reply target.
    @Query("""
            SELECT t FROM EmailThread t
            WHERE t.owner = :owner AND t.conversationKey = :key
            ORDER BY t.updatedAt DESC
            """)
    List<EmailThread> findThreadsByConversationKey(@Param("owner") User owner,
                                                   @Param("key") String key);

    // Per-mailbox variant: the conversation's threads that involve :mailbox
    // (already lowercased) — used to mark only the opened account's threads read.
    @Query("""
            SELECT t FROM EmailThread t
            WHERE t.owner = :owner AND t.conversationKey = :key
              AND (
                EXISTS (SELECT 1 FROM Message sm WHERE sm.thread = t
                        AND LOWER(sm.sender.email) = :mailbox)
                OR EXISTS (SELECT 1 FROM MessageRecipient rr JOIN rr.message rm JOIN rr.participant rp
                           WHERE rm.thread = t AND LOWER(rp.email) = :mailbox)
              )
            ORDER BY t.updatedAt DESC
            """)
    List<EmailThread> findThreadsByConversationKeyForMailbox(@Param("owner") User owner,
                                                             @Param("mailbox") String mailbox,
                                                             @Param("key") String key);

    @Query("""
            SELECT m.sender.email AS email,
                   MAX(m.sender.displayName) AS displayName,
                   COUNT(DISTINCT m.thread.id) AS threadCount
            FROM Message m
            WHERE m.thread.owner = :owner
            GROUP BY m.sender.email
            ORDER BY COUNT(DISTINCT m.thread.id) DESC, m.sender.email ASC
            """)
    List<SenderGroupRow> topSenders(@Param("owner") User owner, Pageable pageable);

    interface SenderGroupRow {
        String getEmail();
        String getDisplayName();
        long getThreadCount();
    }

    // The most recent inbound (received) sender for each thread in the page, so
    // the inbox list can show who each conversation is with when no single
    // sender is filtered. Outbound replies are excluded so we show the
    // correspondent, not "you".
    @Query("""
            SELECT m.thread.id AS threadId,
                   m.sender.displayName AS displayName,
                   m.sender.email AS email
            FROM Message m
            WHERE m.thread.id IN :threadIds
              AND m.outbound = false
              AND m.sentAt = (
                SELECT MAX(m2.sentAt) FROM Message m2
                WHERE m2.thread = m.thread AND m2.outbound = false
              )
            """)
    List<ThreadSenderRow> latestInboundSenders(@Param("threadIds") Collection<Long> threadIds);

    interface ThreadSenderRow {
        Long getThreadId();
        String getDisplayName();
        String getEmail();
    }
}
