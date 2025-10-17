import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.JwtService;
import org.example.controller.CardController;
import org.example.dto.CardCreateDto;
import org.example.service.CardCreateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CardController.class)
class CardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardCreateService cardCreateService;

    @MockBean
    private JwtService jwtService;

    @Test
    void createCard_WithValidData_ShouldReturnCreated() throws Exception {
        // Arrange
        CardCreateDto request = new CardCreateDto();
        request.setAccountId(123L);
        request.setCardId("CARD123");
        request.setPaymentSystem("VISA");

        when(cardCreateService.createCard(any(CardCreateDto.class))).thenReturn(request);

        // Act & Assert
        mockMvc.perform(post("/api/cards/createcard")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardId").value("CARD123"));
    }

    @Test
    void createCard_WithServiceError_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CardCreateDto request = new CardCreateDto();
        request.setAccountId(123L);
        request.setCardId("CARD123");
        request.setPaymentSystem("VISA");

        when(cardCreateService.createCard(any(CardCreateDto.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        mockMvc.perform(post("/api/cards/createcard")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_WithoutAuthHeader_ShouldWork() throws Exception {
        // Arrange
        CardCreateDto request = new CardCreateDto();
        request.setAccountId(123L);
        request.setCardId("CARD123");
        request.setPaymentSystem("VISA");

        when(cardCreateService.createCard(any(CardCreateDto.class))).thenReturn(request);

        // Act & Assert
        mockMvc.perform(post("/api/cards/createcard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}