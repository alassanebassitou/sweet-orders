package com.devcrafter.Patisserie.App.repository;

import com.devcrafter.Patisserie.App.models.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewsRepository extends JpaRepository<Reviews, Long> {

    List<Reviews> findByProductIdAndIsVisibleTrue(Long produitId);

    // Check if client already reviewed this product
    boolean existsByClientIdAndProductId(Long clientId, Long produitId);

    // Average rating for a product
    @Query("SELECT COALESCE(AVG(r.note), 0) " +
            "FROM Reviews r " +
            "WHERE r.product.id = :productId " +
            "AND r.isVisible = true")
    Double moyenneNoteByProduct(@Param("productId") Long productId);

    // Count reviews for a product
    @Query("SELECT COUNT(r) FROM Reviews r " +
            "WHERE r.product.id = :productId " +
            "AND r.isVisible = true")
    Long countByProductId(@Param("productId") Long productId);

    // All reviews by a client
    List<Reviews> findByClientIdOrderByCreatedAtDesc(Long clientId);

    // All reviews — admin view
    List<Reviews> findAllByOrderByCreatedAtDesc();

    // Check client bought the product
    @Query("SELECT COUNT(op) > 0 FROM OrderedProducts op " +
            "WHERE op.commande.client.id = :clientId " +
            "AND op.products.id = :productId " +
            "AND op.commande.status = 'DELIVERED'")
    boolean clientBuyProduct(
            @Param("clientId") Long clientId,
            @Param("productId") Long productId
    );
}
