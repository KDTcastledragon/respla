package com.res.pla.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.res.pla.domain.UserDTO;

@Mapper
public interface UserMapper {

	UserDTO selectUser(String id);

	List<UserDTO> selectAllUsers();

	int matchId(String id);

}
