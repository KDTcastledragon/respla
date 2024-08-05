package com.res.pla.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.res.pla.domain.UserPurchasedProductDTO;
import com.res.pla.mapper.SeatMapper;
import com.res.pla.mapper.UserMapper;
import com.res.pla.mapper.UserPurchasedProductMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class UserPurchasedProductServiceImpl implements UserPurchasedProductService {

	@Autowired
	UserPurchasedProductMapper uppmapper;

	@Autowired
	UserMapper usermapper;

	@Autowired
	SeatMapper seatmapper;

	//	@Autowired
	//	SeatService seatservice;

	@Autowired
	private TaskScheduler dayTaskScheduler;

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> calculateTimeScheduler;
	private ScheduledFuture<?> calculateDayScheduler;

	@Override
	public List<UserPurchasedProductDTO> selectAllUsableUppsById(String id) {
		return uppmapper.selectAllUsableUppsById(id);
	}

	@Override
	public List<UserPurchasedProductDTO> selectAllUppsById(String id) {
		return uppmapper.selectAllUppsById(id);
	}

	@Override
	public List<UserPurchasedProductDTO> selectAfterStartDateUppsById(String id, LocalDateTime startDateTime) {
		return uppmapper.selectAfterStartDateUppsById(id, startDateTime);
	}

	@Override
	public UserPurchasedProductDTO selectInUsedUppOnlyThing(String id) {
		return uppmapper.selectInUsedUppOnlyThing(id);
	}

	@Override
	public UserPurchasedProductDTO selectUppByUppcode(String uppcode) {
		return uppmapper.selectUppByUppcode(uppcode);
	}

	@Override
	public boolean convertInUsed(String id, String uppcode, boolean inused) {
		int updatedresult = uppmapper.convertInUsed(id, uppcode, inused);
		return updatedresult > 0;
	}

	@Override
	public boolean convertUsable(String id, String uppcode, boolean usable) {
		int updatedresult = uppmapper.convertUsable(id, uppcode, usable);
		return updatedresult > 0;
	}

	@Override
	public boolean isDateConflict(String id, LocalDateTime startDateTime, LocalDateTime endDateTime, int dayValuePeriod) {
		List<UserPurchasedProductDTO> uppList = selectAfterStartDateUppsById(id, startDateTime);           // 지정시작날짜보다 후일인 상품목록들(전체를 불러오면 너무 많다.)
		UserPurchasedProductDTO usedUpp = selectInUsedUppOnlyThing(id);                                         // 현재 사용중인 상품(전체를 불러오면 너무 많다.)

		//		log.info("ServiceImpl 날짜충돌검사 upp / List : " + usedUpp.toString() + " / " + uppList.toString());

		//==[1. 기간권 , 고정석 상품을 사용중일 때 ]=================================================================		
		if (usedUpp != null && (usedUpp.getPtype().equals("d") || usedUpp.getPtype().equals("f"))) {
			// 사용중인 상품의 시작날짜 < (구매상품 시작날짜 || 구매상품 종료날짜) : 이 명제는 항상 참이기 때문에, usedUppstartDateTime 경우는 따지지 않는다.

			LocalDateTime usedUppEndDateTime = usedUpp.getEnddate();

			if (usedUppEndDateTime != null && usedUppEndDateTime.isAfter(startDateTime)) {
				log.info("기간충돌 (구매 상품의 시작날짜 < 사용중인 상품의 종료날짜) : " + startDateTime + " < " + usedUppEndDateTime);

				return true;
			}

		}

		//==[2. 시간권 상품 사용 || 사용중인 상품 없을때 ]=================================================================	
		else {
			for (UserPurchasedProductDTO upp : uppList) {
				LocalDateTime uppStartDateTime = upp.getStartdate();
				LocalDateTime uppEndDateTime = upp.getEnddate();

				// 구매 상품의 종료날짜 < 미리 구매한 상품의 시작 날짜 : 이 조건만 만족하면 되기 때문에, 여집합을 필터링 조건으로 설정한다.

				if (endDateTime.isAfter(uppStartDateTime) || endDateTime.equals(uppStartDateTime)) {
					log.info("기간충돌 ( 사용예정 상품의 시작날짜 < 구매 상품의 종료날짜 ) : " + uppEndDateTime + " < " + endDateTime);

					return true;
				}

			} // for

		} // if-else

		return false;
	}

	@Override
	public void RealTimeUpdateUppTime(String id, String uppcode, int minute) {
		uppmapper.RealTimeUpdateUppTime(id, uppcode, minute);
	}

	@Override
	public void RealTimeUpdateUppDay(String id, String uppcode, int hour) {
		uppmapper.RealTimeUpdateUppDay(id, uppcode, hour);
	}

	@Override
	public void calculateUppInUsedTime(String id, String uppcode) {
		UserPurchasedProductDTO usedUppDto = uppmapper.selectUppByUppcode(uppcode);
		int minute = 1;

		log.info("scheduleAtFixedRate usedUppDto : " + usedUppDto.toString());

		if (usedUppDto != null && usedUppDto.getUppcode().equals(uppcode) && usedUppDto.getPtype().equals("m")) { // 사용중인 uppcode 일치 검사
			log.info("scheduleAtFixedRate usedUppDto.getUppcode() : " + usedUppDto.getUppcode().toString());
			log.info("scheduleAtFixedRate usedUppDto.getPtype() : " + usedUppDto.getPtype().toString());

			calculateTimeScheduler = scheduler.scheduleAtFixedRate(() -> {
				UserPurchasedProductDTO currentUppDto = uppmapper.selectInUsedUppOnlyThing(id); // ★최신 상태조회를 반드시 해야함★usedUppDto를 쓰면 반영이 안됨.

				if (currentUppDto.getAvailabletime() >= 1) { // 남은 시간이 존재할 경우에만.

					uppmapper.RealTimeUpdateUppTime(id, currentUppDto.getUppcode(), minute);

					log.info(id + " 의 " + uppcode + " 상품 (분) " + currentUppDto.getAvailabletime() + " 실시간 차감 실행");

				} else if (currentUppDto.getAvailabletime() <= 0) { // 사용가능 시간을 모두 소비.

					log.info(id + " 의 " + uppcode + " 상품 (분) " + currentUppDto.getAvailabletime() + "모두 소비");

					int usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();

					usermapper.checkOutCurrentUse(id);
					seatmapper.checkOutSeat(usedSeatnum, id, uppcode);
					uppmapper.convertInUsed(id, uppcode, false);
					uppmapper.convertUsable(id, uppcode, false);
					stopCalculateUppInUsedTime();

					log.info(id + " 의 " + uppcode + " 상품 사용 종료. ");

				}
			}, 10, minute, TimeUnit.SECONDS); // [if:시간권 사용시]

		} // if:dto null검사 && uppcode 일치 검사
	} // 전체 메소드

	@Override // 지정된 날짜에 자동 작동하며 시간 차감 시작함==========================================================================
	public void afterStartUppDayType(String id, String purchasedUppcode, LocalDateTime startDateTime) {
		try {
			String cronStartDateTime = String.format("%d %d %d %d %d ?", startDateTime.getSecond(), startDateTime.getMinute(), startDateTime.getHour(), startDateTime.getDayOfMonth(), startDateTime.getMonthValue());

			CronTrigger cronTrigger = new CronTrigger(cronStartDateTime);

			dayTaskScheduler.schedule(() -> {

				log.info("예약구매 기간권 스케줄러 작동 시작.");

				UserPurchasedProductDTO alreadyUsedUpp = uppmapper.selectInUsedUppOnlyThing(id);       // 현재 '사용중'인 상품
				log.info("alreadyUsedUpp가 과연 null일까요?? 시발꺼?" + alreadyUsedUpp);

				if ((alreadyUsedUpp.getPtype().equals("m"))) {
					log.info("시간권 사용중일 경우, 예약구매 기간권으로 변경 후 계산 준비 시작");

					int usedSeatNum = seatmapper.selectSeatById(id).getSeatnum();     // 사용중인 좌석 번호.
					String alreadyUsedUppcode = alreadyUsedUpp.getUppcode();		   // 사용중인 시간권 상품의 uppcode.

					seatmapper.checkOutSeat(usedSeatNum, id, alreadyUsedUppcode);      // 시간권으로 사용중인 좌석 체크아웃.
					uppmapper.convertInUsed(id, alreadyUsedUppcode, false);            // 사용중인 시간권 상품 사용상태 전환.
					stopCalculateUppInUsedTime();                                      // 사용중인 시간권 상품 시간계산 중단.

					uppmapper.convertUsable(id, purchasedUppcode, true);
					uppmapper.convertInUsed(id, purchasedUppcode, true);
					calculateUppInUsedDay(id, purchasedUppcode);

					seatmapper.checkInSeat(usedSeatNum, id, purchasedUppcode);
					log.info("현재 시간권 사용중 구매상품 시작날짜 도달. 시간권 사용종료 후 기간권 사용으로 변경  : " + id + purchasedUppcode + startDateTime);

				} else {

					// 시작 시간에 구매한 상품의 usable과 inused 상태를 true로 변경
					uppmapper.convertUsable(id, purchasedUppcode, true);
					uppmapper.convertInUsed(id, purchasedUppcode, true);
					calculateUppInUsedDay(id, purchasedUppcode);

					log.info("구매상품 시작날짜 도달. 사용 가능으로 변경 : ", id, purchasedUppcode, startDateTime);
				}

			}, cronTrigger);

		} catch (Exception e) {
			log.error("startUppDayType 오류 발생 : " + e.toString());
		}
	}

	@Override // 사용중인 시간권,고정석 날짜 계산=====================================================================
	public void calculateUppInUsedDay(String id, String uppcode) {
		UserPurchasedProductDTO usedUppDto = uppmapper.selectInUsedUppOnlyThing(id);
		int hour = 100;

		log.info("scheduleAtFixedRate usedUppDto : " + usedUppDto.toString());

		if (usedUppDto != null && usedUppDto.getUppcode().equals(uppcode) && usedUppDto.getPtype().equals("d")) { // 사용중인 uppcode 일치 검사
			log.info("scheduleAtFixedRate usedUppDto.getUppcode() : " + usedUppDto.getUppcode().toString());
			log.info("scheduleAtFixedRate usedUppDto.getPtype() : " + usedUppDto.getPtype().toString());

			calculateDayScheduler = scheduler.scheduleAtFixedRate(() -> {
				UserPurchasedProductDTO currentUppDto = uppmapper.selectInUsedUppOnlyThing(id); // ★최신 상태조회를 반드시 해야함★usedUppDto를 쓰면 반영이 안됨.

				if (currentUppDto.getAvailableday() >= 1) { // 남은 시간이 존재할 경우에만.

					uppmapper.RealTimeUpdateUppDay(id, currentUppDto.getUppcode(), hour);

					log.info(id + " 의 " + uppcode + " 상품 " + currentUppDto.getAvailableday() + " 실시간 차감 실행");

				} else if (currentUppDto.getAvailableday() <= 0) { // 사용가능 시간을 모두 소비.

					log.info(id + " 의 " + uppcode + " 상품 " + currentUppDto.getAvailableday() + "기간 만료");

					int usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();

					usermapper.checkOutCurrentUse(id);                    // 사용중인 자리 자동 체크아웃
					seatmapper.checkOutSeat(usedSeatnum, id, uppcode);

					uppmapper.convertInUsed(id, uppcode, false);
					log.info("기간제 상품 만료 후 , 미사용으로 전환[inused = false] " + uppcode);

					uppmapper.convertUsable(id, uppcode, false);
					log.info("기간제 상품 만료 후 , 사용불가로 전환[usable = false] " + uppcode);

					stopCalculateUppInUsedDay();

					log.info(id + " 의 " + uppcode + " 상품 사용 종료. ");

				}
			}, 10, 5, TimeUnit.SECONDS); // [if:기간권/고정석 사용시]
			//								}, 0, 24, TimeUnit.HOURS); // [if:기간권/고정석 사용시]

		} // if:dto null검사 && uppcode 일치 검사

	} // 전체 메소드

	@Override
	public void stopCalculateUppInUsedTime() {
		if (calculateTimeScheduler != null) {
			calculateTimeScheduler.cancel(true);
			log.info("시간 차감계산 중단 성공");
		} else {
			log.info("시간 차감계산 중단 실패!!!");
		}
	}

	@Override
	public void stopCalculateUppInUsedDay() {
		if (calculateDayScheduler != null) {
			calculateDayScheduler.cancel(true);
			log.info("일 차감계산 중단 성공");
		} else {
			log.info("일 차감계산 중단 실패!!!");
		}
	}

	@Override
	public List<UserPurchasedProductDTO> selectAllUpps() {
		return uppmapper.selectAllUpps();
	}

}
