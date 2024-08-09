package com.res.pla.service;

import java.util.List;

import com.res.pla.domain.SeatDTO;

public interface SeatService {

	List<SeatDTO> presentAllSeats();

	SeatDTO selectSeat(int seatnum);

	SeatDTO selectSeatById(String id);

	boolean isExistOccupiedSeatByUserId(String id);

	int occupySeat(int seatnum, String id, String uppcode);

	int vacateSeat(int usedSeatnum, String id, String usedUppcode);

	boolean shiftSeat(int usedSeatnum, int newSeatnum, String id, String uppcode);

}
