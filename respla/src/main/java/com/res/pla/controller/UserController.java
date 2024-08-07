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
			boolean isUserCheckined = userservice.isCurrentUse(id);

			Map<String, Object> loginedUserData = new HashMap<>();
			loginedUserData.put("getedId", id);
			loginedUserData.put("isCurrentUse", isUserCheckined);

			if (isUserCheckined) {
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

			boolean isUserCheckined = userservice.isCurrentUse(id);

			log.info("현재 유저 입실상태 확인 : " + isUserCheckined);

			if (isUserCheckined) {
				log.info("유저입실상태 [입실 했음]");
				return ResponseEntity.ok().build();  // 200

			} else {
				log.info("유저입실상태 [미입실] : " + isUserCheckined);

				List<UserPurchasedProductDTO> usableUppDto = uppservice.selectAllUsableUppsById(id);
				log.info("isCurrentUse:usableuppdto 확인 : " + usableUppDto.toString());

				if (usableUppDto.isEmpty()) {
					log.info("usableUppDto_EMPTY : " + usableUppDto.toString());
					return ResponseEntity.status(HttpStatus.NO_CONTENT).body("사용가능 상품 없음. 구매 필요."); // 204

				} else {
					log.info("usableUppDto_OCCUPIED : " + usableUppDto.toString());
					return ResponseEntity.status(HttpStatus.ACCEPTED).body("사용가능상품 존재. 이용하면 됩니다."); // 202
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

}
