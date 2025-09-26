package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.clientModels.entity.ClientProduct;
import org.example.dto.CardCreateDto;
import org.example.service.CardCreateService;
import org.example.service.ClientProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/client-products")
@RequiredArgsConstructor
public class ClientProductController {

    private final ClientProductService clientProductService;
    @GetMapping
    public ResponseEntity<List<ClientProduct>> getAllClientProducts() {
        List<ClientProduct> clientProducts = clientProductService.getAllClientProducts();
        return ResponseEntity.ok(clientProducts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientProduct> getClientProductById(@PathVariable Long id) {
        Optional<ClientProduct> clientProduct = clientProductService.getClientProductById(id);
        return clientProduct.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClientProduct> createClientProduct(@RequestBody ClientProduct clientProduct) {
        try {
            ClientProduct createdClientProduct = clientProductService.createClientProduct(clientProduct);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdClientProduct);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientProduct> updateClientProduct(@PathVariable Long id, @RequestBody ClientProduct clientProduct) {
        try {
            ClientProduct updatedClientProduct = clientProductService.updateClientProduct(id, clientProduct);
            return ResponseEntity.ok(updatedClientProduct);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClientProduct(@PathVariable Long id) {
        try {
            clientProductService.deleteClientProduct(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}