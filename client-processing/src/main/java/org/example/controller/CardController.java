package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.CardCreateDto;
import org.example.service.CardCreateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final CardCreateService cardCreateService;

    @PostMapping("/createcard")
    public ResponseEntity<CardCreateDto> createClientProduct(@RequestBody CardCreateDto cardCreateDto) {
        try {
            CardCreateDto createdCard = cardCreateService.createCard(cardCreateDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCard);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
