package com.emailmessenger.web;

import com.emailmessenger.billing.PlanLimitExceededException;
import com.emailmessenger.billing.PlanLimitKind;
import com.emailmessenger.billing.UpgradeModal;
import com.emailmessenger.domain.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailSendException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    @RestController
    static class ThrowingController {
        @GetMapping("/test/mail")
        void mail() { throw new MailSendException("mail server down"); }

        @GetMapping("/test/not-found")
        void notFound() { throw new NoSuchElementException("item missing"); }

        @GetMapping("/test/conflict")
        void conflict() { throw new DataIntegrityViolationException("constraint violated"); }

        @GetMapping("/test/server-error")
        void server() { throw new RuntimeException("unexpected error"); }

        @GetMapping("/test/plan-limit")
        void planLimit() {
            throw new PlanLimitExceededException(Plan.FREE, PlanLimitKind.THREAD_COUNT, 500, 500);
        }
    }

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mailExceptionReturns502WithErrorView() throws Exception {
        mockMvc.perform(get("/test/mail"))
                .andExpect(status().isBadGateway())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 502))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void noSuchElementReturns404WithErrorView() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void dataIntegrityViolationReturns409WithErrorView() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 409))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void unhandledExceptionReturns500WithErrorView() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 500))
                .andExpect(model().attributeExists("message"));
    }

    @Test
    void planLimitExceededRedirectsToThreadsWithUpgradeModalFlash() throws Exception {
        UpgradeModal modal = (UpgradeModal) mockMvc.perform(get("/test/plan-limit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"))
                .andExpect(flash().attributeExists("upgradeModal"))
                .andReturn()
                .getFlashMap()
                .get("upgradeModal");

        assertThat(modal).isNotNull();
        assertThat(modal.currentPlan()).isEqualTo(Plan.FREE);
        assertThat(modal.kind()).isEqualTo(PlanLimitKind.THREAD_COUNT);
        assertThat(modal.limit()).isEqualTo(500);
        assertThat(modal.current()).isEqualTo(500);
        assertThat(modal.upgradeTarget()).isEqualTo(Plan.PRO);
        assertThat(modal.upgradeTargetParam()).isEqualTo("pro");
    }
}
