package com.res.pla.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.res.pla.domain.SeatDTO;
import com.res.pla.domain.UserDTO;
import com.res.pla.mapper.SeatMapper;
import com.res.pla.mapper.UserMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

	@Autowired
	UserMapper usermapper;

	@Autowired
	SeatMapper seatmapper;

	@Override
	public List<UserDTO> selectAllUsers() {
		return usermapper.selectAllUsers();
	}

	@Override
	public UserDTO selectUser(String id) {
		return usermapper.selectUser(id);
	}

	@Override
	public boolean matchId(String id) {
		try {

			UserDTO userid = usermapper.selectUser(id);

			log.info("matchId : " + userid.getId());

			if (userid.getId().equals(id)) {
				return true;

			} else {

				return false;
			}
		} catch (Exception e) {
			log.info("아이디일치검사 예외처리 : " + e.toString());
			return false;
		}
	}

	@Override
	public boolean isCurrentUse(String id) {
		try {
			SeatDTO usedSeatByUser = seatmapper.selectSeatById(id);

			if (usedSeatByUser != null) {
				log.info("입실여부검사 : 입실중 " + usedSeatByUser.toString());
				return true;

			} else {
				log.info("입실여부 검사 : 미입실");
				return false;
			}

		} catch (Exception e) {
			log.info("입실여부 판별 예외처리" + e.toString());
			return false;
		}
	}

}
