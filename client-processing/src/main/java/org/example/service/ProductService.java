package org.example.service;


import lombok.RequiredArgsConstructor;
import org.example.clientModels.entity.Product;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        // Проверяем уникальность productId
        if (productRepository.existsByProductId(product.getProductId())) {
            throw new RuntimeException("Product with productId '" + product.getProductId() + "' already exists");
        }
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Проверяем уникальность productId для других записей
        if (productRepository.existsByProductIdAndIdNot(productDetails.getProductId(), id)) {
            throw new RuntimeException("Product with productId '" + productDetails.getProductId() + "' already exists");
        }

        product.setName(productDetails.getName());
        product.setKey(productDetails.getKey());
        product.setCreateDate(productDetails.getCreateDate());
        product.setProductId(productDetails.getProductId());

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}