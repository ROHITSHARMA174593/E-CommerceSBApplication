package com.ecom.EcomSB.security.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    private Long productCount;
    private Long totalOrders;
    private Double totalRevenue;
}
