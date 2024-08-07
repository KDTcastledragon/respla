package com.res.pla.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.res.pla.domain.ProductDTO;
import com.res.pla.mapper.ProductMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ProductServiceImpl implements ProductService {

	@Autowired
	ProductMapper productmapper;

	@Override
	public List<ProductDTO> selectPtypeProducts(String ptype) {
		return productmapper.selectPtypeProducts(ptype);
	}

	@Override
	public ProductDTO selectProduct(int productcode) {
		return productmapper.selectProduct(productcode);
	}

	@Override
	public String purchaseProduct(String id, int productcode, String pType, LocalDateTime startDateTime, LocalDateTime endDateTime, String paymentOption, boolean usable) {

		Map<String, Object> params = new HashMap<>();
		params.put("id", id);
		params.put("productcode", productcode);
		params.put("startDateTime", startDateTime);
		params.put("endDateTime", endDateTime);
		params.put("paymentOption", paymentOption);
		params.put("usable", usable);

		productmapper.purchaseProduct(params);
		productmapper.updateSellCount(productcode, 1);

		// 여기에서 params에 의해 설정된 uppcode 값을 가져온다.
		return (String) params.get("uppcode");
	}

	//	================================================================================

}
