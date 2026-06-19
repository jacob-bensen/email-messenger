package com.emailmessenger.team;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.domain.Team;
import com.emailmessenger.domain.TeamMember;
import com.emailmessenger.domain.ThreadNote;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.TeamMemberRepository;
import com.emailmessenger.repository.TeamRepository;
import com.emailmessenger.web.SiteProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code @token} mentions out of a saved {@link ThreadNote} body,
 * resolves them against members of the owner's team, and emails each
 * matched user a notification pointing at {@code /threads/{id}#note-{id}}.
 *
 * <p>EPIC-16 M3. The author is never notified of self-mentions, an
 * unmatched token is silently dropped (no error to the poster), and a
 * mail-send failure for one recipient does not block the rest.
 */
@Service
public class NoteMentionService {

    private static final Logger log = LoggerFactory.getLogger(NoteMentionService.class);

    // Negative lookbehind keeps "@" embedded in an email address (e.g.
    // "jane@example.com") from registering as a mention — only an "@" at
    // the start of input or after a non-handle character counts.
    public static final Pattern MENTION_TOKEN =
            Pattern.compile("(?<![A-Za-z0-9._-])@([A-Za-z0-9._-]{1,64})");
    static final int SNIPPET_MAX = 280;

    private final TeamMemberRepository teamMembers;
    private final TeamRepository teams;
    private final JavaMailSender mailSender;
    private final SiteProperties site;

    @Value("${spring.mail.username:noreply@conexusmail.com}")
    private String fromAddress = "noreply@conexusmail.com";

    NoteMentionService(TeamMemberRepository teamMembers,
                       TeamRepository teams,
                       JavaMailSender mailSender,
                       SiteProperties site) {
        this.teamMembers = teamMembers;
        this.teams = teams;
        this.mailSender = mailSender;
        this.site = site;
    }

    /**
     * View-model row for the @-picker in the notes textarea.
     */
    public record MentionCandidate(Long userId, String handle, String label, String email) {}

    /**
     * Candidates the @-picker should offer to {@code viewer} for the given
     * {@code team}. The viewer is excluded so they don't pick themselves.
     * {@code handle} is what gets inserted into the textarea (no leading @);
     * {@code label} is what's shown next to it.
     */
    /**
     * Look-up variant used by the conversation controller: returns the
     * picker rows for the team owned by {@code thread.owner}, or empty if
     * the owner has not formed a team yet (no lazy-create on GET).
     */
    @Transactional(readOnly = true)
    public List<MentionCandidate> candidatesForThread(EmailThread thread, User viewer) {
        if (thread == null) {
            return List.of();
        }
        return teams.findByOwnerUser(thread.getOwner())
                .map(team -> candidatesFor(team, viewer))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<MentionCandidate> candidatesFor(Team team, User viewer) {
        if (team == null) {
            return List.of();
        }
        Long viewerId = viewer != null ? viewer.getId() : null;
        List<MentionCandidate> out = new ArrayList<>();
        for (TeamMember m : teamMembers.findByTeamOrderByJoinedAtAsc(team)) {
            User u = m.getUser();
            if (viewerId != null && viewerId.equals(u.getId())) {
                continue;
            }
            List<String> handles = handlesFor(u);
            if (handles.isEmpty()) {
                continue;
            }
            String handle = handles.get(handles.size() - 1);
            String label = (u.getDisplayName() != null && !u.getDisplayName().isBlank())
                    ? u.getDisplayName().trim() : u.getEmail();
            out.add(new MentionCandidate(u.getId(), handle, label, u.getEmail()));
        }
        return out;
    }

    /**
     * Notify each team member mentioned in {@code note.body}. Returns the
     * count of recipients that were actually sent an email (post-dedupe,
     * post-author-exclusion). A mail failure for one recipient is logged
     * and counted as zero for that recipient; the rest still get the mail.
     */
    @Transactional(readOnly = true)
    public int notify(ThreadNote note) {
        Objects.requireNonNull(note, "note");
        Team team = note.getTeam();
        if (team == null) {
            return 0;
        }
        List<User> members = teamMembers.findByTeamOrderByJoinedAtAsc(team).stream()
                .map(TeamMember::getUser)
                .toList();
        List<User> mentioned = resolveMentions(note.getBody(), members, note.getAuthorUser());
        if (mentioned.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (User recipient : mentioned) {
            try {
                mailSender.send(compose(note, recipient));
                sent++;
            } catch (MailException e) {
                log.warn("Mention-notify mail send failed (note={}, recipient={}): {}",
                        note.getId(), recipient.getEmail(), e.getMessage());
            }
        }
        return sent;
    }

    /**
     * Resolve {@code @token} mentions in {@code body} against
     * {@code candidates}. Returns matched users in first-mention order,
     * de-duplicated, with {@code excluded} removed (typically the author so
     * they aren't emailed about their own note).
     */
    List<User> resolveMentions(String body, Collection<User> candidates, User excluded) {
        if (body == null || body.isEmpty() || candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        Map<String, User> handleIndex = buildHandleIndex(candidates);
        if (handleIndex.isEmpty()) {
            return List.of();
        }
        Long excludedId = excluded != null ? excluded.getId() : null;
        LinkedHashSet<User> matched = new LinkedHashSet<>();
        Matcher m = MENTION_TOKEN.matcher(body);
        while (m.find()) {
            String token = m.group(1).toLowerCase(Locale.ROOT);
            User user = handleIndex.get(token);
            if (user == null) {
                continue;
            }
            if (excludedId != null && excludedId.equals(user.getId())) {
                continue;
            }
            matched.add(user);
        }
        return new ArrayList<>(matched);
    }

    /**
     * Each candidate becomes addressable by up to three handles: the full
     * email, the email local part, and the normalized display name. On a
     * handle collision the earlier candidate wins (deterministic given the
     * stable join order).
     */
    private static Map<String, User> buildHandleIndex(Collection<User> candidates) {
        Map<String, User> idx = new LinkedHashMap<>();
        for (User u : candidates) {
            for (String handle : handlesFor(u)) {
                idx.putIfAbsent(handle, u);
            }
        }
        return idx;
    }

    static List<String> handlesFor(User user) {
        List<String> out = new ArrayList<>(3);
        String email = user.getEmail();
        if (email != null) {
            String lower = email.toLowerCase(Locale.ROOT);
            out.add(lower);
            int at = lower.indexOf('@');
            if (at > 0) {
                out.add(lower.substring(0, at));
            }
        }
        String displayName = user.getDisplayName();
        if (displayName != null) {
            String normalized = displayName.toLowerCase(Locale.ROOT)
                    .replaceAll("[^a-z0-9._-]", "");
            if (!normalized.isEmpty()) {
                out.add(normalized);
            }
        }
        return out;
    }

    private MimeMessage compose(ThreadNote note, User recipient) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipient.getEmail());
            helper.setSubject(renderSubject(note));
            helper.setText(renderBody(note, recipient), false);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not compose mention email", e);
        }
        return mime;
    }

    private String renderSubject(ThreadNote note) {
        EmailThread thread = note.getThread();
        String subject = (thread != null && thread.getSubject() != null && !thread.getSubject().isBlank())
                ? thread.getSubject().trim()
                : "a thread";
        return authorLabel(note.getAuthorUser()) + " mentioned you on \"" + subject + "\"";
    }

    private String renderBody(ThreadNote note, User recipient) {
        String greeting = (recipient.getDisplayName() != null && !recipient.getDisplayName().isBlank())
                ? recipient.getDisplayName().trim() : "there";
        EmailThread thread = note.getThread();
        Long threadId = thread != null ? thread.getId() : null;
        String url = site.getBaseUrl() + "/threads/" + threadId + "#note-" + note.getId();
        return "Hi " + greeting + ",\n\n"
                + authorLabel(note.getAuthorUser())
                + " mentioned you in an internal note on a ConexusMail thread:\n\n"
                + "  " + snippet(note.getBody()) + "\n\n"
                + "Open the thread to reply or add context:\n\n"
                + url + "\n\n"
                + "Internal notes are private to your team — they're never sent in "
                + "the email reply.\n";
    }

    static String snippet(String body) {
        if (body == null) {
            return "";
        }
        String collapsed = body.trim().replaceAll("\\s+", " ");
        if (collapsed.length() <= SNIPPET_MAX) {
            return collapsed;
        }
        return collapsed.substring(0, SNIPPET_MAX - 1).trim() + "…";
    }

    private static String authorLabel(User author) {
        if (author == null) {
            return "A teammate";
        }
        String name = author.getDisplayName();
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return author.getEmail();
    }
}
