package com.emailmessenger.push;

import com.emailmessenger.domain.PushSubscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.PushSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PushSubscriptionService {

    static final int MAX_SUBSCRIPTIONS_PER_USER = 20;

    private final PushSubscriptionRepository subscriptions;

    PushSubscriptionService(PushSubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Transactional
    public PushSubscription upsert(User user, String endpoint, String p256dhKey,
                                   String authKey, String userAgent) {
        PushSubscription existing = subscriptions.findByUserAndEndpoint(user, endpoint).orElse(null);
        if (existing != null) {
            existing.setP256dhKey(p256dhKey);
            existing.setAuthKey(authKey);
            existing.setUserAgent(userAgent);
            return subscriptions.save(existing);
        }
        // Prevent runaway growth from a page that re-subscribes on every load.
        if (subscriptions.countByUser(user) >= MAX_SUBSCRIPTIONS_PER_USER) {
            throw new SubscriptionLimitReachedException();
        }
        PushSubscription created = new PushSubscription();
        created.setUser(user);
        created.setEndpoint(endpoint);
        created.setP256dhKey(p256dhKey);
        created.setAuthKey(authKey);
        created.setUserAgent(userAgent);
        return subscriptions.save(created);
    }

    @Transactional
    public boolean remove(User user, String endpoint) {
        return subscriptions.deleteByUserAndEndpoint(user, endpoint) > 0;
    }

    @Transactional(readOnly = true)
    public List<PushSubscription> list(User user) {
        return subscriptions.findByUserOrderByCreatedAtDesc(user);
    }

    public static class SubscriptionLimitReachedException extends RuntimeException {
    }
}
