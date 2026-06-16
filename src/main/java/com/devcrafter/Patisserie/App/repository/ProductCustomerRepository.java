package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.ProductCustomization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCustomerRepository extends JpaRepository<ProductCustomization, Long> {
    List<ProductCustomization> findByProductsId(Long produitId);
    void deleteByProductsId(Long productId);
}
