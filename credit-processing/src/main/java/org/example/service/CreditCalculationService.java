package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.creditModels.PaymentRegistry;
import org.example.creditModels.ProductRegistry;
import org.example.repository.PaymentRegistryRepository;
import org.example.repository.ProductRegistryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreditCalculationService {

    private final PaymentRegistryRepository paymentRegistryRepository;
    private final ProductRegistryRepository productRegistryRepository;

    public BigDecimal calculateTotalDebt(String clientId) {
        List<PaymentRegistry> allPayments = paymentRegistryRepository.findAllPaymentsByClientId(clientId);

        // Суммируем debt_amount всех неоплаченных платежей
        return allPayments.stream()
                .filter(payment -> !payment.getExpired()) // Только не истекшие (неоплаченные)
                .map(PaymentRegistry::getDebtAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean hasOverduePayments(String clientId) {
        List<PaymentRegistry> overduePayments = paymentRegistryRepository.findOverduePaymentsByClientId(clientId);
        return !overduePayments.isEmpty();
    }

    public int getOverduePaymentsCount(String clientId) {
        List<PaymentRegistry> overduePayments = paymentRegistryRepository.findOverduePaymentsByClientId(clientId);
        return overduePayments.size();
    }

    public Map<String, Object> getClientCreditSummary(String clientId) {
        BigDecimal totalDebt = calculateTotalDebt(clientId);
        boolean hasOverdue = hasOverduePayments(clientId);
        int overdueCount = getOverduePaymentsCount(clientId);

        // Получаем активные продукты клиента
        List<ProductRegistry> activeProducts = productRegistryRepository.findByClientId(clientId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalDebt", totalDebt);
        summary.put("hasOverdue", hasOverdue);
        summary.put("overdueCount", overdueCount);
        summary.put("activeProductsCount", activeProducts.size());
        summary.put("activeProducts", activeProducts);

        return summary;
    }
}