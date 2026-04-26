package com.emailmessenger.service;

import com.emailmessenger.domain.EmailThread;
import java.util.List;

public record Conversation(
    EmailThread thread,
    List<BubbleRun> runs
) {}
