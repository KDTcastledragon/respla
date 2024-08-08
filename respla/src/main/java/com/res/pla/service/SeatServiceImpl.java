package com.res.pla.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.pla.domain.SeatDTO;
import com.res.pla.mapper.SeatMapper;
import com.res.pla.mapper.UserPurchasedProductMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Transactional
@Log4j2
public class SeatServiceImpl implements SeatService {

	@Autowired
	SeatMapper seatmapper;

	@Autowired
	UserPurchasedProductMapper uppmapper;

	@Autowired
	UserPurchasedProductService uppservice;

	@Override
	public List<SeatDTO> presentAllSeats() {
		return seatmapper.presentAllSeats();
	}

	@Override
	public SeatDTO selectSeatById(String id) {
		SeatDTO usedSeat = seatmapper.selectSeatById(id);
		return usedSeat;
	}

	@Override
	public SeatDTO selectSeat(int seatnum) {
		return seatmapper.selectSeat(seatnum);
	}

	@Override
	public boolean isUserCurrentlyCheckedIn(String id) {
		log.info("");
		log.info("사용자 입실여부 판별 위해, seatmapper 가동준비");
		log.info("");
		return seatmapper.isUserCurrentlyCheckedIn(id); // 입실 여부 확인 // 중요한 작업이라 한번 더 확인함.
	}

	@Override
	public boolean checkInSeat(int seatnum, String id, String uppcode, String pType) {
		log.info("");

		int occupiedSeatRows = seatmapper.occupySeat(seatnum, id, uppcode);
		int convertResult = uppmapper.convertInUsed(id, uppcode, true);

		if (pType.equals("m")) {
			log.info("시간권 체크인. 시간계산 진입 준비");
			uppservice.calculateTimePass(id, uppcode);
		}

		log.info("");

		return occupiedSeatRows > 0;
	}

	@Override
	public boolean checkOutSeat(int usedSeatnum, String id, String usedUppcode, String pType) {
		log.info("");

		uppmapper.convertInUsed(id, usedUppcode, false);

		if (pType.equals("m")) {
			seatmapper.vacateSeat(usedSeatnum, id, usedUppcode);
			log.info("시간권 체크아웃. 시간계산 중단 진입 준비");

			uppservice.stopCalculateTimePass(id, usedUppcode);

		} else if (pType.equals("d")) {
			seatmapper.vacateSeat(usedSeatnum, id, usedUppcode);
			log.info("기간권 체크아웃.");
		}

		log.info("");

		return true;
	}

}
