package com.ecom.EcomSB.controller;

import com.ecom.EcomSB.repositories.OrderRepository;
import com.ecom.EcomSB.repositories.ProductRepository;
import com.ecom.EcomSB.security.response.AnalyticsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/app")
public class AnalyticsController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/analytics")
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        Long productCount = productRepository.count();
        Long totalOrders = orderRepository.count();
        Double totalRevenue = orderRepository.countTotalRevenue();

        if (totalRevenue == null) {
            totalRevenue = 0.0;
        }

        AnalyticsResponse response = new AnalyticsResponse(productCount, totalOrders, totalRevenue);
        return ResponseEntity.ok(response);
    }
}
