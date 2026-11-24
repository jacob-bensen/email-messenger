package com.emailmessenger.email;

import com.emailmessenger.billing.PlanLimitService;
import com.emailmessenger.domain.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The rules for the per-user mail preferences and per-mailbox signatures,
 * shared by the website's {@code /mailboxes} and {@code /api/v1/mailboxes}
 * (docs/API.md §5.10, §5.22, §5.23).
 */
@Component
public class MailPreferencesRules {

    /** Sync-frequency choices, in minutes, ascending. */
    public static final List<Integer> SYNC_FREQUENCY_CHOICES = List.of(5, 15, 30, 60);

    public static final int SIGNATURE_MAX_LENGTH = 2000;

    private final PlanLimitService planLimitService;
    private final PollingPolicy pollingPolicy;

    MailPreferencesRules(PlanLimitService planLimitService, PollingPolicy pollingPolicy) {
        this.planLimitService = planLimitService;
        this.pollingPolicy = pollingPolicy;
    }

    /** The plan's fastest cadence, which is also what "plan default" means. */
    public long minSyncFrequencyMinutes(User user) {
        return pollingPolicy.intervalFor(planLimitService.currentPlan(user)).toMinutes();
    }

    /** {@code null} (plan default) always passes. */
    void checkSyncFrequency(User user, Integer minutes) {
        if (minutes == null) {
            return;
        }
        if (!SYNC_FREQUENCY_CHOICES.contains(minutes)) {
            throw new MailSettingsException(MailSettingsException.Reason.INVALID_SYNC_FREQUENCY,
                    "Pick a valid sync frequency.");
        }
        long floor = minSyncFrequencyMinutes(user);
        if (minutes < floor) {
            throw new MailSettingsException(MailSettingsException.Reason.SYNC_FREQUENCY_BELOW_PLAN,
                    "Checking every " + minutes + " minutes needs a paid plan — your plan syncs every "
                            + floor + " minutes at the fastest.");
        }
    }

    static void checkSignature(String signature) {
        if (signature != null && signature.length() > SIGNATURE_MAX_LENGTH) {
            throw new MailSettingsException(MailSettingsException.Reason.SIGNATURE_TOO_LONG,
                    "Signatures are limited to " + SIGNATURE_MAX_LENGTH + " characters.");
        }
    }
}
