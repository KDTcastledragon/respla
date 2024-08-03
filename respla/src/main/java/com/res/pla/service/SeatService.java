package com.res.pla.service;

import java.util.List;
import com.res.pla.domain.SeatDTO;

public interface SeatService {

	List<SeatDTO> presentAllSeats();
	
	boolean checkInSeat(int seatnum , String id , String uppcode);   // 오류 발생을 줄이기 위해 구분해서 만듦.

	boolean checkOutSeat(int usedSeatnum , String id , String usedUppcode);  // 오류 발생을 줄이기 위해 구분해서 만듦.
	
	boolean moveSeat(int usedseatnum , String id , String useduppcode);  // 오류 발생을 줄이기 위해 구분해서 만듦.
	
	SeatDTO selectSeatById(String id);
	
	SeatDTO selectSeat(int seatnum);
    
}



