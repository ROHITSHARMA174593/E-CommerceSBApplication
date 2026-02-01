package com.ecom.EcomSB.service;

import com.ecom.EcomSB.payload.CartDTO;
import org.springframework.lang.NonNull;

import jakarta.transaction.Transactional;

import java.util.List;

public interface CartService {
    CartDTO addProductToCart(@NonNull Long productId, Integer quantity);

    List<CartDTO> getAllCarts();

    CartDTO getCart(String emailId, Long cartId);

    @Transactional
    CartDTO updateProductQuantityInCart(@NonNull Long productId, Integer quantity);

    String deleteProductFromCart(@NonNull Long cartId, @NonNull Long productId);

    void updateProductInCarts(@NonNull Long cartId, @NonNull Long productId);
}
