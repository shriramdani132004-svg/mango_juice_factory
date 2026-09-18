package com.smartfactory.service;

import com.smartfactory.dto.ProductDto;
import com.smartfactory.entity.Product;
import com.smartfactory.exception.ResourceNotFoundException;
import com.smartfactory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findByIsActiveTrue().stream()
            .map(this::toDto)
            .toList();
    }

    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
        return toDto(product);
    }

    public ProductDto getProductByCode(String code) {
        Product product = productRepository.findByProductCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + code));
        return toDto(product);
    }

    private ProductDto toDto(Product p) {
        return new ProductDto(
            p.getId(),
            p.getName(),
            p.getProductCode(),
            p.getDescription(),
            p.getBottleSizeMl(),
            p.getIsActive()
        );
    }
}
