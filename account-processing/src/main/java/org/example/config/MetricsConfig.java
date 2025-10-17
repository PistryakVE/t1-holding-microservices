package org.example.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.example.accountModels.entity.Card;
import org.example.repository.CardRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Gauge totalProductsGauge(MeterRegistry registry, CardRepository cardRepository) {
        return Gauge.builder("bank.products.total")
                .description("Total number of banking products")
                .register(registry, cardRepository,
                        repo -> repo.countByStatus("ACTIVE"));
    }

    @Bean
    public Gauge debitCardsGauge(MeterRegistry registry, CardRepository cardRepository) {
        return Gauge.builder("bank.products.cards.by_type")
                .description("Number of cards by type")
                .tag("type", "debit")
                .register(registry, cardRepository,
                        repo -> repo.countByCardTypeAndStatus("DEBIT", "ACTIVE"));
    }

    @Bean
    public Gauge creditCardsGauge(MeterRegistry registry, CardRepository cardRepository) {
        return Gauge.builder("bank.products.cards.by_type")
                .description("Number of cards by type")
                .tag("type", "credit")
                .register(registry, cardRepository,
                        repo -> repo.countByCardTypeAndStatus("CREDIT", "ACTIVE"));
    }
}