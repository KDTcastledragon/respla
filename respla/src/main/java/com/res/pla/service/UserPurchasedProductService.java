package com.res.pla.service;

import java.time.LocalDateTime;
import java.util.List;

import com.res.pla.domain.UserPurchasedProductDTO;

public interface UserPurchasedProductService {

	List<UserPurchasedProductDTO> selectAllUsableUppsById(String id);   // 현재 사용가능 이용권만 가져온다.

	List<UserPurchasedProductDTO> selectAllUppsById(String id);         // 사용자의 모든 구매이력을 가져온다.

	List<UserPurchasedProductDTO> selectAfterStartDateUppsById(String id, LocalDateTime startDateTime);         // 사용자의 시작날짜 이후 상품들을 가져온다.

	List<UserPurchasedProductDTO> selectAllUpps();

	UserPurchasedProductDTO selectInUsedUppOnlyThing(String id);	    // 사용자가 현재 사용중인 상품 1개를 가져온다.

	UserPurchasedProductDTO selectCalculatedUpp(String id);			    //  현재 시간 / 기간이 차감중인 상품 1개를 가져온다.

	UserPurchasedProductDTO selectUppByUppcode(String uppcode);	        // 상품 1개를 가져온다.

	//==[사용 전환 관련 메소드]====================================================================
	boolean convertInUsed(String id, String uppcode, boolean inused);     // 사용중/미사용중 구분하는 inused Parameter

	boolean convertCalculated(String id, String uppcode, boolean calculated);

	boolean convertUsable(String id, String uppcode, boolean usable);

	boolean isDateConflict(String id, LocalDateTime startDateTime, LocalDateTime endDateTime);

	//==[계산 / 중단 관련 메소드]====================================================================

	void stopCalculateScheduler(String pType);

	//==[상품 계산 제어 메소드]====================================================================
	void manageTimePass(String id, String uppcode, String pType);

	void afterManageDayPassFromStartDate(String id, String uppcode, LocalDateTime startDateTime, String pType);

	void manageDayPass(String id, String uppcode, String pType);

}
