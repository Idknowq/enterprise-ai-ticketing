package com.enterprise.ticketing.ai.provider;

import com.enterprise.ticketing.config.ApplicationProperties;
import com.enterprise.ticketing.ticket.domain.TicketPriority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmProviderRouterTest {

    private final RuleBasedStructuredLlmProvider fallbackProvider = new RuleBasedStructuredLlmProvider();
    private final StructuredLlmOutputValidator outputValidator = new StructuredLlmOutputValidator();

    @Test
    void fallsBackToRuleBasedWhenPrimaryProviderFails() {
        DeepSeekStructuredLlmProvider deepSeekProvider = mock(DeepSeekStructuredLlmProvider.class);
        when(deepSeekProvider.classify(any())).thenThrow(new IllegalStateException("deepseek timeout"));
        when(deepSeekProvider.providerType()).thenReturn("deepseek");

        LlmProviderRouter router = new LlmProviderRouter(
                deepSeekProperties(),
                fallbackProvider,
                providerOf(deepSeekProvider),
                emptyLocalProvider(),
                outputValidator
        );

        StructuredLlmResponse<AiClassificationOutput> response = router.classify(
                new AiClassificationInput("VPN 连接失败", "客户端提示证书失效", null)
        );

        assertThat(response.providerType()).isEqualTo("rule-based");
        assertThat(response.modelName()).isEqualTo("mvp-rule-based");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).contains("deepseek timeout");
    }

    @Test
    void fallsBackToLocalProviderBeforeRuleBased() {
        DeepSeekStructuredLlmProvider deepSeekProvider = mock(DeepSeekStructuredLlmProvider.class);
        LocalStructuredLlmProvider localStructuredLlmProvider = mock(LocalStructuredLlmProvider.class);
        when(deepSeekProvider.classify(any())).thenThrow(new IllegalStateException("deepseek timeout"));
        when(deepSeekProvider.providerType()).thenReturn("deepseek");
        when(localStructuredLlmProvider.classify(any())).thenReturn(new StructuredLlmResponse<>(
                new AiClassificationOutput("VPN_ISSUE", TicketPriority.MEDIUM, 0.61d),
                "local-llm",
                "qwen2.5:3b",
                8,
                5,
                false,
                null
        ));

        ApplicationProperties properties = deepSeekProperties();
        properties.getAi().getProvider().getLocal().setEnabled(true);

        LlmProviderRouter router = new LlmProviderRouter(
                properties,
                fallbackProvider,
                providerOf(deepSeekProvider),
                localProviderOf(localStructuredLlmProvider),
                outputValidator
        );

        StructuredLlmResponse<AiClassificationOutput> response = router.classify(
                new AiClassificationInput("VPN 连接失败", "客户端提示证书失效", null)
        );

        assertThat(response.providerType()).isEqualTo("local-llm");
        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.fallbackReason()).contains("deepseek timeout");
    }

    @Test
    void retriesPrimaryProviderWhenStructuredOutputIsInvalid() {
        DeepSeekStructuredLlmProvider deepSeekProvider = mock(DeepSeekStructuredLlmProvider.class);
        when(deepSeekProvider.extract(any()))
                .thenReturn(new StructuredLlmResponse<>(
                        new AiExtractionOutput(null),
                        "deepseek",
                        "deepseek-chat",
                        10,
                        5,
                        false,
                        null
                ))
                .thenReturn(new StructuredLlmResponse<>(
                        new AiExtractionOutput(java.util.Map.of("system", "VPN")),
                        "deepseek",
                        "deepseek-chat",
                        12,
                        6,
                        false,
                        null
                ));
        when(deepSeekProvider.providerType()).thenReturn("deepseek");

        LlmProviderRouter router = new LlmProviderRouter(
                deepSeekProperties(),
                fallbackProvider,
                providerOf(deepSeekProvider),
                emptyLocalProvider(),
                outputValidator
        );

        StructuredLlmResponse<AiExtractionOutput> response = router.extract(
                new AiExtractionInput("VPN 连接失败", "证书失效", "VPN_ISSUE")
        );

        assertThat(response.providerType()).isEqualTo("deepseek");
        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.output().extractedFields()).containsEntry("system", "VPN");
        verify(deepSeekProvider, times(2)).extract(any());
    }

    private ApplicationProperties deepSeekProperties() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.getAi().getProvider().setType("deepseek");
        properties.getAi().getProvider().setModel("deepseek-chat");
        return properties;
    }

    private ObjectProvider<DeepSeekStructuredLlmProvider> providerOf(DeepSeekStructuredLlmProvider provider) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("deepSeekStructuredLlmProvider", provider);
        return beanFactory.getBeanProvider(DeepSeekStructuredLlmProvider.class);
    }

    private ObjectProvider<LocalStructuredLlmProvider> localProviderOf(LocalStructuredLlmProvider provider) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("localStructuredLlmProvider", provider);
        return beanFactory.getBeanProvider(LocalStructuredLlmProvider.class);
    }

    private ObjectProvider<LocalStructuredLlmProvider> emptyLocalProvider() {
        return new StaticListableBeanFactory().getBeanProvider(LocalStructuredLlmProvider.class);
    }
}
