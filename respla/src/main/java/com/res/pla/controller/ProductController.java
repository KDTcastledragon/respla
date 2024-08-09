package com.res.pla.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.res.pla.domain.ProductDTO;
import com.res.pla.service.ProductService;
import com.res.pla.service.SeatService;
import com.res.pla.service.UserPurchasedProductService;
import com.res.pla.service.UserService;
import com.res.pla.util.Fdate;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/product")
@AllArgsConstructor
@Log4j2
public class ProductController {

	ProductService productservice;
	UserPurchasedProductService uppservice;
	UserService userservice;
	SeatService seatservice;

	//=[1]==============================================================================
	@GetMapping("/productListByPType")
	public ResponseEntity<?> productListByPType(@RequestParam(name = "ptype") String ptype) {
		try {
			log.info("");
			log.info("productList_ptype 확인 : " + ptype);

			List<ProductDTO> productList = productservice.selectPtypeProducts(ptype);

			log.info("selectPtypeProducts 확인 : " + productList);

			return ResponseEntity.ok().body(productList);

		} catch (Exception e) {

			log.info("productList 오류발생 : " + e.toString());

			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("ProductLists Error");

		}
	}

	//==[2]===============================================================================================================================
	@PostMapping("/isDateConflict")
	public ResponseEntity<?> isDateConflict(@RequestBody Map<String, Object> requestData) {
		try {
			log.info("");
			log.info("구매상품 요청 Data: " + requestData);

			String id = (String) requestData.get("id");
			String startDateTimeString = (String) requestData.get("startDateTime");
			String endDateTimeString = (String) requestData.get("endDateTime");

			LocalDateTime startDateTime = (startDateTimeString == null) ? null : LocalDateTime.parse(startDateTimeString); // 시작날짜 타입변환
			LocalDateTime endDateTime = (endDateTimeString == null) ? null : LocalDateTime.parse(endDateTimeString); // 시작날짜 타입변환

			if (startDateTime != null && endDateTime != null) {                                                                     // null Exception 처리.
				log.info("시작일 ~ 종료일 :: {} ~ {} ", Fdate.chg(startDateTime), Fdate.chg(endDateTime));

				boolean isDateConflictResult = uppservice.isDateConflict(id, startDateTime, endDateTime);    // 날짜 충돌 여부 검사
				log.info("날짜충돌검사 결과 : " + isDateConflictResult);

				if (isDateConflictResult == false) {
					return ResponseEntity.ok().build();

				} else {
					log.info("종료날짜와 다른 상품의 시작날짜가 겹침. 409");
					return ResponseEntity.status(HttpStatus.CONFLICT).body("isDateConflict"); // 409 Conflict
				}

			} else if (startDateTime == null && endDateTime == null) {
				return ResponseEntity.ok().build(); // 잠깐만 쓰자 ㅎ

			} else {
				// startDateTime && endDateTime == null (시작/종료날짜가 존재하지 않음)
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("DateTime is null");      // 400 Bad Request
			}

		} catch (Exception e) {
			log.info("purchaseProduct_예외처리 : " + e.toString());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("purchase_Product Exception");
		}
	}

	//==[3]===============================================================================================================================
	@PostMapping("/payment")
	public ResponseEntity<?> payment(@RequestBody Map<String, Object> requestData) {
		try {
			log.info("");
			log.info("상품결제 요청 Data: " + requestData);

			String id = (String) requestData.get("id");
			int productcode = (int) requestData.get("productcode");
			String pType = (String) requestData.get("ptype");
			String startDateTimeString = (String) requestData.get("startDateTime");
			String endDateTimeString = (String) requestData.get("endDateTime");
			String paymentOption = (String) requestData.get("paymentOption");

			LocalDateTime startDateTime = (startDateTimeString == null) ? null : LocalDateTime.parse(startDateTimeString); // 시작날짜 타입변환
			LocalDateTime endDateTime = (endDateTimeString == null) ? null : LocalDateTime.parse(endDateTimeString); // 시작날짜 타입변환

			//==[1. 기간권 , 고정석 상품 구매]==================================================
			if ((pType.equals("d") || pType.equals("f")) && (startDateTime != null && endDateTime != null)) {
				LocalDateTime currentDateTime = LocalDateTime.now();        // 현재 시간.
				log.info("시작일 ~ 종료일 :: {} ~ {} ", Fdate.chg(startDateTime), Fdate.chg(endDateTime));

				String purchasedUppcode = productservice.purchaseProduct(id, productcode, pType, startDateTime, endDateTime, paymentOption, false);
				log.info("기간권 구매 성공 (uppcode) : " + purchasedUppcode);

				//==[1-1. 시작날짜가 구매날짜보다 뒤일때 (예약구매) ]============================
				if (startDateTime.isAfter(currentDateTime)) {
					log.info("1-1. 시작날짜가 구매날짜보다 뒤일때 (예약구매) ");

					uppservice.afterLaunchDayPassFromStartDate(id, purchasedUppcode, startDateTime);
					log.info("예약구매 확인 : " + id + purchasedUppcode + startDateTime);
				}

				//==[1-2. 시작날짜가 구매날짜보다 전이거나 같을때 (구매 즉시 사용가능) ]============================
				else {
					log.info("1-2. 시작날짜가 구매날짜보다 전이거나 같을때 (구매 즉시 사용가능)");
					uppservice.convertUsable(id, purchasedUppcode, true);
					uppservice.validateTimePassBeforeCalculateDayPass(id, purchasedUppcode);
				}

				return ResponseEntity.ok().build();
			}

			//==[2. 시간권 상품 구매]==================================================
			else if (pType.equals("m")) {
				String purchasedUppcode = productservice.purchaseProduct(id, productcode, pType, startDateTime, endDateTime, paymentOption, true);
				log.info("시간권 구매 성공 (uppcode) : " + purchasedUppcode);

				return ResponseEntity.ok().build();

			} else {
				log.info("존재하지 않는 상품 타입");
				return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("unKnown pType Error");
			}

		} catch (Exception e) {
			//			UserPurchasedProductDTO wahtTheHellisIt = uppservice.selectUppByUppcode(purchasedUppcode);   // 어따 쓰려고 만들어놧지?????????? 뭐지?
			throw e;
		} // try-catch
	} // 전체 메소드

	@GetMapping("/allProductList")
	public ResponseEntity<?> allProductList() {
		try {
			List<ProductDTO> productList = productservice.selectAllProducts();

			return ResponseEntity.ok().body(productList);

		} catch (Exception e) {

			log.info("productList 오류발생 : " + e.toString());
			throw e;

		}
	}

}
