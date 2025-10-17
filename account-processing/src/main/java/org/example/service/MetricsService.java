package org.example.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final Counter cardCreationCounter;
    private final Counter depositCreationCounter;
    private final Counter creditCreationCounter;

    public MetricsService(MeterRegistry registry) {
        this.cardCreationCounter = Counter.builder("bank.products.created")
                .tag("type", "card")
                .register(registry);

        this.depositCreationCounter = Counter.builder("bank.products.created")
                .tag("type", "deposit")
                .register(registry);

        this.creditCreationCounter = Counter.builder("bank.products.created")
                .tag("type", "credit")
                .register(registry);
    }

    public void incrementCardCounter() {
        cardCreationCounter.increment();
    }

    public void incrementDepositCounter() {
        depositCreationCounter.increment();
    }

    public void incrementCreditCounter() {
        creditCreationCounter.increment();
    }
}