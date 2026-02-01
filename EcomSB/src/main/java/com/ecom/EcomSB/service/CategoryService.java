package com.ecom.EcomSB.service;

import com.ecom.EcomSB.payload.CategoryDTO;
import org.springframework.lang.NonNull;
import com.ecom.EcomSB.payload.CategoryResponse;

public interface CategoryService {
    CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    CategoryDTO createCategory(CategoryDTO categoryDTO);

    CategoryDTO deleteCategory(@NonNull Long categoryId);

    CategoryDTO updateCategory(CategoryDTO categoryDTO, @NonNull Long categoryId);

}
