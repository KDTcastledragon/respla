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
import com.res.pla.domain.UserPurchasedProductDTO;
import com.res.pla.service.ProductService;
import com.res.pla.service.SeatService;
import com.res.pla.service.UserPurchasedProductService;
import com.res.pla.service.UserService;

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
	@GetMapping("/productList")
	public ResponseEntity<?> productList(@RequestParam(name = "ptype") String ptype) {
		try {
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

			log.info("상품구매 Data: " + requestData);

			String id = (String) requestData.get("id");
			String startDateTimeString = (String) requestData.get("startDateTime");
			String endDateTimeString = (String) requestData.get("endDateTime");

			LocalDateTime startDateTime = (startDateTimeString == null) ? null : LocalDateTime.parse(startDateTimeString); // 시작날짜 타입변환
			LocalDateTime endDateTime = (endDateTimeString == null) ? null : LocalDateTime.parse(endDateTimeString); // 시작날짜 타입변환

			if (startDateTime != null && endDateTime != null) {                                                                     // null Exception 처리.
				log.info("시작일 ~ 종료일 : " + startDateTime + " ~ " + endDateTime);

				boolean isDateConflictResult = uppservice.isDateConflict(id, startDateTime, endDateTime);    // 날짜 충돌 여부 검사
				log.info("날짜충돌검사 결과 : " + isDateConflictResult);

				if (isDateConflictResult == false) {
					return ResponseEntity.ok().build();

				} else {
					log.info("종료날짜와 다른 상품의 시작날짜가 겹침. 409");
					return ResponseEntity.status(HttpStatus.CONFLICT).body("isDateConflict"); // 409 Conflict
				}

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
			if (pType.equals("d") || pType.equals("f")) {
				LocalDateTime currentDateTime = LocalDateTime.now();        // 현재 시간.

				if (startDateTime != null && endDateTime != null) {                                                                     // null Exception 처리.
					log.info("기간권 시작일 ~ 종료일 : " + startDateTime + " ~ " + endDateTime);

					boolean isDateConflictResult = uppservice.isDateConflict(id, startDateTime, endDateTime);    // 날짜 충돌 여부 검사
					log.info("날짜충돌검사 결과 : " + isDateConflictResult);

					//==[날짜 충돌 없을때. (== 구매가능상태) 이중검사...이긴 하다. ]					
					if (isDateConflictResult == false) {

						String purchasedUppcode = productservice.purchaseProduct(id, productcode, pType, startDateTime, endDateTime, paymentOption, true);
						log.info("상품 구매 성공 (uppcode) : " + purchasedUppcode);

						UserPurchasedProductDTO wahtTheHellisIt = uppservice.selectUppByUppcode(purchasedUppcode);   // 어따 쓰려고 만들어놧지?????????? 뭐지?

						//==[1-1. 시작날짜가 구매날짜보다 뒤일때 (==예약구매) ]============================
						if (startDateTime != null && startDateTime.isAfter(currentDateTime)) {
							log.info("시작날짜가 구매날짜보다 뒤일때 (==예약구매)");

							uppservice.convertUsable(id, purchasedUppcode, false); // 예약날짜에 true가 되야하므로, false로 변경.
							uppservice.afterCalculateDayPassFromStartDate(id, purchasedUppcode, startDateTime);
							log.info("예약구매 확인 : " + id + purchasedUppcode + startDateTime);
						}

						//==[1-2. 시작날짜가 구매날짜보다 전이거나 같을때 (==구매 즉시 사용) ]============================
						else {
							log.info("시작날짜가 구매날짜보다 전이거나 같을때::False : " + startDateTime.isAfter(currentDateTime));

							UserPurchasedProductDTO alreadyUsedUpp = uppservice.selectI(id);       // 현재 '사용중'인 상품

							//==[1-2-a. 현재 시간권상품을 사용하여 입실한 경우, 기간권으로 자동 변경시 ]=======================							
							if (alreadyUsedUpp != null && (alreadyUsedUpp.getPtype().equals("m"))) {
								int usedSeatNum = seatservice.selectSeatById(id).getSeatnum();     // 사용중인 좌석 번호.
								String alreadyUsedUppcode = alreadyUsedUpp.getUppcode();		   // 사용중인 시간권 상품의 uppcode.

								seatservice.checkOutSeat(usedSeatNum, id, alreadyUsedUppcode);     // 시간권으로 사용중인 좌석 체크아웃.
								uppservice.convertInUsed(id, alreadyUsedUppcode, false);           // 사용중인 시간권 상품 사용안함으로 전환.
								uppservice.convertCalculated(id, alreadyUsedUppcode, false);       // 사용중인 시간권 시간계산 중단.
								uppservice.stopCalculateScheduler(alreadyUsedUpp.getPtype());                           // 사용중인 시간권 상품 시간계산 중단.

								uppservice.convertUsable(id, purchasedUppcode, true);              // 구매한 기간권 상품 사용가능 전환
								uppservice.convertCalculated(id, purchasedUppcode, true);              // 구매한 기간권 상품 사용가능 전환
								uppservice.convertInUsed(id, purchasedUppcode, true);              // 구매한 기간권 상품 사용가능 전환
								uppservice.manageDayPass(id, purchasedUppcode, pType);            // 구매한 기간권 상품 시간계산 시작.

								seatservice.checkInSeat(usedSeatNum, id, purchasedUppcode);        // 구매한 기간권으로 같은 좌석 재 체크인.
								log.info("시간권 체크아웃 & 기간권 즉시 변경 후 재 체크인");

							}

							//==[1-2-b. 체크인에 사용중인 시간권 상품 없을 시   ]=======================	
							else {
								uppservice.manageDayPass(id, purchasedUppcode);
								log.info("기간권 (즉시 사용가능) : " + id + " / " + purchasedUppcode);        // 재 체크인 때문에 코드 중복 사용.
							}
						}

						return ResponseEntity.ok().build();

					} else {
						// isDateConflictResult == true (종료날짜와 다른 상품의 시작날짜가 겹침)
						log.info("종료날짜와 다른 상품의 시작날짜가 겹침. 409");
						return ResponseEntity.status(HttpStatus.CONFLICT).body("isDateConflict"); // 409 Conflict
					}

				} else {
					// startDateTime == null (시작날짜가 존재하지 않음)
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("startDateTime is null");      // 400 Bad Request
				}
			}

			//==[2. 시간권 상품 구매]==================================================
			else if (pType.equals("m")) {
				String purchasedUppcode = productservice.purchaseProduct(id, productcode, pType, startDateTime, endDateTime, paymentOption, false);
				uppservice.convertUsable(id, purchasedUppcode, true);
				log.info("시간권 구매 성공");

				return ResponseEntity.ok().build();

			} else {
				log.info("ptype 오류");
				return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("unKnown pType Error");
			}

		} catch (Exception e) {

			log.info("purchaseProduct_예외처리 : " + e.toString());
			throw e;
		}
	}

	////		 =======[??]===================================================================================================
	//		public ResponseEntity<?> selectProduct(int productcode) {
	//			try {
	//				List<ProductDTO> productLists = productservice.selectAllProducts();
	//				
	//				log.info("seatList성공 : " + productservice);
	//				
	//				return ResponseEntity.ok(productLists);
	//			} catch (Exception e) {
	//				
	//				log.info("seatList[info] 오류발생 : " + e.toString());
	//				log.error("seatList[error] 오류내용"+ e.toString());
	//				return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("seat ERROR");
	//			}
	//		}
	//		 

}
