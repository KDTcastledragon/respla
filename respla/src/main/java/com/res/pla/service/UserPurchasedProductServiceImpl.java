package com.res.pla.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.res.pla.domain.UserPurchasedProductDTO;
import com.res.pla.mapper.SeatMapper;
import com.res.pla.mapper.UsageHistoryMapper;
import com.res.pla.mapper.UserMapper;
import com.res.pla.mapper.UserPurchasedProductMapper;
import com.res.pla.util.Fdate;

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
	public int convertInUsed(String id, String uppcode, boolean inused) {
		log.info("");
		return uppmapper.convertInUsed(id, uppcode, inused);
	}

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
				log.info("기간충돌 (구매 상품의 시작날짜 < 사용중인 상품의 종료날짜) :: {} < {} ", Fdate.chg(startDateTime), Fdate.chg(usedUppEndDateTime));

				log.info("");
				return true;
			}
		}

		//==[2. 시간권 상품 사용 || 사용중인 상품 없을때 ]=================================================================	
		else {
			if (uppList.isEmpty()) {
				log.info("예약구매한 기간권/고정석 상품 존재하지 않음. ");
				log.info("기간권/고정석 상품 구매 가능!! ");

				log.info("");
				return false;

			} else {
				log.info("예약구매한 기간권/고정석 상품 존재함:::uppList for반복 실행 ");

				for (UserPurchasedProductDTO upp : uppList) {
					LocalDateTime uppStartDateTime = upp.getStartdate();
					LocalDateTime uppEndDateTime = upp.getEnddate();

					if (uppStartDateTime != null && endDateTime.isAfter(uppStartDateTime) == true) {
						// 구매 상품의 종료날짜 =< 미리 구매한 상품의 시작 날짜 : 이 조건만 만족하면 되기 때문에, 여집합을 필터링 조건으로 설정한다.
						log.info("{} 타입의 {}주({}일) 기간 상품 이미 존재", upp.getPtype(), (upp.getInitialdayvalue() / 24 / 7), upp.getInitialdayvalue());
						log.info("기간충돌 ( 사용예정 상품의 시작날짜 < 구매 상품의 종료날짜 ) :: {} < {} ", Fdate.chg(uppEndDateTime), Fdate.chg(endDateTime));
					}

					log.info("");
					return true;
				} // for:uppList존재

			} // uppList 존재 유무

		} // if-else:기간권 사용중 여부

		log.info("기간 충돌 검사 결과 : 충돌 없음.");
		log.info("");

		return false;
	}

	//====[시간/기간 계산 종료]==================================================================================================================================

	public boolean isSchedulerOperating(String id, String uppcode) {
		boolean isCalculatedTime = (calculateTimeScheduler != null && !calculateTimeScheduler.isCancelled() && !calculateTimeScheduler.isDone());
		boolean isCalculatedDay = (calculateDayScheduler != null && !calculateDayScheduler.isCancelled() && !calculateDayScheduler.isDone());
		boolean currentlyCalculated = isCalculatedTime || isCalculatedDay;

		uppmapper.convertCalculated(id, uppcode, currentlyCalculated);

		return currentlyCalculated;
	}

	@Override
	public void stopCalculateTimePass(String id, String uppcode) {
		log.info("");

		if (calculateTimeScheduler != null) {
			calculateTimeScheduler.cancel(true);

			isSchedulerOperating(id, uppcode);

			log.info("과연 정말로 Time의 calculated가 false로 바뀌었을까요?? : " + uppmapper.selectUppByUppcode(uppcode).isCalculated());
			log.info("");
			log.info("시간권 상품 계산 종료");

		} else {
			log.info("calculateTimeScheduler == null");
		}

		log.info("");
	}

	public void endCalculateDayPass(String id, String uppcode) {
		log.info("");

		if (calculateDayScheduler != null) {
			uppmapper.convertUsable(id, uppcode, false);
			calculateDayScheduler.cancel(true);
			isSchedulerOperating(id, uppcode);

			log.info("과연 정말로 Day의 calculated가 false로 바뀌었을까요?? : " + uppmapper.selectUppByUppcode(uppcode).isCalculated());
			log.info("");
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
		int minute = 10;

		AtomicBoolean isFirstOperate = new AtomicBoolean(true);    // boolean은 익명함수 람다식에서 불가능.

		UserPurchasedProductDTO upp = uppmapper.selectUppByUppcode(uppcode);

		if (upp != null && upp.getPtype().equals("m")) {

			log.info("TimeScheduler calculated 적용전 확인 : {} ", uppmapper.selectUppByUppcode(uppcode).isCalculated());

			calculateTimeScheduler = scheduler.scheduleAtFixedRate(() -> {
				UserPurchasedProductDTO currentlyCalculatedUpp = uppmapper.selectUppByUppcode(uppcode); // ★최신 상태조회를 반드시 해야함★ 위의 upp를 쓰면 반영이 되지 않음.

				if (isFirstOperate.get()) {
					LocalDateTime firstRunTime = LocalDateTime.now();
					log.info("★★★★★TimeScheduler 최초 작동시에만 실행. 1분 후 계산 차감 실행.★★★★★★");
					log.info("★★★★★최초실행 시각 : {} ", firstRunTime);
					log.info("★★★★★TimeScheduler calculated 최초 적용 확인 : {} ", uppmapper.selectUppByUppcode(uppcode).isCalculated());

					isSchedulerOperating(id, uppcode);
					isFirstOperate.set(false);

				} else {
					LocalDateTime secondRunTime = LocalDateTime.now();
					log.info("반복 계산 실행 시작 시각 : {} ", secondRunTime);
					log.info("TimeScheduler calculated 반복 적용 확인 : {} ", uppmapper.selectUppByUppcode(uppcode).isCalculated());

					// [1. 상품 시간이 존재할 경우]====================================================
					if (currentlyCalculatedUpp.getAvailabletime() >= 1) {
						log.info("계산 전 남은 시간 : {}분 ", currentlyCalculatedUpp.getAvailabletime());

						LocalDateTime thirdRealRunTime = LocalDateTime.now();
						uppmapper.realTimeCalculateUppTimePass(id, currentlyCalculatedUpp.getUppcode(), minute);

						log.info("ㄹㅇ DB에 접근해 계산해버린 시각 : " + thirdRealRunTime);
						log.info(" {} 의 {} 상품 {}분 차감", id, uppcode, minute);
						log.info("계산 후 남은 시간 : {}분 ", uppmapper.selectUppByUppcode(uppcode).getAvailabletime());

					}
					// [2. 상품 시간 모두 소비.]====================================================
					else if (currentlyCalculatedUpp.getAvailabletime() <= 0) {
						log.info(" {} 의 {} 상품 시간 모두 소비 : {} ", id, uppcode, currentlyCalculatedUpp.getAvailabletime());

						int usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();

						seatmapper.vacateSeat(usedSeatnum, id, uppcode);   // 자동 체크아웃1 : 좌석 비우기
						uppmapper.convertInUsed(id, uppcode, false);       // 자동 체크아웃2 : 상품 미사용
						stopCalculateTimePass(id, uppcode);                // 자동 체크아웃3 : 시간 계산 종료
						uppmapper.convertUsable(id, uppcode, false);       // 사용 불가로 전환.
						usgmapper.recordAction(id, usedSeatnum, "autoOutExpiry", uppcode);

						log.info(id + " 의 " + uppcode + " 시간권 상품 사용 종료 및 자동 체크아웃...잘가라....급하면 더 사고");
						log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
						log.info("");

						shiftToNewPassFromExpiryPass(usedSeatnum, id);

					} else {
						log.info("시간권 남은 시간 검사 도중 알수없는 오류");
						throw new RuntimeException();
					} // 남은 시간 검사 (time >=1 || time <= 0)
				} // isFirstOperate 체크
				log.info("");
			}, 0, 20, TimeUnit.SECONDS);

		} else {
			log.info("시간권 계산 오류발생");// if-else : dto null검사 && uppcode 일치 검사
		}
	} // 전체 메소드

	//====[기간권 계산]==================================================================================================================================
	public void calculateDayPass(String id, String uppcode) {
		log.info("");

		int hour = 50;

		AtomicBoolean isFirstOperate = new AtomicBoolean(true);    // boolean은 익명함수 람다식에서 불가능.

		log.info("DayScheduler calculated 적용전 확인 : {} ", uppmapper.selectUppByUppcode(uppcode).isCalculated());

		calculateDayScheduler = scheduler.scheduleAtFixedRate(() -> {
			UserPurchasedProductDTO currentlyCalculatedUpp = uppmapper.selectUppByUppcode(uppcode); // ★최신 상태조회를 반드시 해야함★ usedUppDto를 쓰면 반영이 안됨.

			if (isFirstOperate.get()) {
				LocalDateTime firstRunTime = LocalDateTime.now();
				log.info("★★★★★DayScheduler 최초 작동시에만 실행. 1시간 후 계산 차감 실행.★★★★★★");
				log.info("★★★★★최초실행 시각 : " + firstRunTime);
				log.info("★★★★★DayScheduler calculated 최초 적용 확인 : {} ", uppmapper.selectUppByUppcode(uppcode).isCalculated());

				isSchedulerOperating(id, uppcode);
				isFirstOperate.set(false);

			} else {
				LocalDateTime secondRunTime = LocalDateTime.now();
				log.info("반복 계산 실행 시작 시각 : {} ", secondRunTime);
				log.info("DayScheduler calculated 반복 적용 확인 : {} ", uppmapper.selectUppByUppcode(uppcode).isCalculated());

				// [1. 상품 시간이 존재할 경우]====================================================
				if (currentlyCalculatedUpp.getAvailableday() >= 1) {
					log.info("계산 전 남은 시간 : {}시간 ", currentlyCalculatedUpp.getAvailableday());

					LocalDateTime thirdRealRunTime = LocalDateTime.now();
					uppmapper.realTimeCalculateUppDayPass(id, uppcode, hour);

					log.info("ㄹㅇ DB에 접근해 계산해버린 시각 : " + thirdRealRunTime);
					log.info(" {} 의 {} 상품 {}시간 차감", id, uppcode, hour);
					log.info("계산 후 남은 시간 : {}시간 ", uppmapper.selectUppByUppcode(uppcode).getAvailableday());
					// [2. 상품 기간 종료.]====================================================
				} else if (currentlyCalculatedUpp.getAvailableday() <= 0) { // 사용가능 시간을 모두 소비.
					log.info("{} 의 {} 상품 기간 모두 소비 : {} ", id, uppcode, currentlyCalculatedUpp.getAvailableday());

					boolean isExistOccupied = seatmapper.isExistOccupiedSeatByUserId(id);
					UserPurchasedProductDTO inUsedPass = uppmapper.selectInUsedTrueUpp(id);

					//===[입실중인 경우]===================================
					if (isExistOccupied == true && inUsedPass != null && (inUsedPass.getPtype().equals("d") || inUsedPass.getPtype().equals("d"))) {
						log.info("기간권을 이용하여 입실중이었군....후,,,,하지만..너의 기간은 끝났다!!!!! 꺼져 시발");

						int usedSeatNum = seatmapper.selectSeatById(id).getSeatnum();

						seatmapper.vacateSeat(usedSeatNum, id, uppcode);   // 자동 체크아웃1 : 좌석 비우기
						uppmapper.convertInUsed(id, uppcode, false);       // 자동 체크아웃2 : 상품 미사용
						endCalculateDayPass(id, uppcode);                  // 
						usgmapper.recordAction(id, usedSeatNum, "autoOutExpiry", uppcode);

						shiftToNewPassFromExpiryPass(usedSeatNum, id);

					} else if (isExistOccupied == false) {
						log.info("입실중이 아니었군....뭐 어쨌든.,,,,뭐 어쨌든.,,,,뭐 어쨌든.,,,,뭐 어쨌든.,,,,.너의 기간은 끝났다!");

						endCalculateDayPass(id, uppcode);

					} else {
						log.info("넌 뭐야 시발아.");
					}

					log.info("{}의 {} 상품 기간권 상품 사용 종료...잘가세요=잘가세요=잘가세요=잘가세요=잘가세요=잘가세요=잘가세요=잘가세요=잘가세요=잘가세요=", id, uppcode);
					log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

				} else {
					log.info("기간권 남은 기간 검사 도중 알수없는 오류");
					throw new RuntimeException();
				}// 남은 기간 검사 (day >=1 || day <= 0)
			} // isFirstOperate 체크
			log.info("");
		}, 0, 10, TimeUnit.SECONDS);

		log.info("");
	} // 전체 메소드

	//=========[시간권사용중, 기간권의 시작날짜가 되었을때 교체하기 위해서]========================================
	@Override
	public void validateTimePassBeforeCalculateDayPass(String id, String uppcode) {
		log.info("");

		UserPurchasedProductDTO upp = uppmapper.selectUppByUppcode(uppcode);

		if (upp != null && (upp.getPtype().equals("d") || upp.getPtype().equals("f"))) {
			log.info("후훗...기간권이군...오랜만이다 이 감각.....기꺼이 써주도록 하지. ^-^ ^오^ ^~^");
			uppmapper.convertUsable(id, uppcode, true);

			boolean isExistOccupied = seatmapper.isExistOccupiedSeatByUserId(id);
			UserPurchasedProductDTO calculatedPass = uppmapper.selectCalculatedTrueUpp(id);

			if (isExistOccupied == true && calculatedPass != null && calculatedPass.getPtype().equals("m")) {
				log.info("♥♥♥♥이런이런,,,시간권을 사용중이었단 말이지???????????? 안되겠군! 진행시켜!!");

				String calPassUppcode = calculatedPass.getUppcode();
				int usedSeatNum = seatmapper.selectSeatById(id).getSeatnum();

				seatmapper.vacateSeat(usedSeatNum, id, calPassUppcode);                     // 자동 체크아웃1 : 좌석 비우기
				uppmapper.convertInUsed(id, calPassUppcode, false);                         // 자동 체크아웃2 : 상품 미사용
				usgmapper.recordAction(id, usedSeatNum, "autoOutSuspend", calPassUppcode);  // 사용기록 : 시간권 중단
				stopCalculateTimePass(id, calPassUppcode);                                  // 자동 체크아웃3 : 시간 계산 종료

				seatmapper.occupySeat(usedSeatNum, id, uppcode);                            // 자동 체크인1 : 좌석 앉기
				uppmapper.convertInUsed(id, uppcode, true);                                 // 자동 체크인2 : 상품 사용중
				usgmapper.recordAction(id, usedSeatNum, "autoIn", uppcode);                 // 사용기록 : 기간권 입실
				calculateDayPass(id, uppcode);                                              // 시간계산 시작.

				log.info("♥♥♥♥훗,,,,시간권을 무사히 탈출시키고, 기간권으로 대체시켰다!");
				log.info("♥♥♥♥당신의 시간권! 기간권으로 대체될 것이다! 중복사용은 없다! ");

			} else {
				log.info("♥♥♥♥시간권 사용이 없군...훗....드가잇!!");
				calculateDayPass(id, uppcode);
			}

		} else if (upp == null) {
			log.info("기간권 upp : null");
		} else {
			log.info("기간권 사용하려했지만 ptype 오류");
		}
		log.info("");
	} // 전체 메소드

	//====[입실중, 사용중인 상품이 만료되었을 시에 작동]==============================================================================	
	public boolean shiftToNewPassFromExpiryPass(int usedUseatNum, String id) {
		log.info("");
		List<UserPurchasedProductDTO> usableList = uppmapper.selectAllUsableUppsById(id);

		boolean existTimePass = usableList.stream().anyMatch(listDto -> "m".equals(listDto.getPtype()));
		boolean existDayPass = usableList.stream().anyMatch(listDto -> "d".equals(listDto.getPtype()) || "f".equals(listDto.getPtype()));

		if (existDayPass == true) {
			log.info("========================================================================================");
			log.info("이런...사용가능한 기간권이 있단말인가??...이자식...꽤 탄탄하군....! 이꾸욧!");
			log.info("가장 먼저 구매했던 시간권으로 교체작업!쒸부럴 가즈아");

			String newUppcode = uppmapper.selectUsableOneUppByIdPType(id, "df").getUppcode();
			log.info("새롭게 사용할 기간권 : " + newUppcode.toString());

			seatmapper.occupySeat(usedUseatNum, id, newUppcode);   // 자동 체크인1 : 좌석 앉기
			uppmapper.convertInUsed(id, newUppcode, true);        // 자동 체크인2 : 상품 사용중
			usgmapper.recordAction(id, usedUseatNum, "autoIn", newUppcode);
			calculateDayPass(id, newUppcode);                     // 자동 체크인3 : 기간 계산 시작

		}

		else if (existTimePass == true && existDayPass == false) {
			log.info("========================================================================================");
			log.info("...사용가능한 시간권이?!?!?!?!??????????? 기간권이 없으니 시간이라도 써야지! 가즈야아이이마");
			log.info("가장 먼저 구매했던 시간권으로 교체작업! 쒸부럴 가즈아");

			String newUppcode = uppmapper.selectUsableOneUppByIdPType(id, "m").getUppcode();
			log.info("새롭게 사용할 시간권!간다잇!!! : " + newUppcode.toString());

			seatmapper.occupySeat(usedUseatNum, id, newUppcode);   // 자동 체크인1 : 좌석 앉기
			uppmapper.convertInUsed(id, newUppcode, true);        // 자동 체크인2 : 상품 사용중
			usgmapper.recordAction(id, usedUseatNum, "autoIn", newUppcode);
			calculateTimePass(id, newUppcode);                    // 자동 체크인3 : 시간 계산 시작

		} else {
			return false;
		}

		return true;
	}

	@Override // [지정된 날짜에 자동 작동하며 시간 차감 시작함]==========================================================================
	public void afterLaunchDayPassFromStartDate(String id, String purchasedUppcode, LocalDateTime startDateTime) {
		try {
			String cronStartDateTime = String.format("%d %d %d %d %d ? %d", startDateTime.getSecond(), startDateTime.getMinute(), startDateTime.getHour(), startDateTime.getDayOfMonth(), startDateTime.getMonthValue(), startDateTime.getYear());
			CronTrigger cronTrigger = new CronTrigger(cronStartDateTime);

			dayTaskScheduler.schedule(() -> {
				log.info("");
				log.info("예약구매한 기간권 스케줄러 작동 시작시간 : " + cronTrigger);

				uppmapper.convertUsable(id, purchasedUppcode, true);
				LocalDateTime currentDateTime = LocalDateTime.now();

				validateTimePassBeforeCalculateDayPass(id, purchasedUppcode);
				log.info("예약구매 기간권 시간 계산 작동. " + currentDateTime);

			}, cronTrigger);

		} catch (Exception e) {
			log.error("startUppDayType 오류 발생 : " + e.toString());
		}
	} // 전체 메소드

}
