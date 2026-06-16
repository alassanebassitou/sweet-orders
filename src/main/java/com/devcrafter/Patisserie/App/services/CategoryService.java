package com.devcrafter.Patisserie.App.services;

import com.devcrafter.Patisserie.App.Exceptions.ResourceNotFoundException;
import com.devcrafter.Patisserie.App.dto.request.CategoryRequest;
import com.devcrafter.Patisserie.App.dto.response.CategoryResponse;
import com.devcrafter.Patisserie.App.models.Category;
import com.devcrafter.Patisserie.App.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static com.devcrafter.Patisserie.App.utils.AppConstants.CAT_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public CategoryResponse createdCategory(CategoryRequest request) {
        Category category = new Category();

        category.setName(request.getName().toUpperCase());
        category.setDescription(request.getDescription());

        if (request.getPhotoUrl() != null && !request.getPhotoUrl().isBlank()) {
            category.setPhotoUrl(request.getPhotoUrl());
        }
        category = categoryRepository.save(category);

        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(CAT_NOT_FOUND));
    }

    @Transactional
    public CategoryResponse modifyCategory(Long catId, CategoryRequest request) {
        Category category = this.get(catId);

        if (request.getName() != null && !request.getName().isBlank()) {
            category.setName(request.getName());
        }

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            category.setDescription(request.getDescription());
        }

        if (request.getPhotoUrl() != null && !request.getPhotoUrl().isBlank()) {
            category.setPhotoUrl(request.getPhotoUrl());
        }

        category = categoryRepository.save(category);

        return CategoryResponse.from(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CAT_NOT_FOUND));
    }

    /**
     * Upload or replace category image.
     */
    public CategoryResponse uploadPhoto(Long categoryId, MultipartFile file)
            throws IOException {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        CAT_NOT_FOUND + ": " + categoryId
                ));

        String newUrl = cloudinaryService.replaceImage(
                file,
                category.getPhotoUrl(),
                cloudinaryService.getCategoriesFolder()
        );

        category.setPhotoUrl(newUrl);
        return CategoryResponse.from(
                categoryRepository.save(category)
        );
    }

    private Category get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CAT_NOT_FOUND));
    }
}
