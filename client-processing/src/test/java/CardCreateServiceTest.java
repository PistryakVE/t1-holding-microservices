import org.example.dto.CardCreateDto;
import org.example.service.CardCreateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardCreateServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CardCreateService cardCreateService;

    @Test
    void createCard_WithValidData_ShouldSendToKafkaAndReturnDto() throws Exception {
        // Arrange
        CardCreateDto request = new CardCreateDto();
        request.setAccountId(123L);
        request.setCardId("CARD123");
        request.setPaymentSystem("VISA");

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(kafkaTemplate.send("client_cards", "CARD123", request)).thenReturn(future);

        // Act
        CardCreateDto result = cardCreateService.createCard(request);

        // Assert
        assertEquals(request, result);
        verify(kafkaTemplate).send("client_cards", "CARD123", request);
    }

    @Test
    void createCard_WithMissingRequiredFields_ShouldThrowException() {
        // Arrange
        CardCreateDto request = new CardCreateDto();
        request.setAccountId(null); // missing accountId

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cardCreateService.createCard(request));

        assertEquals("Account ID is required", exception.getMessage());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void createCard_WithKafkaTimeout_ShouldThrowTimeoutException() throws Exception {
        // Arrange
        CardCreateDto request = new CardCreateDto();
        request.setAccountId(123L);
        request.setCardId("CARD123");
        request.setPaymentSystem("VISA");

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send("client_cards", "CARD123", request)).thenReturn(future);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> cardCreateService.createCard(request));

        assertEquals("Kafka timeout - please try again later", exception.getMessage());
    }
}