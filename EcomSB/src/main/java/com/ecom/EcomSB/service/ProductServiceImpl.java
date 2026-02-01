package com.ecom.EcomSB.service;


import com.ecom.EcomSB.exception.APIException;
import com.ecom.EcomSB.exception.ResourceNotFoundException;
import com.ecom.EcomSB.model.Cart;
import com.ecom.EcomSB.model.Category;
import com.ecom.EcomSB.model.Product;

import com.ecom.EcomSB.payload.ProductDTO;
import com.ecom.EcomSB.payload.ProductResponse;
import com.ecom.EcomSB.repositories.CartRepository;
import com.ecom.EcomSB.repositories.CategoryRepository;
import com.ecom.EcomSB.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import org.springframework.lang.NonNull;

@Service
public class ProductServiceImpl implements ProductService{

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}") // use in file path
    private String path;

    @Value("${image.base.url}")
    private String imageBaseUrl;

// todo ::: ----------------------------------- POST METHOD START -----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ProductDTO addProduct(@NonNull Long categoryId, ProductDTO productDTO) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "CategoryId", categoryId));

        boolean ifProductNotPresent = true;
        List<Product> products = category.getProducts();
        for (Product value : products) {
            if (value.getProductName().equals(productDTO.getProductName())) {
                ifProductNotPresent = false;
                break;
            }
        }

        if(ifProductNotPresent){
            Product product = modelMapper.map(productDTO, Product.class);
            product.setCategory(category);
            product.setImage("default.png");
            double specialPrice = product.getPrice() - ((product.getDiscount() * 0.01)*product.getPrice());
            product.setSpecialPrice(specialPrice);
            Product savedProduct = productRepository.save(product);
            ProductDTO responseDTO = modelMapper.map(savedProduct, ProductDTO.class);
            responseDTO.setImage(constructImageUrl(savedProduct.getImage()));
            return responseDTO;
        }else {
            throw new APIException(" !!! Product is Alerady Exist !!! ");
        }

    }
// todo ::: --------------------------------- POST METHOD END --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


// todo ::: --------------------------------- GET METHOD START --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ProductResponse getAllProducts(String keyword, String category, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetail = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Specification<Product> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
        if(keyword != null && !keyword.isEmpty()){
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%"+keyword.toLowerCase()+"%"));
        }

        if(category != null && !category.isEmpty()){
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category));
        }


        Page<Product> pageProducts = productRepository.findAll(specification, pageDetail); // we add JpaSpecificationExecuter in Repository of this model(ProductRepository)

        List<Product> products = pageProducts.getContent();
        List<ProductDTO> productDTOS = products.stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                })
                .toList();

        // if there is no products
        if(products.isEmpty()){
            throw new APIException(" !!! No Products Exist !!! ");
        }

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());
        return productResponse;
    }

    private String constructImageUrl(String imageName){
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl+ "/"+imageName;
    }

    @Override
    public ProductResponse searchByCategory(@NonNull Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder){
        // First::  Get the Category on this particular ID
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "CategoryID", categoryId));

        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetail = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProducts = productRepository.findByCategoryOrderByPriceAsc(category, pageDetail); // This is SQL Query automatically written by JPA

        List<Product> products = pageProducts.getContent();
        if(products.isEmpty()){
            throw new APIException(category.getCategoryName() +" Category does not have any Products");
        }


        List<ProductDTO> productDTOS = products.stream()
                .map(pp -> {
                    ProductDTO productDTO = modelMapper.map(pp, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(pp.getImage()));
                    return productDTO;
                })
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());

        return productResponse;
    }

    @Override
    public ProductResponse searchProductByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) { // keyword === productName
        Sort sortByAndOrder = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        Pageable pageDetail = PageRequest.of(pageNumber, pageSize, sortByAndOrder);
        Page<Product> pageProducts = productRepository.findByProductNameLikeIgnoreCase('%'+keyword+'%', pageDetail);; // % because here JPA use the Like keyword of SQL and this keyword needs these percentage signs for finding anything in a string.(and this query is also written by JPA)

        List<Product> products = pageProducts.getContent();
        List<ProductDTO> productDTOS = products.stream()
                .map(pp -> {
                    ProductDTO productDTO = modelMapper.map(pp, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(pp.getImage()));
                    return productDTO;
                })
                .toList();

        if(products.isEmpty()){
            throw new APIException("Product NOT found with Keyword : "+keyword);
        }

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOS);
        productResponse.setPageNumber(pageProducts.getNumber());
        productResponse.setPageSize(pageProducts.getSize());
        productResponse.setTotalElements(pageProducts.getTotalElements());
        productResponse.setTotalPages(pageProducts.getTotalPages());
        productResponse.setLastPage(pageProducts.isLast());

        return productResponse;
    }
// todo ::: --------------------------------------- GET METHOD END-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

// todo ::: -------------------------------------- PUT METHOD START--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ProductDTO updateProduct(@NonNull Long productId, ProductDTO productDTO){
        //1 : Get the products from DB
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productID", productId));

        //2: Update the Product info with user shared body
        Product product = modelMapper.map(productDTO, Product.class);
        productFromDB.setProductName(product.getProductName());
        productFromDB.setDescription(product.getDescription());
        productFromDB.setQuantity(product.getQuantity());
        productFromDB.setDiscount(product.getDiscount());
        productFromDB.setPrice(product.getPrice());
        productFromDB.setSpecialPrice(product.getSpecialPrice());

        //3: Save to the DB
        Product savedProduct = productRepository.save(productFromDB);

        ProductDTO responseDTO = modelMapper.map(savedProduct, ProductDTO.class);
        responseDTO.setImage(constructImageUrl(savedProduct.getImage()));
        return responseDTO;
    }
// todo ::: ---------------------------------PUT METHOD END-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


// todo ::: ------------------------------- DELETE METHOD START---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ProductDTO deletedProduct(@NonNull Long productId){
        //1: Get from DB(by ID)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        carts.forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));

        //todo: Yaha per hum direct ye neeche wali line kar bhi kaam chala sakte hai lekin agar in case ID nahi mili DB me to Error Handling bhi zaroori hai for direct deletion we have a inbuilt method :: deleteById()
        productRepository.delete(product);

        ProductDTO responseDTO = modelMapper.map(product, ProductDTO.class);
        responseDTO.setImage(constructImageUrl(product.getImage()));
        return responseDTO;

    }
// todo ::: -------------------------------- DELETE METHOD END --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

// todo ::: -------------------------------- PUT METHOD START --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public ProductDTO updateProductImage(@NonNull Long productId, @NonNull MultipartFile image) throws IOException {
        // Get Product from DB
        Product productFromDB = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productID", productId));

        // Upload the Image to server
        // Get the file name of uploaded image
//        String path = "images/"; // isko hum application.properties file me hi add kar denge
        String fileName = fileService.uploadImage(path, image); // this method implementation is after this method ending

        // Updating new file name to the product
        productFromDB.setImage(fileName);

        // save the updated product
        Product updatedProduct = productRepository.save(productFromDB);

        // return DTO after Mapping product to DTO
        ProductDTO responseDTO = modelMapper.map(updatedProduct, ProductDTO.class);
        responseDTO.setImage(constructImageUrl(updatedProduct.getImage()));
        return responseDTO;
    }

// todo ::: -------------------------------- PUT METHOD END --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
}
