package com.ecom.EcomSB.service;

import com.ecom.EcomSB.payload.OrderDTO;
import org.springframework.lang.NonNull;

public interface OrderService {
    OrderDTO placeOrder(String emailId, @NonNull Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);
}
