package com.res.pla.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.res.pla.domain.UserPurchasedProductDTO;

import lombok.extern.log4j.Log4j2;

@Component
@Transactional
@Log4j2
public class SeatFacade {

	@Autowired
	SeatService seatservice;

	@Autowired
	UserPurchasedProductService uppservice;

	@Autowired
	UsageHistoryService uhservice;

	public boolean checkInSeat(int seatnum, String id, String uppcode, String pType) {
		log.info("");

		int isConvertInUsedTrue = uppservice.convertInUsed(id, uppcode, true);
		int isRecordedUsage = uhservice.recordAction(id, seatnum, "in", uppcode);

		if (pType.equals("m")) {

			int isOccupied = seatservice.occupySeat(seatnum, id, uppcode);
			boolean isStartCalculateTimePass = false;
			uppservice.calculateTimePass(id, uppcode);
			isStartCalculateTimePass = true;

			log.info("SeatFacade 시간권 체크인");
			return (isOccupied > 0) && (isConvertInUsedTrue > 0) && (isRecordedUsage > 0) && isStartCalculateTimePass;

		} else if (pType.equals("d")) {

			int isOccupied = seatservice.occupySeat(seatnum, id, uppcode);

			log.info("SeatFacade 기간권 체크인");
			return (isOccupied > 0) && (isConvertInUsedTrue > 0) && (isRecordedUsage > 0);

		} else if (pType.equals("f")) {
			boolean isUserCheckedIn = seatservice.isExistOccupiedSeatByUserId(id);

			if (isUserCheckedIn) {

				log.info("SeatFacade 고정석 체크인 : 이미 고정석 사용중.");
				return (isRecordedUsage > 0);

			} else {
				int isOccupied = seatservice.occupySeat(seatnum, id, uppcode);

				log.info("SeatFacade 고정석 체크인 : 고정석 첫 이용.");
				return (isOccupied > 0) && (isConvertInUsedTrue > 0) && (isRecordedUsage > 0);
			}

		}

		log.info("SeatFacade 체크인 pType 오류");
		return false;
	}

	public boolean checkOutSeat(int usedSeatNum, String id, String usedUppcode, String pType) {
		log.info("");

		int isConvertInUsedFalse = uppservice.convertInUsed(id, usedUppcode, false);
		int isRecordedUsage = uhservice.recordAction(id, usedSeatNum, "out", usedUppcode);

		if (pType.equals("m")) {

			int isVacated = seatservice.vacateSeat(usedSeatNum, id, usedUppcode);
			boolean isStartCalculateTimePass = false;
			uppservice.stopCalculateTimePass(id, usedUppcode);
			isStartCalculateTimePass = true;

			log.info("SeatFacade 시간권 체크아웃");
			return (isVacated > 0) && (isConvertInUsedFalse > 0) && (isRecordedUsage > 0) && isStartCalculateTimePass;

		} else if (pType.equals("d")) {

			int isVacated = seatservice.vacateSeat(usedSeatNum, id, usedUppcode);

			log.info("SeatFacade 기간권 체크아웃");
			return (isVacated > 0) && (isConvertInUsedFalse > 0) && (isRecordedUsage > 0);

		} else if (pType.equals("f")) {
			log.info("SeatFacade 고정석 체크아웃");
			return (isConvertInUsedFalse > 0) && (isRecordedUsage > 0);
		}

		log.info("SeatFacade 체크아웃 pType 오류");
		return false;
	}

	public boolean moveSeat(int usedSeatNum, int newSeatNum, String id, String uppcode) {
		log.info("");

		log.info("SeatFacade 자리이동");
		return ((seatservice.vacateSeat(usedSeatNum, id, uppcode) > 0) && (seatservice.occupySeat(newSeatNum, id, uppcode) > 0));
	}

	public boolean isUserCheckedIn(String id) {
		boolean isExist = seatservice.isExistOccupiedSeatByUserId(id);
		UserPurchasedProductDTO inUsedUpp = uppservice.selectInUsedTrueUpp(id);

		if (isExist == true && inUsedUpp != null) {

			return true;
		}
		return false;
	}

}
