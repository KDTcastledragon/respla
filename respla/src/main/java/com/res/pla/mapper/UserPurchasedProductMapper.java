package com.res.pla.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.res.pla.domain.UserPurchasedProductDTO;

@Mapper
public interface UserPurchasedProductMapper {

	List<UserPurchasedProductDTO> selectAllUsableUppsById(String id);    // 현재 사용가능 이용권만 가져온다.

	List<UserPurchasedProductDTO> selectAllUppsById(String id);          // 사용자의 모든 구매이력을 가져온다.

	List<UserPurchasedProductDTO> selectAfterStartDateUppsById(String id, LocalDateTime startDateTime);          // 시작날짜 이후의 upp 예매상품 정보.

	List<UserPurchasedProductDTO> selectAllUpps();

	UserPurchasedProductDTO selectInUsedUppOnlyThing(String id);        // 사용자가 현재 사용중인 상품 1개를 가져온다.

	UserPurchasedProductDTO selectCalculatedUpp(String id);             // 현재 시간or기간이 차감중인 상품 1개를 가져온다.

	UserPurchasedProductDTO selectUppByUppcode(String uppcode);         // 상품 1개를 가져온다.

	int convertInUsed(String id, String uppcode, boolean inused);         // 입실시 true , 퇴실시 false.

	int convertCalculated(String id, String uppcode, boolean calculated); // 시간권: 입실시 true , 기간권: endDate까지 true

	int convertUsable(String id, String uppcode, boolean usable);         // 

	int realTimeCalculateUppTimePass(String id, String uppcode, int minute);   // 시간차감 계산

	int realTimeCalculateUppDayPass(String id, String uppcode, int hour);      // 기간차감 계산

	int calculateUppTimeValue(String id, String uppcode, int value);

	int calculateUppDayValue(String id, String uppcode, int value);

}
