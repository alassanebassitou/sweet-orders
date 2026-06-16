package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.OrderedProducts;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderedProductsRepository extends JpaRepository<OrderedProducts, Long> {

    // In OrderedProductRepository (or CommandeRepository)
    @Query("SELECT op.products.name, " +
            "COUNT(op.commande.id), " +
            "SUM(op.totalPrice) " +
            "FROM OrderedProducts op " +
            "WHERE op.commande.status != 'CANCELLED' " +
            "GROUP BY op.products.name " +
            "ORDER BY COUNT(op.commande.id) DESC")
    List<Object[]> findTopProducts(Pageable pageable);
}
