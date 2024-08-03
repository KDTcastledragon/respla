package com.res.pla.service;

import java.time.LocalDateTime;
import java.util.List;

import com.res.pla.domain.UserPurchasedProductDTO;

public interface UserPurchasedProductService {

	List<UserPurchasedProductDTO> selectAllUsableUppsById(String id);   // 현재 사용가능 이용권만 가져온다.

	List<UserPurchasedProductDTO> selectAllUppsById(String id);         // 사용자의 모든 구매이력을 가져온다.

	List<UserPurchasedProductDTO> selectAfterStartDateUppsById(String id, LocalDateTime startDateTime);         // 사용자의 모든 구매이력을 가져온다.

	UserPurchasedProductDTO selectInUsedUppById(String id);			    // 사용자가 현재 사용중인 상품 1개를 가져온다.

	UserPurchasedProductDTO selectUppByUppcode(String uppcode);	        // 상품 1개를 가져온다.

	boolean convertInUsed(String id, String uppcode, boolean inused);     // 사용중/미사용중 구분하는 inused Parameter

	boolean convertUsable(String id, String uppcode, boolean usable);

	boolean isDateConflict(String id, LocalDateTime startDateTime, LocalDateTime endDateTime, int dayValuePeriod);

	void calculateUppInUsedTime(String id, String uppcode);

	void RealTimeUpdateUppTime(String id, String uppcode);

	void stopCalculateUppInUsedTime();

	void afterStartUppDayType(String id, String uppcode, LocalDateTime startDateTime);

	void RealTimeUpdateUppDay(String id, String uppcode);

	void calculateUppInUsedDay(String id, String uppcode);

	void stopCalculateUppInUsedDay();

	List<UserPurchasedProductDTO> selectAllUpps();

}
