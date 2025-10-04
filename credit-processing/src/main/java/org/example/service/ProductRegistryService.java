package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.aspects.starter.annotation.HttpOutcomeRequestLog;
import org.example.creditModels.PaymentRegistry;
import org.example.creditModels.ProductRegistry;
import org.example.dto.ClientInfo;
import org.example.dto.ClientProductMessage;
import org.example.dto.CreditDecision;
import org.example.repository.PaymentRegistryRepository;
import org.example.repository.ProductRegistryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductRegistryService {
    private final ApplicationContext applicationContext;

    private final RestTemplate restTemplate;
    private final ProductRegistryRepository productRegistryRepository;
    private final PaymentRegistryRepository paymentRegistryRepository;
    private final CreditCalculationService creditCalculationService;

    @Value("${ms1.client-info.url:http://microservices-client-processing-1:8080/api/getclients}")
    private String ms1ClientInfoUrl;

    @Value("${credit.limit.max.total:1000000}")
    private BigDecimal maxTotalLimit;

    public CreditDecision createClientProduct(ClientProductMessage clientProductMessage) {
        try {
            if (clientProductMessage.getClientId() == null || clientProductMessage.getClientId().trim().isEmpty()) {
                return new CreditDecision(false, "Invalid client ID", BigDecimal.ZERO, false, 0, "", 0);
            }

            String clientIdStr = clientProductMessage.getClientId().trim();

            ClientInfo clientInfo = getClientInfoFromMs1ByClientId(clientIdStr);
            if (clientInfo == null) {
                return new CreditDecision(false, "Client info not available", BigDecimal.ZERO, false, 0, "", 0);
            }

            String clientName = clientInfo.getLastName() + " " + clientInfo.getFirstName() + " " + clientInfo.getMiddleName();

            Map<String, Object> creditSummary = creditCalculationService.getClientCreditSummary(clientIdStr);

            BigDecimal totalDebt = (BigDecimal) creditSummary.get("totalDebt");
            boolean hasOverdue = (Boolean) creditSummary.get("hasOverdue");
            int overdueCount = (Integer) creditSummary.get("overdueCount");
            int activeProductsCount = (Integer) creditSummary.get("activeProductsCount");

            CreditDecision decision = makeCreditDecision(creditSummary, clientProductMessage, clientName);

            if (decision.isApproved()) {
                createNewProduct(clientProductMessage, clientInfo);
            }

            return decision;

        } catch (Exception e) {
            return new CreditDecision(false, "Processing error: " + e.getMessage(), BigDecimal.ZERO, false, 0, "", 0);
        }
    }

    private CreditDecision makeCreditDecision(Map<String, Object> creditSummary,
                                              ClientProductMessage newProduct,
                                              String clientName) {

        BigDecimal totalDebt = (BigDecimal) creditSummary.get("totalDebt");
        boolean hasOverdue = (Boolean) creditSummary.get("hasOverdue");
        int overdueCount = (Integer) creditSummary.get("overdueCount");
        int activeProductsCount = (Integer) creditSummary.get("activeProductsCount");

        BigDecimal newProductAmount = getProductAmount(newProduct.getProductKey());
        BigDecimal proposedTotalDebt = totalDebt.add(newProductAmount);

        if (activeProductsCount > 0 && proposedTotalDebt.compareTo(maxTotalLimit) > 0) {
            return new CreditDecision(false,
                    String.format("Total debt exceeds limit. Current: %s, New product: %s, Total: %s, Limit: %s",
                            totalDebt, newProductAmount, proposedTotalDebt, maxTotalLimit),
                    totalDebt, hasOverdue, overdueCount, clientName, activeProductsCount);
        }

        if (activeProductsCount > 0 && hasOverdue) {
            return new CreditDecision(false,
                    String.format("Client has %d overdue payment(s) in existing products", overdueCount),
                    totalDebt, true, overdueCount, clientName, activeProductsCount);
        }

        if (proposedTotalDebt.compareTo(maxTotalLimit) <= 0) {
            return new CreditDecision(true,
                    String.format("Credit approved. Total debt: %s, Limit: %s", proposedTotalDebt, maxTotalLimit),
                    proposedTotalDebt, false, overdueCount, clientName, activeProductsCount);
        }

        return new CreditDecision(false,
                "Credit application does not meet approval criteria",
                totalDebt, hasOverdue, overdueCount, clientName, activeProductsCount);
    }

    private BigDecimal getProductAmount(String productKey) {
        Map<String, BigDecimal> productAmounts = Map.of(
                "IPO", BigDecimal.valueOf(500000),
                "PC", BigDecimal.valueOf(200000),
                "AC", BigDecimal.valueOf(100000),
                "IPO1", BigDecimal.valueOf(500000),
                "PC1", BigDecimal.valueOf(200000),
                "AC1", BigDecimal.valueOf(100000)
        );
        return productAmounts.getOrDefault(productKey, BigDecimal.valueOf(100000));
    }

    private void createNewProduct(ClientProductMessage message, ClientInfo clientInfo) {
        ProductRegistry newProduct = new ProductRegistry();
        newProduct.setClientId(message.getClientId());
        newProduct.setProductId(message.getProductId());
        newProduct.setAccountId(generateAccountId());
        newProduct.setInterestRate(getInterestRate(message.getProductKey()));
        newProduct.setOpenDate(LocalDate.parse(message.getOpenDate()));
        newProduct.setMonthCount(12);

        ProductRegistry savedProduct = productRegistryRepository.save(newProduct);
        createInitialPayments(savedProduct);
    }

    private void createInitialPayments(ProductRegistry product) {
        LocalDate startDate = product.getOpenDate();
        BigDecimal productAmount = getProductAmount(product.getProductId());

        // Расчет аннуитетного платежа
        BigDecimal monthlyPayment = calculateAnnuityPayment(
                productAmount,
                product.getInterestRate(),
                product.getMonthCount()
        );

        BigDecimal remainingDebt = productAmount; // Остаток долга

        for (int i = 1; i <= product.getMonthCount(); i++) {
            PaymentRegistry payment = new PaymentRegistry();
            payment.setProductRegistry(product);

            LocalDate paymentDate = startDate.plusMonths(i);
            payment.setPaymentDate(paymentDate);
            payment.setPaymentExpirationDate(paymentDate.plusDays(10));
            payment.setAmount(monthlyPayment);

            // Расчет процентов за текущий месяц
            BigDecimal monthlyRate = product.getInterestRate()
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP) // Годовая ставка -> десятичная
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP); // Месячная ставка

            BigDecimal interestAmount = remainingDebt.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            // Расчет основного долга
            BigDecimal principalAmount = monthlyPayment.subtract(interestAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            payment.setInterestRateAmount(interestAmount);
            payment.setDebtAmount(principalAmount);
            payment.setExpired(false);

            paymentRegistryRepository.save(payment);

            // Обновление остатка долга
            remainingDebt = remainingDebt.subtract(principalAmount)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
    private BigDecimal calculateAnnuityPayment(BigDecimal loanAmount, BigDecimal annualRate, int months) {
        // Месячная процентная ставка (i)
        BigDecimal monthlyRate = annualRate
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP) // 8.5% -> 0.085
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP); // 0.085/12

        // (1 + i)^n
        BigDecimal base = BigDecimal.ONE.add(monthlyRate);
        BigDecimal powered = pow(base, months); // Возведение в степень

        // [i × (1 + i)^n] / [(1 + i)^n - 1]
        BigDecimal numerator = monthlyRate.multiply(powered);
        BigDecimal denominator = powered.subtract(BigDecimal.ONE);
        BigDecimal annuityCoefficient = numerator.divide(denominator, 10, RoundingMode.HALF_UP);

        // A = S × коэффициент
        return loanAmount.multiply(annuityCoefficient)
                .setScale(2, RoundingMode.HALF_UP);
    }
    private BigDecimal pow(BigDecimal base, int exponent) {
        BigDecimal result = BigDecimal.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(base);
        }
        return result;
    }
    private Long generateAccountId() {
        return System.currentTimeMillis() % 1000000000L;
    }

    private BigDecimal getInterestRate(String productKey) {
        Map<String, BigDecimal> interestRates = Map.of(
                "IPO", BigDecimal.valueOf(8.50),
                "PC", BigDecimal.valueOf(12.99),
                "AC", BigDecimal.valueOf(5.50),
                "IPO1", BigDecimal.valueOf(8.50),
                "PC1", BigDecimal.valueOf(12.99),
                "AC1", BigDecimal.valueOf(5.50)
        );
        return interestRates.getOrDefault(productKey, BigDecimal.valueOf(10.0));
    }

    private ClientInfo getClientInfoFromMs1ByClientId(String clientId) {
        try {
            String url = ms1ClientInfoUrl + "/" + clientId;
            // Получаем прокси через ApplicationContext
            ProductRegistryService proxy = applicationContext.getBean(ProductRegistryService.class);
            return proxy.makeHttpCallWithLogging(url, clientId);
        } catch (Exception e) {
            throw new RuntimeException("MS-1 service unavailable for client_id: " + clientId, e);
        }
    }
    @HttpOutcomeRequestLog
    public ClientInfo makeHttpCallWithLogging(String url, String clientId) {
        System.out.println("=== INSIDE makeHttpCallWithLogging ===");

        ResponseEntity<ClientInfo> response = restTemplate.getForEntity(url, ClientInfo.class);
        System.out.println("=== HTTP RESPONSE STATUS: " + response.getStatusCode() + " ===");

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to get client info from MS-1, status: " + response.getStatusCode());
        }
    }
}