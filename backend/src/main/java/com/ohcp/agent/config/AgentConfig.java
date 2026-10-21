package com.ohcp.agent.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * langchain4j 智能体配置：构造 OpenAI 兼容的 ChatModel Bean。
 */
@Configuration
@EnableConfigurationProperties(LlmProperties.class)
public class AgentConfig {

    @Bean
    public ChatModel chatModel(LlmProperties props) {
        return OpenAiChatModel.builder()
                .baseUrl(props.getBaseUrl())
                .apiKey(props.getApiKey())
                .modelName(props.getModelName())
                .temperature(props.getTemperature())
                .maxTokens(props.getMaxTokens())
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
