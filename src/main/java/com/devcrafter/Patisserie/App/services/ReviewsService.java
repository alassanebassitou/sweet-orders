package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.BusinessException;
import com.devcrafter.Patisserie.App.Exceptions.ConflictException;
import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.response.ReviewsStatsResponse;
import com.devcrafter.Patisserie.App.dto.request.ReviewsRequest;
import com.devcrafter.Patisserie.App.dto.response.ReviewsResponse;
import com.devcrafter.Patisserie.App.models.*;
import com.devcrafter.Patisserie.App.repository.CommandeRepository;
import com.devcrafter.Patisserie.App.repository.ProductRepository;
import com.devcrafter.Patisserie.App.repository.ReviewsRepository;
import com.devcrafter.Patisserie.App.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewsService {

    private final ReviewsRepository reviewsRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CommandeRepository commandeRepository;

    /**
     * Client submits a review.
     * Rules:
     * 1. Client must have a LIVREE order containing this product
     * 2. One review per client per product
     */
    public ReviewsResponse makeReview(
            ReviewsRequest request,
            SessionUser currentUser) {

        Long clientId = currentUser.getUserId();

        log.info("ClientId: {} and productId: {}", clientId, request.getProductId());

        // Rule 1 — client must have bought the product
        boolean aAchete = reviewsRepository.clientBuyProduct(clientId, request.getProductId());
        log.info("Is bought it: {}", aAchete);
        if (!aAchete) {
            throw new BusinessException("NOT_PURCHASED",
                    "Vous ne pouvez noter que les produits " +
                            "que vous avez achetés et reçus."
            );
        }

        // Rule 2 — one review per product
        boolean dejaNote = reviewsRepository
                .existsByClientIdAndProductId(clientId, request.getProductId());
        if (dejaNote) {
            throw new ConflictException(
                    "Vous avez déjà laissé un reviews " +
                            "pour ce products."
            );
        }

        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable"));

        Products products = productRepository
                .findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        Commande commande = commandeRepository
                .findById(request.getCommandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));

        Reviews reviews = new Reviews();
        reviews.setClient(client);
        reviews.setProduct(products);
        reviews.setCommande(commande);
        reviews.setNote(request.getNote());
        reviews.setComment(request.getComment());
        reviews.setIsVisible(true);

        Reviews saved = reviewsRepository.save(reviews);
        log.info("Review {} stars for product {} by {}",
                request.getNote(),
                products.getName(),
                currentUser.getEmail()
        );

        return ReviewsResponse.from(saved);
    }

    /**
     * Get all reviews + stats for a product.
     */
    public ReviewsStatsResponse getProductReviews(Long produitId) {

        List<ReviewsResponse> avis = reviewsRepository
                .findByProductIdAndIsVisibleTrue(produitId)
                .stream()
                .map(ReviewsResponse::from)
                .toList();

        Double moyenne = reviewsRepository
                .moyenneNoteByProduct(produitId);

        Long total = reviewsRepository
                .countByProductId(produitId);

        // Distribution: how many 5-star, 4-star, etc.
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            final int star = i;
            long count = avis.stream()
                    .filter(a -> a.getNote() == star)
                    .count();
            distribution.put(star, count);
        }

        return new ReviewsStatsResponse(
                Math.round(moyenne * 10.0) / 10.0,
                total,
                distribution,
                avis
        );
    }

    /**
     * Get reviews left by the current client.
     */
    public List<ReviewsResponse> getMyReviews(
            SessionUser currentUser) {
        return reviewsRepository
                .findByClientIdOrderByCreatedAtDesc(currentUser.getUserId())
                .stream()
                .map(ReviewsResponse::from)
                .toList();
    }

    /**
     * Admin — hide an inappropriate review.
     */
    public void hiddeReview(Long id) {
        Reviews reviews = reviewsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));
        reviews.setIsVisible(false);
        reviewsRepository.save(reviews);
        log.info("Review {} hidden by admin", id);
    }

    /**
     * Admin — restore a hidden review.
     */
    public void restaureReviews(Long id) {
        Reviews avis = reviewsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));
        avis.setIsVisible(true);
        reviewsRepository.save(avis);
    }

    /**
     * Admin — delete a review permanently.
     */
    public void removeReviews(Long id) {
        reviewsRepository.deleteById(id);
        log.info("Review {} deleted by admin", id);
    }

    /**
     * Admin — all reviews.
     */
    public List<ReviewsResponse> getAllReviews() {
        return reviewsRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReviewsResponse::from)
                .toList();
    }
}
