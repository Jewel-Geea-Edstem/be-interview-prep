package com.example.prep.order.repository;

import com.example.prep.product.entity.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProductStockRepository extends Repository<Product, Long> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update Product p set p.stock = p.stock - :quantity where p.id = :id and p.stock >= :quantity")
  int reserve(@Param("id") Long id, @Param("quantity") int quantity);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Product p set p.stock = p.stock + :quantity where p.id = :id")
  int release(@Param("id") Long id, @Param("quantity") int quantity);

  @Query("select p.stock from Product p where p.id = :id")
  Optional<Integer> findStock(@Param("id") Long id);
}
