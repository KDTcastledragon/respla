package com.res.pla.mapper;

import java.util.List;

import com.res.pla.domain.UsageHistoryDTO;

public interface UsageHistoryMapper {

	int recordAction(String id, int seatNum, String actionType, String uppcode);

	List<UsageHistoryDTO> selectAllHistory();

	List<UsageHistoryDTO> selectAllHistoryById(String id);

	List<UsageHistoryDTO> selectAllHistoryByIdActionType(String id);
}
