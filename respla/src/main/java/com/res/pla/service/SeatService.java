package com.res.pla.service;

import java.util.List;

import com.res.pla.domain.SeatDTO;

public interface SeatService {

	List<SeatDTO> presentAllSeats();

	SeatDTO selectSeat(int seatnum);

	SeatDTO selectSeatById(String id);

	boolean isUserCurrentlyCheckedIn(String id);

	boolean checkInSeat(int seatnum, String id, String uppcode, String pType);   // 오류 발생을 줄이기 위해 구분해서 만듦.

	boolean checkOutSeat(int usedSeatnum, String id, String usedUppcode, String pType);  // 오류 발생을 줄이기 위해 구분해서 만듦.

	boolean moveSeat(int usedSeatnum, int newSeatnum, String id, String uppcode);

}
