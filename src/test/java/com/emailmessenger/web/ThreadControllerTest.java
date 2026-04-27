package com.emailmessenger.web;

import com.emailmessenger.domain.EmailThread;
import com.emailmessenger.repository.EmailThreadRepository;
import com.emailmessenger.service.Conversation;
import com.emailmessenger.service.ReplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class ThreadControllerTest {

    @Mock EmailThreadRepository threadRepository;
    @Mock ThreadViewService threadViewService;
    @Mock ReplyService replyService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ThreadController controller = new ThreadController(threadRepository, threadViewService, replyService);
        // Prefix/suffix prevents InternalResourceViewResolver from producing a path that
        // matches the request URL (which would cause a circular-dispatch error in standalone mode).
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void rootRedirectsToThreads() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads"));
    }

    @Test
    void listThreadsReturnsThreadsViewWithModel() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findAllByOrderByUpdatedAtDesc(any(Pageable.class))).thenReturn(empty);

        mockMvc.perform(get("/threads"))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"))
                .andExpect(model().attributeExists("threads"));
    }

    @Test
    void listThreadsModelContainsTodayAndYesterday() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findAllByOrderByUpdatedAtDesc(any(Pageable.class))).thenReturn(empty);

        mockMvc.perform(get("/threads"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("today", "yesterday"));
    }

    @Test
    void listThreadsNegativePageClampsToZero() throws Exception {
        Page<EmailThread> empty = new PageImpl<>(List.of());
        when(threadRepository.findAllByOrderByUpdatedAtDesc(any(Pageable.class))).thenReturn(empty);

        mockMvc.perform(get("/threads").param("page", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("threads"));
    }

    @Test
    void viewConversationReturnsConversationView() throws Exception {
        EmailThread thread = new EmailThread("Test Subject", "<root@test>");
        Conversation conv = new Conversation(thread, List.of());
        when(threadViewService.getConversation(1L)).thenReturn(conv);

        mockMvc.perform(get("/threads/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attributeExists("conversation", "replyForm"));
    }

    @Test
    void viewConversationWithUnknownIdReturns404() throws Exception {
        when(threadViewService.getConversation(999L)).thenThrow(new NoSuchElementException());

        mockMvc.perform(get("/threads/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    @Test
    void replyWithEmptyBodyShowsValidationErrorAndConversationView() throws Exception {
        EmailThread thread = new EmailThread("Test", "<root@test>");
        Conversation conv = new Conversation(thread, List.of());
        when(threadViewService.getConversation(1L)).thenReturn(conv);

        mockMvc.perform(post("/threads/1/reply").param("body", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("conversation"))
                .andExpect(model().attributeHasFieldErrors("replyForm", "body"));

        verify(replyService, never()).sendReply(anyLong(), anyString(), anyString());
    }

    @Test
    void replyWithValidBodyRedirectsWithSuccessFlash() throws Exception {
        EmailThread thread = new EmailThread("Test Subject", "<root@test>");
        when(threadRepository.findById(1L)).thenReturn(Optional.of(thread));
        doNothing().when(replyService).sendReply(anyLong(), anyString(), anyString());

        mockMvc.perform(post("/threads/1/reply").param("body", "Thanks for your message!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/threads/1"));
    }

    @Test
    void replyWithUnknownThreadIdReturns404() throws Exception {
        when(threadViewService.getConversation(999L)).thenThrow(new NoSuchElementException());

        // Validation error path triggers getConversation — should result in 404 via exception handler
        mockMvc.perform(post("/threads/999/reply").param("body", ""))
                .andExpect(status().isNotFound());
    }
}
