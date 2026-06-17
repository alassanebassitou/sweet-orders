package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.BadRequestException;
import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.request.ProductCustomizationRequest;
import com.devcrafter.Patisserie.App.dto.request.ProductRequest;
import com.devcrafter.Patisserie.App.dto.response.ProductCustomizationResponse;
import com.devcrafter.Patisserie.App.dto.response.ProductResponse;
import com.devcrafter.Patisserie.App.models.Category;
import com.devcrafter.Patisserie.App.models.Products;
import com.devcrafter.Patisserie.App.models.ProductCustomization;
import com.devcrafter.Patisserie.App.repository.CategoryRepository;
import com.devcrafter.Patisserie.App.repository.ProductCustomerRepository;
import com.devcrafter.Patisserie.App.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.devcrafter.Patisserie.App.utils.AppConstants.PRODUCT_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCustomerRepository productCustomerRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    @Cacheable(cacheNames = "products", key = "'tous'")
    public List<ProductResponse> getAllProducts() {
        log.info("Loading products from PostgreSQL");
        return productRepository.findByIsActifTrue()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Cacheable(cacheNames = "products", key = "#id")
    public ProductResponse getProduct(Long id) {
        Products products = findProductOrThrow(id);
        return ProductResponse.from(products);
    }

    public List<ProductCustomizationResponse> getCustomization(Long produitId) {
        findProductOrThrow(produitId);
        return productCustomerRepository.findByProductsId(produitId)
                .stream()
                .map(ProductCustomizationResponse::from)
                .toList();
    }
    // ─── Admin endpoints ─────────────────────────────────────────────

    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        Products products = new Products();

        Category category = this.getCategory(request.getCategory());

        products.setName(request.getName());
        products.setDescription(request.getDescription());
        products.setBasePrice(request.getBasePrice());
        products.setCategory(category);
        products.setIsActif(true);

        Products saved = productRepository.save(products);
        log.info("Product created: {}", saved.getName());
        return ProductResponse.from(saved);
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse modifierProduit(Long id, ProductRequest request) {
        Products products = findProductOrThrow(id);


        //final String catName = request.getCategory();
        Category category = this.getCategory(request.getCategory());

        if (request.getName() != null)
            products.setName(request.getName());
        if (request.getDescription() != null)
            products.setDescription(request.getDescription());
        if (request.getBasePrice() != null)
            products.setBasePrice(request.getBasePrice());
        if (request.getCategory() != null)
            products.setCategory(category);

        return ProductResponse.from(productRepository.save(products));
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public void deactivatedProduct(Long id) {
        Products products = findProductOrThrow(id);
        products.setIsActif(false);
        productRepository.save(products);
        log.info("Product deactivated: {}", id);
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse uploadPhoto(Long id, MultipartFile file)
            throws IOException {

        Products products = findProductOrThrow(id);

        // Save file locally
        String filename  = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadDir   = Paths.get("uploads/produits");
        Files.createDirectories(uploadDir);
        Files.copy(file.getInputStream(),
                uploadDir.resolve(filename),
                StandardCopyOption.REPLACE_EXISTING);

        products.setPhotoUrl("/uploads/produits/" + filename);
        return ProductResponse.from(productRepository.save(products));
    }

    public ProductCustomizationResponse addCustomization(
            Long produitId,
            ProductCustomizationRequest request) {

        Products products = findProductOrThrow(produitId);

        ProductCustomization p = new ProductCustomization();
        p.setProducts(products);
        p.setLibelle(request.getLibelle());
        p.setAdditionalPrice(
                request.getAdditionalPrice() != null
                        ? request.getAdditionalPrice()
                        : java.math.BigDecimal.ZERO
        );
        p.setIsRequire(
                request.getIsRequire() != null
                        ? request.getIsRequire()
                        : false
        );

        return ProductCustomizationResponse.from(
                productCustomerRepository.save(p)
        );
    }

    public ProductCustomizationResponse updateCustomization(Long id , ProductCustomizationRequest request) {
        ProductCustomization c = productCustomerRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customization", id)
                );

        if (request.getLibelle() != null)
            c.setLibelle(request.getLibelle());
        if (request.getAdditionalPrice() != null)
            c.setAdditionalPrice(request.getAdditionalPrice());
        if (request.getIsRequire() != null)
            c.setIsRequire(request.getIsRequire());
        productCustomerRepository.save(c);

        return buildCustomization(c);
    }

    public void deleteCustomization(Long id) {
        productCustomerRepository.deleteById(id);
    }

    // ─── Image upload methods ─────────────────────────────────

    /**
     * Upload or replace main product photo.
     * Old photo is automatically deleted from Cloudinary.
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse uploadMainPhoto(Long productId, MultipartFile file) throws IOException {

        Products product = findProductOrThrow(productId);

        String newUrl = cloudinaryService.replaceImage(
                file,
                product.getPhotoUrl(),
                cloudinaryService.getProductsFolder()
        );

        product.setPhotoUrl(newUrl);
        return ProductResponse.from(
                productRepository.save(product)
        );
    }

    /**
     * Add an additional image to a product.
     * A product can have multiple images.
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse addAdditionalPhoto(Long productId, MultipartFile file)
            throws IOException {

        Products product = findProductOrThrow(productId);

        String newUrl = cloudinaryService.uploadImage(
                file,
                cloudinaryService.getProductsFolder()
        );

        List<String> photos = product.getAdditionalPhotos();
        if (photos == null) photos = new ArrayList<>();
        photos.add(newUrl);
        product.setAdditionalPhotos(photos);

        return ProductResponse.from(
                productRepository.save(product)
        );
    }

    /**
     * Delete a specific additional image.
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse deleteAdditionalPhoto(Long productId, String photoUrl) {

        Products product = findProductOrThrow(productId);

        // Remove from list
        List<String> photos = product.getAdditionalPhotos();
        if (photos != null) {
            photos.remove(photoUrl);
            product.setAdditionalPhotos(photos);
        }

        // Delete from Cloudinary
        cloudinaryService.deleteImage(photoUrl);

        return ProductResponse.from(
                productRepository.save(product)
        );
    }

    // ─── Helper ──────────────────────────────────────────────────────

    private Products findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT_NOT_FOUND, id));
    }

    private Category getCategory(Long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new BadRequestException("Category is null"));
    }

    private ProductCustomizationResponse buildCustomization(ProductCustomization c) {
        return ProductCustomizationResponse.from(c);
    }
}
