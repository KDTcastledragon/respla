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
	public List<UserPurchasedProductDTO> selectAllUpps() {
		return uppmapper.selectAllUpps();
	}

	@Override
	public UserPurchasedProductDTO selectInUsedTrueUpp(String id) {
		return uppmapper.selectInUsedTrueUpp(id);
	}

	@Override
	public UserPurchasedProductDTO selectCalculatedTrueUpp(String id) {
		return uppmapper.selectCalculatedTrueUpp(id);
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
	public boolean convertCalculated(String id, String uppcode, boolean calculated) {
		int updatedresult = uppmapper.convertCalculated(id, uppcode, calculated);
		return updatedresult > 0;
	}

	@Override
	public boolean convertUsable(String id, String uppcode, boolean usable) {
		int updatedresult = uppmapper.convertUsable(id, uppcode, usable);
		return updatedresult > 0;
	}

	@Override
	public boolean isDateConflict(String id, LocalDateTime startDateTime, LocalDateTime endDateTime) {
		List<UserPurchasedProductDTO> uppList = selectAfterStartDateUppsById(id, startDateTime);           // 지정시작날짜보다 후일인 상품목록들(전체를 불러오면 너무 많다.)
		UserPurchasedProductDTO usedUpp = selectCalculatedTrueUpp(id);                                    // 현재 사용중인 상품(전체를 불러오면 너무 많다.)

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

				// 구매 상품의 종료날짜 =< 미리 구매한 상품의 시작 날짜 : 이 조건만 만족하면 되기 때문에, 여집합을 필터링 조건으로 설정한다.

				if (endDateTime.isAfter(uppStartDateTime)) {
					log.info("기간충돌 ( 사용예정 상품의 시작날짜 < 구매 상품의 종료날짜 ) : " + uppEndDateTime + " < " + endDateTime);
					return true;
				}

			} // for

		} // if-else

		return false;
	}

	//====[시간 계산]==================================================================================================================================	

	@Override
	public void stopCalculateTimePass(String id, String uppcode) {
		if (calculateTimeScheduler != null) {
			calculateTimeScheduler.cancel(true);
			log.info("시간권 상품 시간 차감 중단 성공");

		} else {
			log.info("calculateTimeScheduler == null");
		}
	}

	@Override
	public void endCalculateDayPass(String id, String uppcode) {
		if (calculateDayScheduler != null) {
			int resultInUsed = uppmapper.convertInUsed(id, uppcode, false);
			int resultcaluclated = uppmapper.convertCalculated(id, uppcode, false);
			int resultUsable = uppmapper.convertUsable(id, uppcode, false);

			calculateDayScheduler.cancel(true);
			log.info("기간권 상품 계산 종료");

		} else {
			log.info("calculateDayScheduler == null");
		}

	}

	@Override
	public void calculateTimePass(String id, String uppcode) {
		UserPurchasedProductDTO upp = uppmapper.selectInUsedTrueUpp(uppcode);
		int minute = 1;

		if (upp != null && upp.getUppcode().equals(uppcode) && upp.getPtype().equals("m")) { // 사용중인 uppcode 일치 검사
			uppmapper.convertCalculated(id, uppcode, true);
			log.info("convertCalculated == true ");

			calculateTimeScheduler = scheduler.scheduleAtFixedRate(() -> {
				UserPurchasedProductDTO currentUpp = uppmapper.selectCalculatedTrueUpp(id); // ★최신 상태조회를 반드시 해야함★usedUppDto를 쓰면 반영이 안됨.

				// [1. 상품 시간이 존재할 경우]====================================================
				if (currentUpp.getAvailabletime() >= 1) {
					log.info(id + " 의 " + uppcode + " : " + currentUpp.getAvailabletime() + " 시간 계산 작동 시작");

					uppmapper.calculateUppTimeValue(id, currentUpp.getUppcode(), minute);
					log.info(id + " 의 " + uppcode + " : " + currentUpp.getAvailabletime() + " 시간(분) 계산 작동 성공.");

				}
				// [2. 상품 시간 모두 소비.]====================================================
				else if (currentUpp.getAvailabletime() <= 0) { // 
					log.info(id + " 의 " + uppcode + " : " + currentUpp.getAvailabletime() + "시간 모두 소비");

					int usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();

					seatmapper.vacateSeat(usedSeatnum, id, uppcode);
					uppmapper.convertInUsed(id, uppcode, false);
					uppmapper.convertCalculated(id, uppcode, false);
					uppmapper.convertUsable(id, uppcode, false);

					calculateTimeScheduler.cancel(true);

					log.info(id + " 의 " + uppcode + " 상품 사용 종료. ");

				}
			}, 3, minute, TimeUnit.SECONDS); // [if:시간권 사용시]

		} else {
			log.info("시간권 계산 오류발생");// if-else : dto null검사 && uppcode 일치 검사
		}
	} // 전체 메소드

	@Override // 사용중인 시간권,고정석 날짜 계산=====================================================================
	public void calculateDayPass(String id, String uppcode) {
		UserPurchasedProductDTO upp = uppmapper.selectInUsedTrueUpp(id);
		int hour = 100;

		if (upp != null && upp.getUppcode().equals(uppcode) && (upp.getPtype().equals("d") || upp.getPtype().equals("f"))) { // 사용중인 uppcode 일치 검사
			uppmapper.convertCalculated(id, uppcode, true);
			log.info("convertCalculated == true ");

			calculateDayScheduler = scheduler.scheduleAtFixedRate(() -> {
				UserPurchasedProductDTO currentUpp = uppmapper.selectCalculatedTrueUpp(id); // ★최신 상태조회를 반드시 해야함★usedUppDto를 쓰면 반영이 안됨.

				if (currentUpp != null && (currentUpp.getPtype().equals("d") || currentUpp.getPtype().equals("f"))) {

					// [1. 상품 시간이 존재할 경우]====================================================
					if (currentUpp.getAvailableday() >= 1) { // 남은 시간이 존재할 경우에만.

						uppmapper.calculateUppTimeValue(id, currentUpp.getUppcode(), hour);
						log.info(id + " 의 " + uppcode + " 상품 " + currentUpp.getAvailableday() + " 실시간 차감 실행");

					} else if (currentUpp.getAvailableday() <= 0) { // 사용가능 시간을 모두 소비.
						log.info(id + " 의 " + uppcode + " 상품 " + currentUpp.getAvailableday() + "기간 만료");

						if (isUse) {
							int usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();

							seatmapper.checkOutSeat(usedSeatnum, id, uppcode);                   // 사용중인 자리 자동 체크아웃

							log.info("입실 취소해버리기~ 상품 만료 후 날려버릴 씻넘 : " + usedSeatnum);

						} else {
							log.info("입실중이 아닝네요????");
						} // if-else : 입실여부 판별

						stopCalculateScheduler(pType);

						log.info(id + " 의 " + uppcode + " 상품 사용 종료. ");

					} // if-else if 시간계산

				} else if (currentUppDto == null) {
					log.info("currentUppDto : null");
				} else {
					log.info("currentUppDto _ another Error case");
				}
			}, 4, 1, TimeUnit.SECONDS);
			//								}, 0, 24, TimeUnit.HOURS); // [if:기간권/고정석 사용시]

		} else {
			log.info("usedUppDto Error case");
		}

	} // 전체 메소드

	@Override // 지정된 날짜에 자동 작동하며 시간 차감 시작함==========================================================================
	public void afterCalculateDayPassFromStartDate(String id, String purchasedUppcode, LocalDateTime startDateTime) {
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
					stopCalculateScheduler(alreadyUsedUpp.getPtype());                                      // 사용중인 시간권 상품 시간계산 중단.

					uppmapper.convertUsable(id, purchasedUppcode, true);
					uppmapper.convertInUsed(id, purchasedUppcode, true);
					manageDayPass(id, purchasedUppcode, pType);

					seatmapper.checkInSeat(usedSeatNum, id, purchasedUppcode);
					log.info("현재 시간권 사용중 구매상품 시작날짜 도달. 시간권 사용종료 후 기간권 사용으로 변경  : " + id + purchasedUppcode + startDateTime);

				} else {

					// 시작 시간에 구매한 상품의 usable과 inused 상태를 true로 변경
					uppmapper.convertUsable(id, purchasedUppcode, true);
					uppmapper.convertInUsed(id, purchasedUppcode, true);
					manageDayPass(id, purchasedUppcode, pType);

					log.info("구매상품 시작날짜 도달. 사용 가능으로 변경 : ", id, purchasedUppcode, startDateTime);
				}

			}, cronTrigger);

		} catch (Exception e) {
			log.error("startUppDayType 오류 발생 : " + e.toString());
		}
	}

}
