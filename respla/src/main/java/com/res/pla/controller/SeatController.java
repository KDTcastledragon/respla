package com.res.pla.controller;

import java.util.List;

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

	//	====[2. 입실]==========================================================================================
	@PostMapping(value = "/checkIn")
	public ResponseEntity<?> checkIn(@RequestBody SeatDTO seatdto) {
		try {

			log.info("체크인 선택자리 & 사용할 상품코드 dto : " + seatdto.toString());

			int seatnum = seatdto.getSeatnum();
			String id = seatdto.getId();
			String uppcode = seatdto.getUppcode(); // 선택한 usedUppcode
			UserPurchasedProductDTO uppDto = uppservice.selectUppByUppcode(uppcode);
			UserPurchasedProductDTO inUsedUppAvoidDuplicate = uppservice.selectInUsedUppById(id);

			log.info("체크인 전 확인 num/id/ptype/upp : " + seatnum + " / " + id + " / " + uppDto.getPtype() + " / " + uppcode);

			//====[체크인 작업 시작]=======================================================
			UserDTO userDto = userservice.selectUser(id);
			if (userDto.isCurrentuse()) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body("already CheckIn");

			} else if (userDto.isCurrentuse() == false && uppcode != null && uppDto.isUsable() == true) { // 미입실 && uppcode존재 && upp사용가능
				log.info("체크인 전 좌석정보 확인 : " + seatservice.selectSeat(seatnum));

				userservice.checkInCurrentUse(id);                  // 사용자 : '사용중'으로 전환
				seatservice.checkInSeat(seatnum, id, uppcode);      // 좌석: '사용중'으로 전환
				uppservice.convertInUsed(id, uppcode, true);        // upp: '사용중'으로 전환

				//==[시간권 사용하여 체크인.]==================================================
				if (uppDto.getPtype().equals("m")) {                                              // 시간권 계산 (== "m"으로 하면 안된다.)

					//==[기간권 소유시, 시간권 중복 사용 방지.]
					if (inUsedUppAvoidDuplicate != null && (inUsedUppAvoidDuplicate.getPtype().equals("d") || inUsedUppAvoidDuplicate.getPtype().equals("f"))) {
						return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Avoid Duplicate"); // 403
					} else {
						uppservice.calculateUppInUsedTime(id, uppcode);
						log.info("m타입시 실행 : " + uppDto.getPtype());
					}

				}

				//==[기간권,고정석 사용하여 체크인.]==================================================
				else if (uppDto.getPtype().equals("d") || uppDto.getPtype().equals("f")) {      // 기간권,고정석 계산 (== "d/f" 으로 하면 안된다.) 
					log.info("d,f타입시 실행 : " + uppDto.getPtype());
				}

				log.info("체크인 마감 num/id/ptype/upp : " + seatnum + " / " + id + " / " + uppDto.getPtype() + " / " + uppcode);

				return ResponseEntity.ok().build();

			} else if (uppcode == null) {                                         // React에서도 uppcode null 방지를 하지만, 입실관련 이므로 한번더 방지.
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("none choosed Upp");

			} else {
				return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("etc");
			}
		} catch (Exception e) {
			log.info("seatList[info] 오류발생 : " + e.toString());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("CheckIn ERROR");
		}
	}

	//	====[3. 퇴실]==========================================================================================
	@PostMapping(value = "/checkOut")
	public ResponseEntity<?> checkOut(@RequestBody SeatDTO seatdto) {
		try {

			log.info("체크아웃 dto : " + seatdto.toString());

			int usedSeatnum = seatdto.getSeatnum();
			String id = seatdto.getId();
			String usedUppcode = seatdto.getUppcode();

			UserPurchasedProductDTO uppDto = uppservice.selectUppByUppcode(usedUppcode);

			log.info("체크아웃! num/id/upp : " + usedSeatnum + " / " + id + " / " + usedUppcode);

			//			=====[체크아웃 작업 시작]=========
			UserDTO userDto = userservice.selectUser(id);
			if (userDto.isCurrentuse()) {
				userservice.checkOutCurrentUse(id);
				seatservice.checkOutSeat(usedSeatnum, id, usedUppcode);
				if (uppDto.getPtype().equals("m")) {                                              // 시간권 계산 (== "m"으로 하면 안된다.)
					log.info("시간권 체크아웃 : " + uppDto.getPtype());
					uppservice.convertInUsed(id, usedUppcode, false);
					uppservice.stopCalculateUppInUsedTime();

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
