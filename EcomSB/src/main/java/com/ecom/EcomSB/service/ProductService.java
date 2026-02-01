package com.ecom.EcomSB.service;

import com.ecom.EcomSB.payload.ProductDTO;
import com.ecom.EcomSB.payload.ProductResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.lang.NonNull;

import java.io.IOException;

public interface ProductService {
    // Add Product
    ProductDTO addProduct(@NonNull Long categoryId, ProductDTO product);

    // Get All the Products
    ProductResponse getAllProducts(String keyword, String category, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
    // Get the Product By Category
    ProductResponse searchByCategory(@NonNull Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    ProductDTO updateProduct(@NonNull Long productId, ProductDTO productDTO);

    ProductDTO deletedProduct(@NonNull Long productId);

    ProductDTO updateProductImage(@NonNull Long productId, @NonNull MultipartFile image) throws IOException;

}
