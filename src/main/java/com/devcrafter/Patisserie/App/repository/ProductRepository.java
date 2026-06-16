package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.Category;
import com.devcrafter.Patisserie.App.models.Products;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Products, Long> {
    List<Products> findByIsActifTrue();
    List<Products> findByIsActifTrueAndCategory(Category category);
}
