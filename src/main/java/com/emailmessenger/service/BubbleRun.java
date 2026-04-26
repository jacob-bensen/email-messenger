package com.emailmessenger.service;

import com.emailmessenger.domain.Participant;
import java.util.List;

record BubbleRun(
    Participant sender,
    List<BubbleMessage> messages
) {}
