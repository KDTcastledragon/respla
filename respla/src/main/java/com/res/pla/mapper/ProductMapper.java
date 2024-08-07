package com.res.pla.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.res.pla.domain.ProductDTO;

@Mapper
public interface ProductMapper {
	List<ProductDTO> selectPtypeProducts(String ptype);

	ProductDTO selectProduct(int productcode);

	void purchaseProduct(Map<String, Object> params);

	int updateSellCount(int productcode, int amount);

	int updateRefundCount(int productcode, int amount);

	//	void purchaseProduct(String id, int productcode, LocalDateTime startDateTime, boolean usable); // void를 유지하면서 uppcode를 반환받으려면 이 방법은 불가능하다.
	//	void purchaseProduct(@Param("id") String id, @Param("productcode") int productcode, @Param("startDateTime") LocalDateTime startDateTime, @Param("usable") boolean usable);
}
