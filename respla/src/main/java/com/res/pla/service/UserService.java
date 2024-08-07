package com.res.pla.service;

import java.util.List;

import com.res.pla.domain.UserDTO;

public interface UserService {

	List<UserDTO> selectAllUsers();

	UserDTO selectUser(String id);

	boolean matchId(String id);

	boolean isCurrentUse(String id); // upp를 사용하지 않고 입실이 되는 경우는, seatService에서 미리 방지할 것이다.

}
