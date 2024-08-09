package com.res.pla.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.res.pla.domain.UserDTO;
import com.res.pla.domain.UserPurchasedProductDTO;
import com.res.pla.service.SeatFacade;
import com.res.pla.service.SeatService;
import com.res.pla.service.UserPurchasedProductService;
import com.res.pla.service.UserService;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
@Log4j2
public class UserController {

	UserPurchasedProductService uppservice;
	UserService userservice;
	SeatService seatservice;
	SeatFacade seatfacade;

	//====[1. 로그인]========================================================================================
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody UserDTO request) {
		String id = request.getId();

		log.info("request.id :" + id);

		UserDTO dto = userservice.selectUser(id);

		if (dto != null && userservice.matchId(id) == true) {
			log.info("login 성공" + dto.toString());

			return new ResponseEntity<>(dto, HttpStatus.OK);

		} else if (dto == null) {
			log.info("아이디 없음");
			return new ResponseEntity<>(dto, HttpStatus.BAD_GATEWAY);

		} else {
			log.info("아이디 불일치" + dto.toString());
			return new ResponseEntity<>(dto, HttpStatus.UNAUTHORIZED);
		}
	}

	//====[2. 로그인 유저 실시간 정보]========================================================================================
	@PostMapping("/loginedUser")
	public ResponseEntity<?> loginedUser(@RequestBody Map<String, String> idData) {
		try {
			log.info("loginedUser_idData 조회 : " + idData);

			String id = idData.get("id");
			boolean isUserCheckedIn = seatfacade.isUserCheckedIn(id); // 입실 여부 확인

			Map<String, Object> loginedUserData = new HashMap<>();
			loginedUserData.put("getedId", id);
			loginedUserData.put("isCurrentUse", isUserCheckedIn);

			if (isUserCheckedIn) {
				int usedSeatNum = seatservice.selectSeatById(id).getSeatnum();
				UserPurchasedProductDTO upp = uppservice.selectInUsedTrueUpp(id);

				loginedUserData.put("usedSeatNum", usedSeatNum);
				loginedUserData.put("inUsedUppcode", upp.getUppcode());
				loginedUserData.put("ptype", upp.getPtype());

				if (upp.getPtype().equals("m")) {
					loginedUserData.put("initialTimeValue", upp.getInitialtimevalue());
					loginedUserData.put("usedTime", upp.getUsedtime());
					loginedUserData.put("availableTime", upp.getAvailabletime());

				} else if (upp.getPtype().equals("d") || upp.getPtype().equals("f")) {
					loginedUserData.put("initialDayValue", upp.getInitialdayvalue());
					loginedUserData.put("usedDay", upp.getUsedday());
					loginedUserData.put("availableDay", upp.getAvailableday());

				} else {
					log.info("로그인 사용자 upp pType 오류");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("ptype error");
				}
			}

			log.info("loginedUserData 전송 : " + loginedUserData.toString());

			return ResponseEntity.ok().body(loginedUserData);

		} catch (Exception e) {
			throw e;
		}
	}

	//====[3. 입실 상태 & 사용가능 상품 존재 여부 확인]========================================================================================isCurrentUse
	@PostMapping("/verifyUserStatus")
	public ResponseEntity<?> verifyUserStatus(@RequestBody UserDTO loginID) {
		try {

			String id = loginID.getId();
			log.info("verifyUserStatus id확인 : " + id);

			boolean isUserCheckedIn = seatfacade.isUserCheckedIn(id); // 입실 여부 확인
			log.info("사용자 현재입실상태 : {} ", (isUserCheckedIn == true ? "입실중" : "미입실"));

			if (isUserCheckedIn) {
				return ResponseEntity.ok().build();  // 200

			} else {
				List<UserPurchasedProductDTO> usableUppList = uppservice.selectAllUsableUppsById(id);
				log.info("사용가능한 상품 리스트 : " + usableUppList.toString());

				if (usableUppList.isEmpty()) {
					log.info("사용가능 상품 없음. 구매 필요. : " + usableUppList.toString());
					return ResponseEntity.status(HttpStatus.NO_CONTENT).body("usableUppList_no_content_204"); // 204

				} else {
					log.info("사용가능상품 존재. 이용하면 됩니다. : " + usableUppList.toString());
					return ResponseEntity.status(HttpStatus.ACCEPTED).body("usableUppList_aceepted_202"); // 202
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}

	//====[4. ]========================================================================================isCurrentUse
	@GetMapping("/allUserList")
	public ResponseEntity<?> allUserList() {
		List<UserDTO> userList = userservice.selectAllUsers();

		return ResponseEntity.ok(userList);
	}

	//====[5. ]========================================================================================isCurrentUse
	@GetMapping("/abcde")
	public ResponseEntity<?> abcde() {
		userservice.clean();
		log.info("");
		log.info("@@@@@@@@@@@@@쉬발 싹다 지워버려잇@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		log.info("");

		return ResponseEntity.ok().build();
	}

}
