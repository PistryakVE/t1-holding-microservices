package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.clientModels.entity.Client;
import org.example.clientModels.entity.ClientProduct;
import org.example.clientModels.entity.Product;
import org.example.clientModels.enums.ProductKey;
import org.example.dto.ClientProductMessage;
import org.example.repository.ClientProductRepository;
import org.example.repository.ClientRepository;
import org.example.repository.ProductRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class ClientProductService {

    private final ClientProductRepository clientProductRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate; // Правильный тип
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;

    public List<ClientProduct> getAllClientProducts() {
        return clientProductRepository.findAll();
    }

    public Optional<ClientProduct> getClientProductById(Long id) {
        return clientProductRepository.findById(id);
    }

    public List<ClientProduct> getClientProductsByClientId(Long clientId) {
        return clientProductRepository.findByClientId(clientId);
    }

    public List<ClientProduct> getClientProductsByProductId(Long productId) {
        return clientProductRepository.findByProductId(productId);
    }

    public ClientProduct createClientProduct(ClientProduct clientProduct) {
        if (clientProduct.getClient() == null || clientProduct.getClient().getId() == null) {
            throw new RuntimeException("Client is required");
        }
        if (clientProduct.getProduct() == null || clientProduct.getProduct().getId() == null) {
            throw new RuntimeException("Product is required");
        }

        // Загружаем полные сущности из базы
        Client client = clientRepository.findById(clientProduct.getClient().getId())
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientProduct.getClient().getId()));

        Product product = productRepository.findById(clientProduct.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + clientProduct.getProduct().getId()));

        // Устанавливаем загруженные сущности
        clientProduct.setClient(client);
        clientProduct.setProduct(product);

        System.out.println("Starting Kafka transaction...");

        // 1. Сначала отправляем в Kafka и ждем подтверждения
        try {
            CompletableFuture<SendResult<String, Object>> future = sendToKafka(clientProduct);

            // Ждем подтверждения от Kafka (таймаут 10 секунд)
            SendResult<String, Object> result = future.get(10, TimeUnit.SECONDS);

            System.out.println("Kafka confirmation received: " + result.getRecordMetadata());

            // 2. Только после подтверждения от Kafka сохраняем в базу
            ClientProduct savedClientProduct = clientProductRepository.save(clientProduct);
            System.out.println("Entity saved to database: " + savedClientProduct.getId());

            return savedClientProduct;

        } catch (TimeoutException e) {
            System.err.println("Kafka timeout - message not delivered within 10 seconds");
            throw new RuntimeException("Kafka timeout - please try again later");
        } catch (Exception e) {
            System.err.println("Kafka error: " + e.getMessage());
            throw new RuntimeException("Message delivery failed: " + e.getMessage());
        }
    }

    private CompletableFuture<SendResult<String, Object>> sendToKafka(ClientProduct clientProduct) {
        try {
            Product product = clientProduct.getProduct();
            ProductKey productKey = product.getKey();

            String topic;
            if (productKey == ProductKey.DC || productKey == ProductKey.CC ||
                    productKey == ProductKey.NS || productKey == ProductKey.PENS) {
                topic = "client_products";
            } else {
                topic = "client_credit_products";
            }

            System.out.println("Selected topic: " + topic);

            // Создаем DTO объект для отправки
            ClientProductMessage message = new ClientProductMessage();
            message.setClientProductId("PENDING");
            message.setClientId(clientProduct.getClient().getClientId());
            message.setProductId(clientProduct.getProduct().getProductId());
            message.setProductKey(productKey.toString());
            message.setOpenDate(clientProduct.getOpenDate().toString());
            message.setStatus(clientProduct.getStatus().toString());

            if (clientProduct.getCloseDate() != null) {
                message.setCloseDate(clientProduct.getCloseDate().toString());
            }

            System.out.println("Sending message to Kafka topic: " + topic);
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, "pending", message);

            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    System.err.println("Kafka send failed: " + exception.getMessage());
                } else {
                    System.out.println("Kafka send successful: " + result.getRecordMetadata());
                }
            });

            return future;

        } catch (Exception e) {
            System.err.println("Error preparing Kafka message: " + e.getMessage());
            CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(e);
            return failedFuture;
        }
    }

    public ClientProduct updateClientProduct(Long id, ClientProduct clientProductDetails) {
        ClientProduct clientProduct = clientProductRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ClientProduct not found with id: " + id));

        clientProduct.setOpenDate(clientProductDetails.getOpenDate());
        clientProduct.setCloseDate(clientProductDetails.getCloseDate());
        clientProduct.setStatus(clientProductDetails.getStatus());

        if (clientProductDetails.getClient() != null && clientProductDetails.getClient().getId() != null) {
            Client client = clientRepository.findById(clientProductDetails.getClient().getId())
                    .orElseThrow(() -> new RuntimeException("Client not found with id: " + clientProductDetails.getClient().getId()));
            clientProduct.setClient(client);
        }
        if (clientProductDetails.getProduct() != null && clientProductDetails.getProduct().getId() != null) {
            Product product = productRepository.findById(clientProductDetails.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + clientProductDetails.getProduct().getId()));
            clientProduct.setProduct(product);
        }

        return clientProductRepository.save(clientProduct);
    }

    public void deleteClientProduct(Long id) {
        if (!clientProductRepository.existsById(id)) {
            throw new RuntimeException("ClientProduct not found with id: " + id);
        }
        clientProductRepository.deleteById(id);
    }
}