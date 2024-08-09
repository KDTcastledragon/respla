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

	@Override
	public List<SeatDTO> presentAllSeats() {
		return seatmapper.presentAllSeats();
	}

	@Override
	public SeatDTO selectSeatById(String id) {
		return seatmapper.selectSeatById(id);
	}

	@Override
	public SeatDTO selectSeat(int seatnum) {
		return seatmapper.selectSeat(seatnum);
	}

	@Override
	public boolean isExistOccupiedSeatByUserId(String id) {
		log.info("");
		return seatmapper.isExistOccupiedSeatByUserId(id); // 입실 여부 확인 // 중요한 작업이라 한번 더 확인함.
	}

	@Override
	public int occupySeat(int seatnum, String id, String uppcode) {
		log.info("");
		return seatmapper.occupySeat(seatnum, id, uppcode);
	}

	@Override
	public int vacateSeat(int usedSeatnum, String id, String usedUppcode) {
		log.info("");
		return seatmapper.vacateSeat(usedSeatnum, id, usedUppcode);
	}

	@Override
	public boolean shiftSeat(int usedSeatnum, int newSeatnum, String id, String uppcode) {
		log.info("");

		int isVacated = seatmapper.vacateSeat(usedSeatnum, id, uppcode);
		int isOccupied = seatmapper.occupySeat(newSeatnum, id, uppcode);

		boolean isShift = (isVacated > 0) && (isOccupied > 0);

		log.info("");
		return isShift;
	}

}
