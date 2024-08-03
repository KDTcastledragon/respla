package com.res.pla.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.res.pla.domain.UserDTO;
import com.res.pla.mapper.UserMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class UserServiceImpl implements UserService{
	
	@Autowired
	UserMapper usermapper;

	@Override
	public UserDTO selectUser(String id) {
		return usermapper.selectUser(id);
	}

	@Override
	public int matchId(String id) {
		UserDTO userid = usermapper.selectUser(id);
		
		
		log.info("userid.getId : " +userid.getId());
		
		if(userid.getId().equals(id)) {
			return 1;
		} else {
			
			return 0;
		}
	}
	
	@Override
	public List<UserDTO> selectAllUsers() {
		return usermapper.selectAllUsers();
	}
	
	@Override
	public boolean checkInCurrentUse(String id) {
		int updatedrow = usermapper.checkInCurrentUse(id);
		return updatedrow > 0;
	}
	
	@Override
	public boolean checkOutCurrentUse(String id) {
		int updatedrow = usermapper.checkOutCurrentUse(id);
		return updatedrow > 0;
	}
	



	



	

}
