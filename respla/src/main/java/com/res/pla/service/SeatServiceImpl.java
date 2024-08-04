package com.res.pla.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.res.pla.domain.SeatDTO;
import com.res.pla.mapper.SeatMapper;

@Service
@Transactional
public class SeatServiceImpl implements SeatService {

	@Autowired
	SeatMapper seatmapper;

	@Override
	public List<SeatDTO> presentAllSeats() {
		return seatmapper.presentAllSeats();
	}

	@Override
	public boolean checkInSeat(int seatnum, String id, String uppcode) {
		int updatedrows = seatmapper.checkInSeat(seatnum, id, uppcode);
		return updatedrows > 0;
	}

	@Override
	public boolean checkOutSeat(int usedSeatnum, String id, String usedUppcode) {
		int updatedrows = seatmapper.checkOutSeat(usedSeatnum, id, usedUppcode);
		return updatedrows > 0;
	}

	@Override
	public boolean moveSeat(int usedseatnum, String id, String useduppcode) {
		int updatedrows = seatmapper.moveSeat(usedseatnum, id, useduppcode);
		return updatedrows > 0;
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

}
