package com.res.pla.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.res.pla.domain.UsageHistoryDTO;
import com.res.pla.mapper.UsageHistoryMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UsageHistoryServiceImpl implements UsageHistoryService {

	@Autowired
	UsageHistoryMapper usgmapper;

	@Override
	public int recordAction(String id, int seatNum, String actionType, String uppcode) {
		return usgmapper.recordAction(id, seatNum, actionType, uppcode);
	}

	@Override
	public List<UsageHistoryDTO> selectAllHistory() {
		return usgmapper.selectAllHistory();
	}

	@Override
	public List<UsageHistoryDTO> selectAllHistoryById(String id) {
		return usgmapper.selectAllHistoryById(id);
	}

	@Override
	public List<UsageHistoryDTO> selectAllHistoryByIdActionType(String id) {
		return usgmapper.selectAllHistoryByIdActionType(id);
	}

}
