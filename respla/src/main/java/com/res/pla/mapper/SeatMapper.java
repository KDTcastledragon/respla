package com.res.pla.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.res.pla.domain.SeatDTO;

@Mapper
public interface SeatMapper {

	List<SeatDTO> presentAllSeats();
	
	int checkInSeat(int seatnum , String id, String uppcode);
	
	int checkOutSeat(int usedSeatnum , String id, String usedUppcode);

	int moveSeat(int usedseatnum , String id, String usedUppcode);
	
	SeatDTO selectSeatById(String id);
	
	SeatDTO selectSeat(int seatnum);
    

}
