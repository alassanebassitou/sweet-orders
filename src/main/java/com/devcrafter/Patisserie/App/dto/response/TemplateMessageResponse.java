package com.devcrafter.Patisserie.App.dto.response;

import com.devcrafter.Patisserie.App.models.TemplateMessage;
import lombok.Data;

@Data
public class TemplateMessageResponse {
    private Long id;
    private String type;
    private String libelle;
    private String content;

    public static TemplateMessageResponse from(
            TemplateMessage t) {
        TemplateMessageResponse r =
                new TemplateMessageResponse();
        r.setId(t.getId());
        r.setType(t.getType());
        r.setLibelle(t.getLibelle());
        r.setContent(t.getContent());
        return r;
    }
}
