package com.marcos.leairning.ai.chat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = ChatAIProperties.PREFIX)
public class ChatAIProperties {

    public static final String PREFIX = "leairning.ai.chat";

    private int maxMessages;
}
