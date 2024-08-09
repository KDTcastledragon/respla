package com.res.pla.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.res.pla.domain.SeatDTO;
import com.res.pla.domain.UserPurchasedProductDTO;
import com.res.pla.service.SeatFacade;
import com.res.pla.service.SeatService;
import com.res.pla.service.UsageHistoryService;
import com.res.pla.service.UserPurchasedProductService;
import com.res.pla.service.UserService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/seat")
@Log4j2
@AllArgsConstructor
public class SeatController {

	SeatFacade seatfacade;
	SeatService seatservice;
	UserService userservice;
	UserPurchasedProductService uppservice;
	UsageHistoryService usgservice;

	//	====[1. 좌석현황 출력]==========================================================================================
	@GetMapping("/presentAllSeats")
	public ResponseEntity<?> presentAllSeats() {
		try {
			List<SeatDTO> seatLists = seatservice.presentAllSeats();

			log.info("seatList성공 : " + seatLists);

			return ResponseEntity.ok(seatLists);
		} catch (Exception e) {

			log.info("seatList[info] 오류발생 : " + e.toString());
			log.error("seatList[error] 오류내용" + e.toString());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("seat ERROR");

		}
	}

	//====[2. 기간권 사용중 시간권 중복사용 방지]===============================================
	@PostMapping(value = "/finalChooseProduct")
	public ResponseEntity<?> finalChooseProduct(@RequestBody Map<String, String> choosedData) {
		try {
			log.info("입실 사용할 상품 :" + choosedData);

			String id = choosedData.get("id");
			String choosedpUppcode = choosedData.get("uppcode");

			String choosedUppPtype = uppservice.selectUppByUppcode(choosedpUppcode).getPtype();
			UserPurchasedProductDTO alreadyUsedUpp = uppservice.selectCalculatedTrueUpp(id);               // 

			if (choosedUppPtype.equals("m") && alreadyUsedUpp != null && (alreadyUsedUpp.getPtype().equals("d") || alreadyUsedUpp.getPtype().equals("f"))) {
				log.info("기간권 사용중, 시간권 중복 사용 방지");

				return ResponseEntity.status(HttpStatus.CONFLICT).body("is already used DayPass"); // 409?

			} else {
				log.info("체크인 사용할 상품 선택 : " + choosedpUppcode);
				return ResponseEntity.ok().build();
			}

		} catch (Exception e) {
			log.info("선택상품사용 체크인 예외처리 : " + e.toString());
			throw e;
		}
	}

	//========[3. 입실]==========================================================================================
	@PostMapping(value = "/checkIn")
	public ResponseEntity<?> checkIn(@RequestBody SeatDTO seatdto) {
		try {

			log.info("체크인 요청 데이터 : " + seatdto.toString());

			int seatnum = seatdto.getSeatnum();
			String id = seatdto.getId();
			String uppcode = seatdto.getUppcode(); // 위 메소드의 choosedpUppcode와 같은 값.

			String uppPType = uppservice.selectUppByUppcode(uppcode).getPtype();
			boolean uppIsUsable = uppservice.selectUppByUppcode(uppcode).isUsable();

			log.info("체크인 작업 전, Data확인 (num/id/ptype/upp) : " + seatnum + " / " + id + " / " + uppPType + " / " + uppcode);

			boolean isUserCheckedIn = seatfacade.isUserCurrentlyCheckedIn(id); // 입실 여부 확인 // 중요한 작업이라 한번 더 확인함.
			log.info("체크인여부 확인 : " + isUserCheckedIn);

			if (isUserCheckedIn) {
				log.info("이미 입실하였음");
				return ResponseEntity.status(HttpStatus.CONFLICT).body("already CheckIn");  // 409

			} else if (isUserCheckedIn == false && uppcode != null && uppIsUsable == true) { // 미입실 && uppcode존재 && upp사용가능
				log.info("미입실상태. 입실을 위한 상품 타입 검사 시작");

				//===[1. 시간권으로 입실]======================================================================================================				
				if (uppPType.equals("m")) {
					log.info("상품타입 검사 : {}", uppPType);

					UserPurchasedProductDTO usableDayPass = uppservice.selectUsableOneUppByIdPType(id, "df"); // 사용가능한 기간권 보유 확인.

					if (usableDayPass != null) {
						log.info("사용가능한 기간권 이미 보유중. 중복사용 차단.");
						return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Avoid Duplicate"); // 403

					} else {
						log.info("사용가능한 기간권 보유하지 않음.");
						log.info("시간권 사용하여 체크인 시도" + uppPType);

					}

				}
				//===[2. 기간권/고정석으로 입실]======================================================================================================
				else if (uppPType.equals("d") || uppPType.equals("f")) {
					log.info("상품타입 검사 : {}", uppPType);
					log.info("기간권 사용하여 체크인 시도 " + uppPType);

				}

				seatfacade.checkInSeat(seatnum, id, uppcode, uppPType);      // 체크인
				log.info("체크인 성공 데이터 확인 : " + seatnum + " / " + id + " / " + uppPType + " / " + uppcode);

				return ResponseEntity.ok().build();

			} else if (uppIsUsable == false) {     // 체크인 시도 도중, 상품(기간권/고정석)의 기간이 모두 소모되어 입실이 불가능해진 상태.

				log.info("사용불가능 상품. 재구매 필요.");
				return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("upp is Not Usable now"); // 422

			} else if (uppcode == null) {

				log.info("uppcode == null");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("etc");

			} else {
				return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("etc");
			}

		} catch (Exception e) {
			log.info("체크인 예외처리 : " + e.toString());
			throw e;
		}
	}

	//	====[3. 퇴실]==========================================================================================
	@PostMapping(value = "/checkOut")
	public ResponseEntity<?> checkOut(@RequestBody SeatDTO seatdto) {
		try {
			log.info("체크아웃 요청 데이터 : " + seatdto.toString());

			String id = seatdto.getId();
			int usedSeatnum = seatdto.getSeatnum();
			String usedUppcode = seatdto.getUppcode();

			String uppPType = uppservice.selectUppByUppcode(usedUppcode).getPtype();

			boolean isUserCheckedIn = seatfacade.isUserCurrentlyCheckedIn(id); // 입실 여부 확인 // 중요한 작업이라 한번 더 확인함.

			log.info("체크아웃 작업 전, Data확인 (num/id/ptype/upp) : " + usedSeatnum + " / " + id + " / " + uppPType + " / " + usedUppcode);

			if (isUserCheckedIn == true && usedUppcode != null) {
				log.info("체크인여부 , 사용upp 확인. 체크아웃 작업 시작");

				seatfacade.checkOutSeat(usedSeatnum, id, usedUppcode, uppPType);

				log.info("체크아웃 성공");
				return ResponseEntity.ok().build();

			} else {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("you need to checkin First "); // 403
			}

		} catch (Exception e) {
			log.info("checkOut 예외처리 : " + e.toString());
			throw e;
		}
	}

	//====[4. 자리이동]===========================//:: 오류 발생률을 줄이기 위해, 컨트롤러의 checkIn/Out 메소드를 재사용하는 대신, 새로 생성함.
	@PostMapping(value = "/moveSeat")
	public ResponseEntity<?> moveSeat(@RequestBody SeatDTO seatdto) {
		try {

			log.info("moveSeat dto : " + seatdto.toString());

			int newSeatnum = seatdto.getSeatnum();      // 새로 옮길 자리
			String id = seatdto.getId();
			String usedUppcode = seatdto.getUppcode();

			int usedSeatnum = seatservice.selectSeatById(id).getSeatnum();
			String uppPType = uppservice.selectUppByUppcode(usedUppcode).getPtype();

			log.info("moveSeat! num/id/upp : " + usedSeatnum + " / " + id + " / " + usedUppcode);

			//			=====[자리이동 작업 시작]=========

			boolean isUserCheckedIn = seatfacade.isUserCurrentlyCheckedIn(id); // 입실 여부 확인 // 중요한 작업이라 한번 더 확인함.

			log.info("자리이동 작업 전, Data확인 (num/id/ptype/upp) : " + usedSeatnum + " / " + id + " / " + uppPType + " / " + usedUppcode);

			if (isUserCheckedIn == true && usedUppcode != null) {
				log.info("체크인여부 , 사용upp 확인. 자리이동 작업 시작");

				seatfacade.moveSeat(usedSeatnum, newSeatnum, id, usedUppcode);

				log.info("자리이동 성공");
				return ResponseEntity.ok().build();

			} else {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body("you need to checkin First "); // 403
			}
		} catch (Exception e) {
			log.info("moveSeat 예외처리 : " + e.toString());
			throw e;
		}
	}
}
