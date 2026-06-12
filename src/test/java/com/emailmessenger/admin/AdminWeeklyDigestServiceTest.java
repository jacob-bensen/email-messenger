package com.emailmessenger.admin;

import com.emailmessenger.auth.UserService;
import com.emailmessenger.billing.BillingPeriod;
import com.emailmessenger.billing.StripeCheckoutGateway;
import com.emailmessenger.billing.StripePortalGateway;
import com.emailmessenger.domain.Plan;
import com.emailmessenger.domain.Subscription;
import com.emailmessenger.domain.User;
import com.emailmessenger.repository.SubscriptionRepository;
import com.emailmessenger.service.ReplyService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AdminWeeklyDigestServiceTest {

    @Autowired AdminWeeklyDigestService digestService;
    @Autowired AdminProperties adminProperties;
    @Autowired UserService userService;
    @Autowired SubscriptionRepository subscriptions;

    @MockBean JavaMailSender mailSender;
    @MockBean StripeCheckoutGateway stripeCheckout;
    @MockBean StripePortalGateway stripePortal;
    @MockBean ReplyService replyService;

    @BeforeEach
    void resetAdminEmailsAndStubMime() {
        adminProperties.setEmails(List.of());
        when(mailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage((Session) null));
    }

    @Test
    void emptyAllowlistSendsNothing() {
        int sent = digestService.sendDigest();

        assertThat(sent).isZero();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void everyAllowlistedAddressReceivesOneEmail() throws Exception {
        adminProperties.setEmails(List.of("op1@example.com", "op2@example.com"));

        int sent = digestService.sendDigest();

        assertThat(sent).isEqualTo(2);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, atLeastOnce()).send(captor.capture());
        List<MimeMessage> all = captor.getAllValues();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getAllRecipients()[0].toString()).isEqualTo("op1@example.com");
        assertThat(all.get(1).getAllRecipients()[0].toString()).isEqualTo("op2@example.com");
    }

    @Test
    void bodyContainsMrrArrAndDashboardLink() throws Exception {
        adminProperties.setEmails(List.of("op@example.com"));
        User payer = userService.register("payer@example.com", "password1", null);
        Subscription sub = new Subscription(payer, "cus_payer", "active");
        sub.setPlan(Plan.PERSONAL);
        sub.setBillingPeriod(BillingPeriod.MONTHLY);
        subscriptions.save(sub);

        digestService.sendDigest();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        String body = (String) mime.getContent();
        assertThat(mime.getSubject()).contains("MailIM weekly:");
        assertThat(mime.getSubject()).contains("$9 MRR");
        assertThat(body).contains("MRR:");
        assertThat(body).contains("$9");
        assertThat(body).contains("ARR:");
        assertThat(body).contains("$108");
        assertThat(body).contains("Active subscribers:  1");
        assertThat(body).contains("/admin/revenue");
    }

    @Test
    void weeklyDeltasCountActiveAndCanceledTouchedInsideTheLookback() throws Exception {
        adminProperties.setEmails(List.of("op@example.com"));

        User newPayer = userService.register("new@example.com", "password1", null);
        Subscription fresh = new Subscription(newPayer, "cus_new", "active");
        fresh.setPlan(Plan.PERSONAL);
        fresh.setBillingPeriod(BillingPeriod.MONTHLY);
        subscriptions.save(fresh);

        User churned = userService.register("churn@example.com", "password1", null);
        Subscription cancelled = new Subscription(churned, "cus_churn", "canceled");
        cancelled.setPlan(Plan.PERSONAL);
        cancelled.setBillingPeriod(BillingPeriod.MONTHLY);
        subscriptions.save(cancelled);

        digestService.sendDigest();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String body = (String) captor.getValue().getContent();
        assertThat(body).contains("New paying customers: 1");
        assertThat(body).contains("Churn (canceled):     1");
    }

    @Test
    void bodyIncludesPerPlanChurnBreakdownForCancellationsInsideLookback() throws Exception {
        adminProperties.setEmails(List.of("op@example.com"));

        User personalChurn = userService.register("p-churn@example.com", "password1", null);
        Subscription pSub = new Subscription(personalChurn, "cus_p_churn", "canceled");
        pSub.setPlan(Plan.PERSONAL);
        pSub.setBillingPeriod(BillingPeriod.MONTHLY);
        subscriptions.save(pSub);

        User teamChurn = userService.register("t-churn@example.com", "password1", null);
        Subscription tSub = new Subscription(teamChurn, "cus_t_churn", "canceled");
        tSub.setPlan(Plan.TEAM);
        tSub.setBillingPeriod(BillingPeriod.ANNUAL);
        subscriptions.save(tSub);

        digestService.sendDigest();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String body = (String) captor.getValue().getContent();
        assertThat(body).contains("Personal:       1 canceled (-$9 MRR)");
        assertThat(body).contains("Team:           1 canceled (-$24 MRR)");
        assertThat(body).contains("Enterprise:     0 canceled (-$0 MRR)");
    }

    @Test
    void perRecipientMailExceptionIsLoggedAndDoesNotAbortTheSweep() {
        adminProperties.setEmails(List.of("flaky@example.com", "good@example.com"));
        doThrow(new MailSendException("smtp down"))
                .doNothing()
                .when(mailSender).send(any(MimeMessage.class));

        int sent = digestService.sendDigest();

        assertThat(sent).isEqualTo(1);
        verify(mailSender, atLeastOnce()).send(any(MimeMessage.class));
    }
}
