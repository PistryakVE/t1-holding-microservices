package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.PaymentMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPayment(PaymentMessageDto paymentMessage) {

        String key = paymentMessage.getMessageId().toString();
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send("client_payments", key, paymentMessage);
        log.info("Payment sent to topic {}: {}", "client_payments", paymentMessage.getMessageId());
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Transaction sent successfully: {}, offset: {}",
                        paymentMessage.getMessageId(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send transaction: {}, error: {}",
                        paymentMessage.getMessageId(), ex.getMessage());
            }
        });
    }

    // Вспомогательные методы для создания тестовых платежей
    public void sendLoanPayment(String accountId, BigDecimal amount) {
        PaymentMessageDto message = new PaymentMessageDto(
                UUID.randomUUID(),
                accountId,
                amount,
                PaymentMessageDto.PaymentType.LOAN_PAYMENT,
                LocalDateTime.now(),
                "Платеж по кредиту"
        );
        sendPayment(message);
    }

    public void sendEarlyRepayment(String accountId, BigDecimal amount) {
        PaymentMessageDto message = new PaymentMessageDto(
                UUID.randomUUID(),
                accountId,
                amount,
                PaymentMessageDto.PaymentType.EARLY_REPAYMENT,
                LocalDateTime.now(),
                "Досрочное погашение"
        );
        sendPayment(message);
    }

    public void sendRegularPayment(String accountId, BigDecimal amount) {
        PaymentMessageDto message = new PaymentMessageDto(
                UUID.randomUUID(),
                accountId,
                amount,
                PaymentMessageDto.PaymentType.REGULAR_PAYMENT,
                LocalDateTime.now(),
                "Регулярный платеж"
        );
        sendPayment(message);
    }

}