package com.res.pla.service;

import java.time.LocalDateTime;
import java.util.List;

import com.res.pla.domain.ProductDTO;

public interface ProductService {

	List<ProductDTO> selectPtypeProducts(String ptype);

	List<ProductDTO> selectAllProducts();

	String purchaseProduct(String id, int productcode, String pType, LocalDateTime startDateTime, LocalDateTime endDateTime, String paymentOption, boolean usable);

	ProductDTO selectProduct(int productcode);

}
