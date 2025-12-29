package com.ecom.EcomSB.repositories;

import com.ecom.EcomSB.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @org.springframework.data.jpa.repository.Query("SELECT SUM(o.totalAmount) FROM Order o")
    Double countTotalRevenue();
}
