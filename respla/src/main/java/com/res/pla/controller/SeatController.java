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
import com.res.pla.domain.UserDTO;
import com.res.pla.domain.UserPurchasedProductDTO;
import com.res.pla.service.SeatService;
import com.res.pla.service.UserPurchasedProductService;
import com.res.pla.service.UserService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/seat")
@Log4j2
@AllArgsConstructor
public class SeatController {

	SeatService seatservice;
	UserService userservice;
	UserPurchasedProductService uppservice;

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
			log.info(choosedData);

			String id = choosedData.get("id");
			String choosedpUppcode = choosedData.get("uppcode");

			UserPurchasedProductDTO inUsedUpp = uppservice.selectInUsedUppOnlyThing(id);
			String choosedUppPtype = uppservice.selectUppByUppcode(choosedpUppcode).getPtype();

			if (inUsedUpp != null && (inUsedUpp.getPtype().equals("d") || inUsedUpp.getPtype().equals("f")) && choosedUppPtype.equals("m")) {
				log.info("중복 사용 방지 : " + choosedpUppcode);

				return ResponseEntity.status(HttpStatus.CONFLICT).body("is already used DayPass");

			} else {
				return ResponseEntity.ok().build();
			}

		} catch (Exception e) {
			log.info("선택상품사용 체크인 예외처리 : " + e.toString());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("finalChoose_Exception");
		}
	}

	//========[3. 입실]==========================================================================================
	@PostMapping(value = "/checkIn")
	public ResponseEntity<?> checkIn(@RequestBody SeatDTO seatdto) {
		try {

			log.info("체크인 요청 데이터 : " + seatdto.toString());

			int seatnum = seatdto.getSeatnum();
			String id = seatdto.getId();
			String uppcode = seatdto.getUppcode(); // 최종선택한 Uppcode

			String uppPType = uppservice.selectUppByUppcode(uppcode).getPtype();
			boolean uppIsUsable = uppservice.selectUppByUppcode(uppcode).isUsable();

			log.info("체크인 작업 전, Data확인 (num/id/ptype/upp) : " + seatnum + " / " + id + " / " + uppPType + " / " + uppcode);

			boolean isUserCheckined = userservice.isCurrentUse(id); // 입실 여부 확인 // 중요한 작업이라 한번 더 확인함.

			if (isUserCheckined) {
				log.info("이미 입실하였음");
				return ResponseEntity.status(HttpStatus.CONFLICT).body("already CheckIn");

			} else if (isUserCheckined == false && uppcode != null && uppIsUsable == true) { // 미입실 && uppcode존재 && upp사용가능
				log.info("미입실상태. 입실을 위한 상품 타입 검사 시작");

				//===[1. 시간권으로 입실]======================================================================================================				
				if (uppPType.equals("m")) {
					log.info("상품타입 검사 : {}", uppPType);
					UserPurchasedProductDTO usableDayPass = uppservice.selectCalculatedUpp(id); // 계산중인 upp상품 가져오기. 시간권 중복사용 방지를 위해.

					if (usableDayPass != null && (usableDayPass.getPtype().equals("d") || usableDayPass.getPtype().equals("f")) && usableDayPass.isUsable() == true) {
						log.info("사용가능한 기간권 이미 보유중. 중복사용 차단.");
						return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Avoid Duplicate"); // 403

					} else {
						log.info("사용가능한 기간권 보유하지 않음.");

						seatservice.checkInSeat(seatnum, id, uppcode, uppPType);      // 체크인

						log.info("시간권 사용하여 체크인 성공");
					}

				}
				//===[2. 기간권/고정석으로 입실]======================================================================================================
				else if (uppPType.equals("d") || uppPType.equals("f")) {
					log.info("상품타입 검사 : {}", uppPType);

					seatservice.checkInSeat(seatnum, id, uppcode, uppPType);      // 체크인

					log.info("기간권 사용하여 체크인 성공");
				}

				log.info("체크인 성공 데이터 확인 : " + seatnum + " / " + id + " / " + uppPType + " / " + uppcode);

				return ResponseEntity.ok().build();

			} else if (uppcode == null) {                             // react 에서도 방지하지만, 중요하므로 한번 더 체크.
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("none choosed Upp");

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

			int usedSeatnum = seatdto.getSeatnum();
			String id = seatdto.getId();
			String usedUppcode = seatdto.getUppcode();

			String uppPType = uppservice.selectUppByUppcode(usedUppcode).getPtype();

			log.info("체크아웃! num/id/upp : " + usedSeatnum + " / " + id + " / " + usedUppcode);

			boolean isUserCheckined = userservice.isCurrentUse(id); // 입실 여부 확인 // 중요한 작업이라 한번 더 확인함.

			if (isUserCheckined) {
				seatservice.checkOutSeat(usedSeatnum, id, usedUppcode);
				if (uppPType.equals("m")) {                                              // 시간권 계산 (== "m"으로 하면 안된다.)
					log.info("시간권 체크아웃");
					uppservice.convertInUsed(id, usedUppcode, false);
					uppservice.stopCalculateScheduler(uppPType);

				} else if (uppDto.getPtype().equals("d") || uppDto.getPtype().equals("f")) {      // 기간권,고정석 계산 (== "d/f" 으로 하면 안된다.) 
					//					uppservice.OperateUppInUsedTimeDay(id, uppcode);
					log.info("d,f타입시 실행됨 : " + uppDto.getPtype());
				}

				return ResponseEntity.ok().build();

			} else {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("you need to checkin First ");
			}

		} catch (Exception e) {
			log.info("checkOut 오류발생 : " + e.toString());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("CheckIn ERROR");
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

			log.info("moveSeat! num/id/upp : " + usedSeatnum + " / " + id + " / " + usedUppcode);

			//			=====[자리이동 작업 시작]=========
			UserDTO userDto = userservice.selectUser(id);
			if (userDto.isCurrentuse()) {
				seatservice.checkOutSeat(usedSeatnum, id, usedUppcode);
				seatservice.checkInSeat(newSeatnum, id, usedUppcode);

				return ResponseEntity.ok().build();

			} else {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("you need to checkin First ");
			}
		} catch (Exception e) {
			log.info("moveSeat 오류발생 : " + e.toString());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("moveSeat ERROR");
		}
	}
}
