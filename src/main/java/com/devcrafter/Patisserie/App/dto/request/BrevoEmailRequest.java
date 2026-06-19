package com.devcrafter.Patisserie.App.dto.request;

import com.devcrafter.Patisserie.App.dto.Attachment;
import com.devcrafter.Patisserie.App.dto.Recipient;
import com.devcrafter.Patisserie.App.dto.Sender;

import java.util.List;

public record BrevoEmailRequest(
        Sender sender,
        List<Recipient> to,
        String subject,
        String htmlContent,
        List<Attachment> attachments
) {}
