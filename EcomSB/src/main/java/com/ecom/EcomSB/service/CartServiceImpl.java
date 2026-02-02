package com.ecom.EcomSB.service;

import com.ecom.EcomSB.exception.ResourceNotFoundException;
import com.ecom.EcomSB.exception.APIException;

import com.ecom.EcomSB.model.Cart;
import com.ecom.EcomSB.model.CartItem;
import com.ecom.EcomSB.model.Product;
import com.ecom.EcomSB.payload.CartDTO;
import com.ecom.EcomSB.payload.ProductDTO;
import com.ecom.EcomSB.repositories.CartItemRepository;
import com.ecom.EcomSB.repositories.CartRepository;
import com.ecom.EcomSB.repositories.ProductRepository;
import com.ecom.EcomSB.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import com.ecom.EcomSB.payload.CartItemRequest;
import com.ecom.EcomSB.payload.CartDTO;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class CartServiceImpl implements CartService{

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    ModelMapper modelMapper;

    @org.springframework.beans.factory.annotation.Value("${image.base.url}")
    private String imageBaseUrl;

    private String constructImageUrl(String imageName){
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl+ "/"+imageName;
    }

    @Override
    public CartDTO addProductToCart(@NonNull Long productId, Integer quantity) {
        Cart cart  = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }

        if (product.getQuantity() < 0) {
            product.setQuantity(0);
            productRepository.save(product);
        }

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        CartItem newCartItem = new CartItem();

        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItem);

        product.setQuantity(product.getQuantity());

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));

        // CRITICAL FIX: Add item to cart's list so orphanRemoval doesn't delete it
        cart.getCartItems().add(newCartItem);

        cartRepository.save(cart);

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO map = modelMapper.map(item.getProduct(), ProductDTO.class);
            map.setImage(constructImageUrl(item.getProduct().getImage()));
            map.setQuantity(item.getQuantity());
            return map;
        });

        cartDTO.setProducts(productStream.toList());

        logger.info("Product {} added to cart {}", productId, cart.getCartId());
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();


        if (carts.size() == 0) {
            throw new APIException("No cart exists");
        }

        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
            logger.info("Processing Cart ID: {}", cart.getCartId());

            List<ProductDTO> products = cart.getCartItems().stream()
                    .map(p -> {
                        ProductDTO dto = modelMapper.map(p.getProduct(), ProductDTO.class);
                        dto.setImage(constructImageUrl(p.getProduct().getImage()));
                        logger.info("Mapped Product: {}", dto.getProductName());
                        return dto;
                    }).collect(Collectors.toList());

            cartDTO.setProducts(products);
            logger.info("Cart ID: {} has {} products", cart.getCartId(), products.size());

            return cartDTO;

        }).collect(Collectors.toList());

        return cartDTOs;
    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
        if (cart == null){
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        cart.getCartItems().forEach(c ->
                c.getProduct().setQuantity(c.getQuantity()));
        List<ProductDTO> products = cart.getCartItems().stream()
                .map(p -> {
                    ProductDTO dto = modelMapper.map(p.getProduct(), ProductDTO.class);
                    dto.setImage(constructImageUrl(p.getProduct().getImage()));
                    return dto;
                })
                .toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }

    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(@NonNull Long productId, Integer quantity) {

        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId  = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + productId + " not available in the cart!!!");
        }

        // Auto-fix for existing negative quantities in DB
        if (cartItem.getQuantity() < 0) {
            logger.warn("Found negative quantity {} for Product {} in Cart {}. Resetting to 0.", cartItem.getQuantity(), productId, cartId);
            cartItem.setQuantity(0);
            cartItem = cartItemRepository.save(cartItem);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() < 0) {
            product.setQuantity(0);
            productRepository.save(product);
        }

        // Calculate new quantity
        int newQuantity = cartItem.getQuantity() + quantity;

        // Validation to prevent negative quantities
        if (newQuantity < 0) {
            throw new APIException("The resulting quantity cannot be negative.");
        }

        if (newQuantity == 0){
            deleteProductFromCart(cartId, productId);
            return modelMapper.map(cart, CartDTO.class);
        }

        if (product.getQuantity() < newQuantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        cartItem.setProductPrice(product.getSpecialPrice());
        cartItem.setQuantity(cartItem.getQuantity() + quantity);
        cartItem.setDiscount(product.getDiscount());
        cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
        cartRepository.save(cart);

        CartItem updatedItem = cartItemRepository.save(cartItem);
        if(updatedItem.getQuantity() == 0){
            cartItemRepository.deleteById(updatedItem.getCartItemId());
        }


        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO prd = modelMapper.map(item.getProduct(), ProductDTO.class);
            prd.setImage(constructImageUrl(item.getProduct().getImage()));
            prd.setQuantity(item.getQuantity());
            return prd;
        });


        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }


    private Cart createCart() {
        Cart userCart  = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if(userCart != null){
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        Cart newCart =  cartRepository.save(cart);

        return newCart;
    }


    @Transactional
    @Override
    public String deleteProductFromCart(@NonNull Long cartId, @NonNull Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() -
                (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);

        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart !!!";
    }


    @Override
    public void updateProductInCarts(@NonNull Long cartId, @NonNull Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        double cartPrice = cart.getTotalPrice()
                - (cartItem.getProductPrice() * cartItem.getQuantity());

        cartItem.setProductPrice(product.getSpecialPrice());

        cart.setTotalPrice(cartPrice
                + (cartItem.getProductPrice() * cartItem.getQuantity()));

        cartItem = cartItemRepository.save(cartItem);
    }

    @Transactional
    @Override
    public CartDTO createCartFromList(List<com.ecom.EcomSB.payload.CartItemRequest> cartItems) {
        Cart cart = createCart();
        Long cartId = cart.getCartId();

        for (com.ecom.EcomSB.payload.CartItemRequest itemRequest : cartItems) {
            Long productId = itemRequest.getProductId();
            Integer quantity = itemRequest.getQuantity();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

            CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

            if (cartItem == null) {
                // Not in cart, add it
                if (product.getQuantity() > 0) {
                     CartItem newCartItem = new CartItem();
                     newCartItem.setProduct(product);
                     newCartItem.setCart(cart);
                     newCartItem.setQuantity(quantity);
                     newCartItem.setDiscount(product.getDiscount());
                     newCartItem.setProductPrice(product.getSpecialPrice());
                     
                     cartItemRepository.save(newCartItem);
                     cart.getCartItems().add(newCartItem);
                     
                     cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
                }
            } else {
                // Already in cart, update quantity? Or leave it?
                // For sync, maybe we ensure at least this quantity or overwrite?
                // Frontend sends "sync", let's overwite/add logic if needed.
                // for now, let's just ignore if already exists to avoid duplication error,
                // or we could update quantity. Use safer approach: if exists, do nothing (assume backend is truth or already synced)
                // OR better: update quantity to match request (sync)
                // Let's safe update:
                 /*
                double oldPrice = cartItem.getProductPrice() * cartItem.getQuantity();
                cartItem.setQuantity(quantity);
                cartItem.setProductPrice(product.getSpecialPrice());
                cartItemRepository.save(cartItem);
                
                cart.setTotalPrice(cart.getTotalPrice() - oldPrice + (product.getSpecialPrice() * quantity));
                */
            }
        }
        
        cartRepository.save(cart);

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);
        List<ProductDTO> products = cart.getCartItems().stream()
                .map(p -> {
                    ProductDTO dto = modelMapper.map(p.getProduct(), ProductDTO.class);
                    dto.setImage(constructImageUrl(p.getProduct().getImage()));
                    dto.setQuantity(p.getQuantity()); // Ensure quantity is set in response
                    return dto;
                })
                .collect(Collectors.toList());
        cartDTO.setProducts(products);

        return cartDTO;
    }
}
