package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.config.JwtService;
import org.example.dto.CardCreateDto;
import org.example.service.CardCreateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardCreateService cardCreateService;
    private final JwtService jwtService;

    @PostMapping("/createcard")
    public ResponseEntity<CardCreateDto> createClientProduct(@RequestBody CardCreateDto cardCreateDto,
                                                             @RequestHeader("Authorization") String token) {

        // Извлекаем роль пользователя из токена
        String userRole = jwtService.extractRole(token);

        // Проверяем, имеет ли пользователь право на создание продукта
        if (!hasCreatePermission(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            CardCreateDto createdCard = cardCreateService.createCard(cardCreateDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
