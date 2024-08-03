package com.res.pla.service;

import java.util.List;

import com.res.pla.domain.UserDTO;

public interface UserService {
	
	UserDTO selectUser(String id);
	
	boolean checkInCurrentUse(String id);   // 오류 발생율을 줄이기 위해 구분해서 만듦.
	
	boolean checkOutCurrentUse(String id);  // 오류 발생율을 줄이기 위해 구분해서 만듦.
	
	List<UserDTO> selectAllUsers();
	
	int matchId(String id);

}
