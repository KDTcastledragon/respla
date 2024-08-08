package com.res.pla.service;

import java.util.List;

import com.res.pla.domain.UserDTO;

public interface UserService {

	List<UserDTO> selectAllUsers();

	UserDTO selectUser(String id);

	boolean matchId(String id);

}
