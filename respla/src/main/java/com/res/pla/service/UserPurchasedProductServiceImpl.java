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
import com.res.pla.mapper.UsageHistoryMapper;
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
	UsageHistoryMapper usgmapper;

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
	public List<UserPurchasedProductDTO> selectAllUsableUppsByIdPType(String id, String pType) {
		return uppmapper.selectAllUsableUppsByIdPType(id, pType);
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
	public UserPurchasedProductDTO selectUsableOneUppByIdPType(String id, String pType) {
		log.info("");
		log.info("리부트 정상화 해줫짢아 씨발꺼" + pType);
		return uppmapper.selectUsableOneUppByIdPType(id, pType);
	}

	@Override
	public boolean convertInUsed(String id, String uppcode, boolean inused) {
		int updatedresult = uppmapper.convertInUsed(id, uppcode, inused);
		return updatedresult > 0;
	}

	//	@Override
	//	public boolean convertCalculated(String id, String uppcode, boolean calculated) {
	//		int updatedresult = uppmapper.convertCalculated(id, uppcode, calculated);
	//		return updatedresult > 0;
	//	}

	@Override
	public boolean convertUsable(String id, String uppcode, boolean usable) {
		int updatedresult = uppmapper.convertUsable(id, uppcode, usable);
		return updatedresult > 0;
	}

	@Override
	public boolean isDateConflict(String id, LocalDateTime startDateTime, LocalDateTime endDateTime) {
		log.info("");
		List<UserPurchasedProductDTO> uppList = selectAfterStartDateUppsById(id, startDateTime);           // 지정시작날짜보다 후일인 상품목록들(전체를 불러오면 너무 많다.)
		UserPurchasedProductDTO usedUpp = selectCalculatedTrueUpp(id);                                     // 현재 사용중인 상품(전체를 불러오면 너무 많다.)

		//==[1. 기간권 , 고정석 상품을 사용중일 때 ]=================================================================		
		if (usedUpp != null && (usedUpp.getPtype().equals("d") || usedUpp.getPtype().equals("f"))) {
			// 사용중인 상품의 시작날짜 < (구매상품 시작날짜 || 구매상품 종료날짜) : 이 명제는 항상 참이기 때문에, usedUppstartDateTime 경우는 따지지 않는다.
			log.info("1. 기간권 / 고정석 상품을 사용중일 때.");

			LocalDateTime usedUppEndDateTime = usedUpp.getEnddate();

			if (usedUppEndDateTime != null && usedUppEndDateTime.isAfter(startDateTime)) {
				log.info("기간충돌 (구매 상품의 시작날짜 < 사용중인 상품의 종료날짜) : " + startDateTime + " < " + usedUppEndDateTime);

				log.info("");
				return true;
			}

		}

		//==[2. 시간권 상품 사용 || 사용중인 상품 없을때 ]=================================================================	
		else {
			log.info("2. 시간권 상품 보유중 || 사용가능한 상품 없을때 .");

			for (UserPurchasedProductDTO upp : uppList) {
				LocalDateTime uppStartDateTime = upp.getStartdate();
				LocalDateTime uppEndDateTime = upp.getEnddate();

				// 구매 상품의 종료날짜 =< 미리 구매한 상품의 시작 날짜 : 이 조건만 만족하면 되기 때문에, 여집합을 필터링 조건으로 설정한다.

				if (endDateTime.isAfter(uppStartDateTime)) {
					log.info("기간충돌 ( 사용예정 상품의 시작날짜 < 구매 상품의 종료날짜 ) : " + uppEndDateTime + " < " + endDateTime);
					log.info("");

					return true;
				}

			} // for

		} // if-else

		log.info("기간 충돌 검사 결과 : 충돌 없음.");
		log.info("");

		return false;
	}

	//====[시간/기간 계산 종료]==================================================================================================================================

	public boolean isSchedulerOperating(String id, String uppcode) {
		boolean isCalculated = calculateTimeScheduler != null && !calculateTimeScheduler.isCancelled() && !calculateTimeScheduler.isDone();
		uppmapper.convertCalculated(id, uppcode, isCalculated);

		return isCalculated;
	}

	@Override
	public void stopCalculateTimePass(String id, String uppcode) {
		log.info("");

		if (calculateTimeScheduler != null) {
			//			uppmapper.convertCalculated(id, uppcode, false);

			calculateTimeScheduler.cancel(true);
			log.info("시간권 상품 계산 종료");

		} else {
			log.info("calculateTimeScheduler == null");
		}

		log.info("");
	}

	@Override
	public void endCalculateDayPass(String id, String uppcode) {
		log.info("");

		if (calculateDayScheduler != null) {
			uppmapper.convertUsable(id, uppcode, false);
			calculateDayScheduler.cancel(true);
			isSchedulerOperating(id, uppcode);
			log.info("기간권 상품 계산 종료");

		} else {
			log.info("calculateDayScheduler == null");
		}

		log.info("");
	}

	//====[시간권 계산]==================================================================================================================================
	@Override
	public void calculateTimePass(String id, String uppcode) {
		log.info("");

		int minute = 1;
		UserPurchasedProductDTO upp = uppmapper.selectUppByUppcode(uppcode);

		if (upp != null && upp.getPtype().equals("m")) {

			calculateTimeScheduler = scheduler.scheduleAtFixedRate(() -> {
				boolean iscal2 = isSchedulerOperating(id, uppcode);
				log.info("스케줄러 계산중 : " + iscal2);
				UserPurchasedProductDTO currentUpp = uppmapper.selectUppByUppcode(uppcode); // ★최신 상태조회를 반드시 해야함★usedUppDto를 쓰면 반영이 안됨.

				// [1. 상품 시간이 존재할 경우]====================================================
				if (currentUpp.getAvailabletime() >= 1) {
					log.info("시간 계산 작동 시작");

					uppmapper.realTimeCalculateUppTimePass(id, currentUpp.getUppcode(), minute);
					log.info(id + " 의 " + uppcode + " 시간(분) 차감");
					log.info("남은시간 : " + currentUpp.getAvailabletime() + " 분 ");
					log.info("");

				}
				// [2. 상품 시간 모두 소비.]====================================================
				else if (currentUpp.getAvailabletime() <= 0) {
					log.info(id + " 의 " + uppcode + " 시간 모두 소비 : " + currentUpp.getAvailabletime());

					int usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();

					seatmapper.vacateSeat(usedSeatnum, id, uppcode);   // 자동 체크아웃1 : 좌석 비우기
					uppmapper.convertInUsed(id, uppcode, false);       // 자동 체크아웃2 : 상품 미사용
					stopCalculateTimePass(id, uppcode);                // 자동 체크아웃3 : 시간 계산 종료
					uppmapper.convertUsable(id, uppcode, false);       // 사용 불가로 전환.
					usgmapper.recordAction(id, usedSeatnum, "autoOut", uppcode);

					log.info(id + " 의 " + uppcode + " 시간권 상품 사용 종료 및 자동 체크아웃. ");

					//====[사용가능한 시간권이 존재할 경우, 자동으로 다른 시간권으로 교체]===========================================================
					List<UserPurchasedProductDTO> usableList = uppmapper.selectAllUsableUppsById(id);

					boolean existTimePass = usableList.stream().anyMatch(listDto -> "m".equals(listDto.getPtype()));
					boolean existDayPass = usableList.stream().anyMatch(listDto -> "d".equals(listDto.getPtype()) || "f".equals(listDto.getPtype()));

					if (existTimePass == true && existDayPass == false) {
						log.info("가장 먼저 구매했던 시간권으로 교체작업 할거다.");

						//						UserPurchasedProductDTO newUpp = uppmapper.selectUsableOneUppByIdPType(id, "m");
						String newUppcode = uppmapper.selectUsableOneUppByIdPType(id, "m").getUppcode();
						log.info("새롭게 사용할 시간권 : " + newUppcode.toString());

						seatmapper.occupySeat(usedSeatnum, id, newUppcode);   // 자동 체크인1 : 좌석 앉기
						uppmapper.convertInUsed(id, newUppcode, true);        // 자동 체크인2 : 상품 사용중
						usgmapper.recordAction(id, usedSeatnum, "autoIn", uppcode);
						calculateTimePass(id, newUppcode);                    // 자동 체크인3 : 시간 계산 시작

					} else if (existTimePass == false && existDayPass == true) {
						String newUppcode = uppmapper.selectUsableOneUppByIdPType(id, "df").getUppcode();
						log.info("새롭게 사용할 기간권 : " + newUppcode.toString());

						seatmapper.occupySeat(usedSeatnum, id, newUppcode);   // 자동 체크인1 : 좌석 앉기
						uppmapper.convertInUsed(id, newUppcode, true);        // 자동 체크인2 : 상품 사용중
						usgmapper.recordAction(id, usedSeatnum, "autoIn", uppcode);
						calculateDayPass(id, newUppcode);                     // 자동 체크인3 : 기간 계산 시작
					}

				}

				log.info("");
			}, 60, 60, TimeUnit.SECONDS);

		} else {
			log.info("시간권 계산 오류발생");// if-else : dto null검사 && uppcode 일치 검사
		}
	} // 전체 메소드

	//====[기간권 계산]==================================================================================================================================
	@Override
	public void calculateDayPass(String id, String uppcode) {
		log.info("");
		int hour = 100;

		UserPurchasedProductDTO upp = uppmapper.selectUppByUppcode(uppcode);

		if (upp != null && (upp.getPtype().equals("d") || upp.getPtype().equals("f"))) {
			log.info("후훗...기간권이군...오랜만이다 이 감각...");

			boolean isUserCheckined = seatmapper.isUserCurrentlyCheckedIn(id);
			UserPurchasedProductDTO calculatedPass = uppmapper.selectCalculatedTrueUpp(id);

			if (isUserCheckined != true && calculatedPass != null && calculatedPass.getPtype().equals("m")) {
				String calPassUppcode = calculatedPass.getUppcode();
				log.info("이런이런,,,시간권을 사용중이었단 말이지???????????? 안되겠군! 퇴실. 진행시켜!!");
				int usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();

				seatmapper.vacateSeat(usedSeatnum, id, calPassUppcode);   // 자동 체크아웃1 : 좌석 비우기
				uppmapper.convertInUsed(id, calPassUppcode, false);       // 자동 체크아웃2 : 상품 미사용
				stopCalculateTimePass(id, calPassUppcode);                // 자동 체크아웃3 : 시간 계산 종료
				usgmapper.recordAction(id, usedSeatnum, "autoOutSuspend", calPassUppcode);

				seatmapper.occupySeat(usedSeatnum, id, uppcode);   // 자동 체크인1 : 좌석 앉기
				uppmapper.convertInUsed(id, uppcode, true);        // 자동 체크인2 : 상품 사용중
				usgmapper.recordAction(id, usedSeatnum, "autoIn", uppcode);
			}

			//			uppmapper.convertCalculated(id, uppcode, true);
			//			log.info("convertCalculated == true : {} ", upp.getPtype());

			calculateDayScheduler = scheduler.scheduleAtFixedRate(() -> {
				LocalDateTime currentDateTime = LocalDateTime.now();
				UserPurchasedProductDTO currentUpp = uppmapper.selectCalculatedTrueUpp(id); // ★최신 상태조회를 반드시 해야함★usedUppDto를 쓰면 반영이 안됨.

				if (currentUpp != null && (currentUpp.getPtype().equals("d") || currentUpp.getPtype().equals("f"))) {

					// [1. 상품 시간이 존재할 경우]====================================================
					if (currentUpp.getAvailableday() >= 1) {
						log.info("기간 계산 작동 시작 : " + currentDateTime);

						uppmapper.realTimeCalculateUppDayPass(id, currentUpp.getUppcode(), hour);
						log.info(id + " 의 " + uppcode + " 기간(시간) 계산 성공.");
						log.info("남은기간 : " + currentUpp.getAvailableday() + " 시간 ");
						log.info("");

						// [2. 상품 기간 종료.]====================================================
					} else if (currentUpp.getAvailableday() <= 0) { // 사용가능 시간을 모두 소비.
						log.info(id + " 의 " + uppcode + " 기간 종료 : " + currentUpp.getAvailableday());

						Integer usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();

						seatmapper.vacateSeat(usedSeatnum, id, uppcode);   // 자동 체크아웃1 : 좌석 비우기
						uppmapper.convertInUsed(id, uppcode, false);       // 자동 체크아웃2 : 상품 미사용
						endCalculateDayPass(id, uppcode);
						uppmapper.convertUsable(id, uppcode, false);       // 사용 불가로 전환.
						usgmapper.recordAction(id, usedSeatnum, "autoOut", uppcode);

						log.info(id + " 의 " + uppcode + " 상품 사용 종료. ");
						log.info("");

					} // if-else if 시간계산

				} else if (currentUpp == null) {
					log.info("currentUppDto : null");

				} else {
					log.info("currentUppDto _ another Error case");
				}
			}, 10, 10, TimeUnit.SECONDS);

		} else {
			log.info("usedUppDto Error case");
		}

	} // 전체 메소드

	@Override // [지정된 날짜에 자동 작동하며 시간 차감 시작함]==========================================================================
	public void afterCalculateDayPassFromStartDate(String id, String purchasedUppcode, LocalDateTime startDateTime) {
		try {
			String cronStartDateTime = String.format("%d %d %d %d %d ? %d", startDateTime.getSecond(), startDateTime.getMinute(), startDateTime.getHour(), startDateTime.getDayOfMonth(), startDateTime.getMonthValue(), startDateTime.getYear());
			CronTrigger cronTrigger = new CronTrigger(cronStartDateTime);

			dayTaskScheduler.schedule(() -> {
				log.info("");
				log.info("예약구매한 기간권 스케줄러 작동 시작시간 : " + cronTrigger);

				uppmapper.convertUsable(id, purchasedUppcode, true);
				LocalDateTime currentDateTime = LocalDateTime.now();

				calculateDayPass(id, purchasedUppcode);
				log.info("예약구매 기간권 시간 계산 작동. " + currentDateTime);

			}, cronTrigger);

		} catch (Exception e) {
			log.error("startUppDayType 오류 발생 : " + e.toString());
		}
	} // 전체 메소드

}
