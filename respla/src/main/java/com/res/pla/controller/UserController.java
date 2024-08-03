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

import com.res.pla.domain.SeatDTO;
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

		if (dto != null && userservice.matchId(id) == 1) {

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
			//			log.info("loginedUser_idData 조회 : " + idData);

			String getedId = idData.get("id");

			boolean isCurrentUse = userservice.selectUser(getedId).isCurrentuse();

			UserPurchasedProductDTO inUsedUppDto = uppservice.selectInUsedUppById(getedId);   // null debugging

			log.info("loginedUser_inUsedUppDto 조회 : " + inUsedUppDto);

			String inUsedUppcode = (inUsedUppDto != null) ? inUsedUppDto.getUppcode() : null;  // 입실시, 사용중인 상품 uppcode

			log.info("loginedUser_inUsedUppcode 조회 : " + inUsedUppcode);

			String pType = (inUsedUppDto != null) ? inUsedUppDto.getPtype() : null;            // 입실시, 사용중인 상품 타입

			int initialTimeValue = (inUsedUppDto != null) ? inUsedUppDto.getInitialtimevalue() : 0;  // null debugging

			int usedTime = (inUsedUppDto != null) ? inUsedUppDto.getUsedtime() : 0;  // null debugging

			int availabletime = (inUsedUppDto != null) ? inUsedUppDto.getAvailabletime() : 0;  // null debugging

			int initialDayValue = (inUsedUppDto != null) ? inUsedUppDto.getInitialdayvalue() : 0;  // null debugging

			int usedDay = (inUsedUppDto != null) ? inUsedUppDto.getUsedday() : 0;  // null debugging

			int availableDay = (inUsedUppDto != null) ? inUsedUppDto.getAvailableday() : 0;  // null debugging

			SeatDTO debugSeatDto = seatservice.selectSeatById(getedId);                 // null debugging

			//	        log.info("loginedUser_debugSeatDto 조회 : " + debugSeatDto);

			int usedSeatnum = (debugSeatDto == null) ? 0 : debugSeatDto.getSeatnum();  // null debugging [ null 저장불가. 0으로 처리]

			//	        log.info("loginedUser_usedSeatnum 조회 : " + usedSeatnum);

			//			log.info("loginedUser_ gId iCU iUUpp uSn : " + getedId +" / "+ isCurrentUse +" / "+ inUsedUppcode +" / "+ usedSeatnum);

			Map<String, Object> loginedUserData = new HashMap<>();

			//			log.info("HashMap<>() loginedUserData : " + loginedUserData);

			loginedUserData.put("getedId", getedId);
			loginedUserData.put("isCurrentUse", isCurrentUse);
			loginedUserData.put("usedSeatNum", usedSeatnum);
			loginedUserData.put("inUsedUppcode", inUsedUppcode);
			loginedUserData.put("ptype", pType);
			loginedUserData.put("initialTimeValue", initialTimeValue);
			loginedUserData.put("usedTime", usedTime);
			loginedUserData.put("availableTime", availabletime);
			loginedUserData.put("initialDayValue", initialDayValue);
			loginedUserData.put("usedDay", usedDay);
			loginedUserData.put("availableDay", availableDay);

			log.info("loginedUserData.toString() 확인 : " + loginedUserData.toString());
			log.info("");

			return ResponseEntity.ok().body(loginedUserData);

		} catch (Exception e) {
			log.info("loginedUser 오류발생 : " + e.toString());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("seat ERROR");

		}
	}

	//====[3. 입실 상태 & 사용가능 상품 존재 여부 확인]========================================================================================isCurrentUse
	@PostMapping("/isCurrentUse")
	public ResponseEntity<?> isCurrentUse(@RequestBody UserDTO loginID) {
		String id = loginID.getId();

		log.info("isCurrentUse id확인 : " + id);

		UserDTO userDto = userservice.selectUser(id);

		log.info("isCurrentUse dto 확인 : " + userDto.toString());

		if (userDto.isCurrentuse()) {
			log.info("isCurrentUse [입실 했음] : " + userDto.toString());
			return ResponseEntity.ok().build();  // 200

		} else {
			log.info("isCurrentUse [미입실 상태] : " + userDto.toString());

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
	}

	//====[4. ]========================================================================================isCurrentUse
	@GetMapping("/allUserList")
	public ResponseEntity<?> allUserList() {
		List<UserDTO> userList = userservice.selectAllUsers();

		return ResponseEntity.ok(userList);
	}

}
