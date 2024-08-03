package com.res.pla.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.res.pla.domain.UserPurchasedProductDTO;
import com.res.pla.service.ProductService;
import com.res.pla.service.UserPurchasedProductService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/upp")
@AllArgsConstructor
@Log4j2
public class UserPurchasedProductController {
	
	UserPurchasedProductService uppservice;
	
	@PostMapping("/selectAllUsableUppsById")
	public ResponseEntity<?> selectAllUsableUppsById(@RequestBody Map<String,String> idData) {
		try {
			log.info("구매이력조회 id : " + idData);
			
			String getedId = idData.get("id");
			
			List<UserPurchasedProductDTO> productLists = uppservice.selectAllUsableUppsById(getedId);
			
			log.info("selectAllUppsById성공 : " + productLists);
			
			return ResponseEntity.ok(productLists);
		} catch (Exception e) {
			
			log.info("selectAllUppsById[info] 오류발생 : " + e.toString());
			log.error("selectAllUppsById[error] 오류내용"+ e.toString());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("seat ERROR");

		}
	}
		
		//===[3]=============================================================================
		
	@PostMapping("/selectAllUppsById")
	public ResponseEntity<?> selectAllUppsById(@RequestBody Map<String,String> idData) {
		try {
			log.info("구매이력조회 id : " + idData);
			
			String getedId = idData.get("id");
			
			List<UserPurchasedProductDTO> uppList = uppservice.selectAllUppsById(getedId);
			
			log.info("selectAllUppsById성공 : " + uppList);
			
			return ResponseEntity.ok(uppList);
		} catch (Exception e) {
			
			log.info("selectAllUppsById[info] 오류발생 : " + e.toString());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("seat ERROR");

		}
	}
}
