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
	public SeatDTO selectSeat(int seatnum) {
		return seatmapper.selectSeat(seatnum);
	}

	@Override
	public SeatDTO selectSeatById(String id) {
		SeatDTO usedSeat = seatmapper.selectSeatById(id);
		return usedSeat;
	}

	@Override
	public boolean checkInSeat(int seatnum, String id, String uppcode, String pType) {
		int occupiedSeatRows = seatmapper.occupySeat(seatnum, id, uppcode);
		int convertResult = uppmapper.convertInUsed(id, uppcode, true);

		if (pType.equals("m")) {
			log.info("시간권 체크인. 시간계산 진입 준비");
			uppservice.calculateTimePass(id, uppcode);
		}

		return occupiedSeatRows > 0;
	}

	@Override
	public boolean checkOutSeat(int usedSeatnum, String id, String usedUppcode, String pType) {
		int vacantSeatRows = seatmapper.vacateSeat(usedSeatnum, id, usedUppcode);
		int convertResult = uppmapper.convertInUsed(id, usedUppcode, false);

		if (pType.equals("m")) {
			log.info("시간권 체크아웃. 시간계산 중단 진입 준비");
			uppservice.stopCalculateTimePass(id, usedUppcode);
		}

		return vacantSeatRows > 0;
	}

}
