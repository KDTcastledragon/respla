package com.res.pla.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.res.pla.domain.SeatDTO;

@Mapper
public interface SeatMapper {

	List<SeatDTO> presentAllSeats();

	SeatDTO selectSeatById(String id);

	SeatDTO selectSeat(int seatnum);

	boolean isUserCurrentlyCheckedIn(String id);

	int occupySeat(int seatnum, String id, String uppcode);

	int vacateSeat(int usedSeatnum, String id, String usedUppcode);

}
