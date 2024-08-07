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
		log.info("리부트 정상화 해줫짢아 씨발꺼" + pType);
		return uppmapper.selectUsableOneUppByIdPType(id, pType);
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
		UserPurchasedProductDTO usedUpp = selectCalculatedTrueUpp(id);                                     // 현재 사용중인 상품(전체를 불러오면 너무 많다.)

		//==[1. 기간권 , 고정석 상품을 사용중일 때 ]=================================================================		
		if (usedUpp != null && (usedUpp.getPtype().equals("d") || usedUpp.getPtype().equals("f"))) {
			// 사용중인 상품의 시작날짜 < (구매상품 시작날짜 || 구매상품 종료날짜) : 이 명제는 항상 참이기 때문에, usedUppstartDateTime 경우는 따지지 않는다.
			log.info("1. 기간권 / 고정석 상품을 사용중일 때.");

			LocalDateTime usedUppEndDateTime = usedUpp.getEnddate();

			if (usedUppEndDateTime != null && usedUppEndDateTime.isAfter(startDateTime)) {
				log.info("기간충돌 (구매 상품의 시작날짜 < 사용중인 상품의 종료날짜) : " + startDateTime + " < " + usedUppEndDateTime);

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
					return true;
				}

			} // for

		} // if-else

		log.info("기간 충돌 검사 결과 : 충돌 없음.");
		return false;
	}

	//====[시간/기간 계산 종료]==================================================================================================================================
	@Override
	public void stopCalculateTimePass(String id, String uppcode) {
		if (calculateTimeScheduler != null) {
			uppmapper.convertCalculated(id, uppcode, false);

			calculateTimeScheduler.cancel(true);
			log.info("시간권 상품 계산 종료");

		} else {
			log.info("calculateTimeScheduler == null");
		}
	}

	@Override
	public void endCalculateDayPass(String id, String uppcode) {
		if (calculateDayScheduler != null) {
			uppmapper.convertCalculated(id, uppcode, false);
			uppmapper.convertUsable(id, uppcode, false);

			calculateDayScheduler.cancel(true);
			log.info("기간권 상품 계산 종료");

		} else {
			log.info("calculateDayScheduler == null");
		}
	}

	//====[시간권 계산]==================================================================================================================================
	@Override
	public void calculateTimePass(String id, String uppcode) {
		UserPurchasedProductDTO upp = uppmapper.selectUppByUppcode(uppcode);
		int minute = 10;

		if (upp != null && upp.getPtype().equals("m")) {
			uppmapper.convertCalculated(id, uppcode, true);
			log.info("convertCalculated == true ");

			calculateTimeScheduler = scheduler.scheduleAtFixedRate(() -> {
				UserPurchasedProductDTO currentUpp = uppmapper.selectCalculatedTrueUpp(id); // ★최신 상태조회를 반드시 해야함★usedUppDto를 쓰면 반영이 안됨.

				// [1. 상품 시간이 존재할 경우]====================================================
				if (currentUpp.getAvailabletime() >= 1) {
					log.info("시간 계산 작동 시작");

					uppmapper.realTimeCalculateUppTimePass(id, currentUpp.getUppcode(), minute);
					log.info(id + " 의 " + uppcode + " 시간(분) 계산 성공.");
					log.info("남은시간 : " + currentUpp.getAvailabletime() + " 분 ");

				}
				// [2. 상품 시간 모두 소비.]====================================================
				else if (currentUpp.getAvailabletime() <= 0) {
					log.info(id + " 의 " + uppcode + " 시간 모두 소비 : " + currentUpp.getAvailabletime());

					int usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();

					//					seatservice.checkOutSeat(usedSeatnum, id, uppcode, "m");
					seatmapper.vacateSeat(usedSeatnum, id, uppcode);   // 자동 체크아웃1 : 좌석 비우기
					uppmapper.convertInUsed(id, uppcode, false);       // 자동 체크아웃2 : 상품 미사용
					stopCalculateTimePass(id, uppcode);                // 자동 체크아웃3 : 시간 계산 종료
					uppmapper.convertUsable(id, uppcode, false);       // 사용 불가로 전환.

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

						//						seatservice.checkInSeat(usedSeatnum, id, newUppcode, "m");
						seatmapper.occupySeat(usedSeatnum, id, newUppcode);   // 자동 체크인1 : 좌석 앉기
						uppmapper.convertInUsed(id, newUppcode, true);        // 자동 체크인2 : 상품 사용중
						calculateTimePass(id, newUppcode);                    // 자동 체크인3 : 시간 계산 시작

					} else if (existTimePass == false && existDayPass == true) {
						String newUppcode = uppmapper.selectUsableOneUppByIdPType(id, "df").getUppcode();
						log.info("새롭게 사용할 시간권 : " + newUppcode.toString());

						//						seatservice.checkInSeat(usedSeatnum, id, newUppcode, newUppcode.get~~);
						seatmapper.occupySeat(usedSeatnum, id, newUppcode);   // 자동 체크인1 : 좌석 앉기
						uppmapper.convertInUsed(id, newUppcode, true);        // 자동 체크인2 : 상품 사용중
						calculateDayPass(id, newUppcode);                     // 자동 체크인3 : 기간 계산 시작
					}

				}
			}, 5, 1, TimeUnit.SECONDS);

		} else {
			log.info("시간권 계산 오류발생");// if-else : dto null검사 && uppcode 일치 검사
		}
	} // 전체 메소드

	//====[기간권 계산]==================================================================================================================================
	@Override
	public void calculateDayPass(String id, String uppcode) {
		UserPurchasedProductDTO upp = uppmapper.selectUppByUppcode(uppcode);
		int hour = 100;

		if (upp != null && (upp.getPtype().equals("d") || upp.getPtype().equals("f"))) { // 사용중인 uppcode 일치 검사
			uppmapper.convertCalculated(id, uppcode, true);
			log.info("convertCalculated == true ");

			calculateDayScheduler = scheduler.scheduleAtFixedRate(() -> {
				UserPurchasedProductDTO currentUpp = uppmapper.selectCalculatedTrueUpp(id); // ★최신 상태조회를 반드시 해야함★usedUppDto를 쓰면 반영이 안됨.

				if (currentUpp != null && (currentUpp.getPtype().equals("d") || currentUpp.getPtype().equals("f"))) {

					// [1. 상품 시간이 존재할 경우]====================================================
					if (currentUpp.getAvailableday() >= 1) {
						log.info("기간 계산 작동 시작");

						uppmapper.realTimeCalculateUppDayPass(id, currentUpp.getUppcode(), hour);
						log.info(id + " 의 " + uppcode + " 기간(시간) 계산 성공.");
						log.info("남은기간 : " + currentUpp.getAvailableday() + " 시간 ");

						// [2. 상품 기간 종료.]====================================================
					} else if (currentUpp.getAvailableday() <= 0) { // 사용가능 시간을 모두 소비.
						log.info(id + " 의 " + uppcode + " 기간 종료 : " + currentUpp.getAvailableday());

						//						if (isUse) {
						//							int usedSeatnum = seatmapper.selectSeatById(id).getSeatnum();
						//
						//							seatmapper.checkOutSeat(usedSeatnum, id, uppcode);                   // 사용중인 자리 자동 체크아웃
						//
						//							log.info("입실 취소해버리기~ 상품 만료 후 날려버릴 씻넘 : " + usedSeatnum);
						//
						//						} else {
						//							log.info("입실중이 아닝네요????");
						//						} // if-else : 입실여부 판별

						endCalculateDayPass(id, uppcode);

						log.info(id + " 의 " + uppcode + " 상품 사용 종료. ");

					} // if-else if 시간계산

				} else if (currentUpp == null) {
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

	//	UserPurchasedProductDTO alreadyUsedUpp = uppservice.selectI(id);       // 현재 '사용중'인 상품
	//
	//					//==[1-2-a. 현재 시간권상품을 사용하여 입실한 경우, 기간권으로 자동 변경시 ]=======================							
	//					if (alreadyUsedUpp != null && (alreadyUsedUpp.getPtype().equals("m"))) {
	//						int usedSeatNum = seatservice.selectSeatById(id).getSeatnum();     // 사용중인 좌석 번호.
	//						String alreadyUsedUppcode = alreadyUsedUpp.getUppcode();		   // 사용중인 시간권 상품의 uppcode.
	//
	//						seatservice.checkOutSeat(usedSeatNum, id, alreadyUsedUppcode);     // 시간권으로 사용중인 좌석 체크아웃.
	//						uppservice.convertInUsed(id, alreadyUsedUppcode, false);           // 사용중인 시간권 상품 사용안함으로 전환.
	//						uppservice.convertCalculated(id, alreadyUsedUppcode, false);       // 사용중인 시간권 시간계산 중단.
	//						uppservice.stopCalculateScheduler(alreadyUsedUpp.getPtype());                           // 사용중인 시간권 상품 시간계산 중단.
	//
	//						uppservice.convertUsable(id, purchasedUppcode, true);              // 구매한 기간권 상품 사용가능 전환
	//						uppservice.convertCalculated(id, purchasedUppcode, true);              // 구매한 기간권 상품 사용가능 전환
	//						uppservice.convertInUsed(id, purchasedUppcode, true);              // 구매한 기간권 상품 사용가능 전환
	//						uppservice.manageDayPass(id, purchasedUppcode, pType);            // 구매한 기간권 상품 시간계산 시작.
	//
	//						seatservice.checkInSeat(usedSeatNum, id, purchasedUppcode);        // 구매한 기간권으로 같은 좌석 재 체크인.
	//						log.info("시간권 체크아웃 & 기간권 즉시 변경 후 재 체크인");
	//
	//					}
	//
	//					//==[1-2-b. 체크인에 사용중인 시간권 상품 없을 시   ]=======================	
	//					else {
	//						uppservice.manageDayPass(id, purchasedUppcode);
	//						log.info("기간권 (즉시 사용가능) : " + id + " / " + purchasedUppcode);        // 재 체크인 때문에 코드 중복 사용.
	//					}

	@Override // 지정된 날짜에 자동 작동하며 시간 차감 시작함==========================================================================
	public void afterCalculateDayPassFromStartDate(String id, String purchasedUppcode, LocalDateTime startDateTime) {
		try {
			String cronStartDateTime = String.format("%d %d %d %d %d ?", startDateTime.getSecond(), startDateTime.getMinute(), startDateTime.getHour(), startDateTime.getDayOfMonth(), startDateTime.getMonthValue());
			CronTrigger cronTrigger = new CronTrigger(cronStartDateTime);

			dayTaskScheduler.schedule(() -> {
				log.info("예약구매한 기간권 스케줄러 작동 시작시간 : " + cronStartDateTime);
				uppmapper.convertUsable(id, purchasedUppcode, true);
				calculateDayPass(id, purchasedUppcode);
				log.info("예약구매 기간권 시간 계산 작동.");

			}, cronTrigger);

		} catch (Exception e) {
			log.error("startUppDayType 오류 발생 : " + e.toString());
		}
	} // 전체 메소드

	//	UserPurchasedProductDTO alreadyUsedUpp = uppmapper.selectInUsedUppOnlyThing(id);       // 현재 '사용중'인 상품
	//				log.info("alreadyUsedUpp가 과연 null일까요?? 시발꺼?" + alreadyUsedUpp);
	//
	//				if ((alreadyUsedUpp.getPtype().equals("m"))) {
	//					log.info("시간권 사용중일 경우, 예약구매 기간권으로 변경 후 계산 준비 시작");
	//
	//					int usedSeatNum = seatmapper.selectSeatById(id).getSeatnum();     // 사용중인 좌석 번호.
	//					String alreadyUsedUppcode = alreadyUsedUpp.getUppcode();		   // 사용중인 시간권 상품의 uppcode.
	//
	//					seatmapper.checkOutSeat(usedSeatNum, id, alreadyUsedUppcode);      // 시간권으로 사용중인 좌석 체크아웃.
	//					uppmapper.convertInUsed(id, alreadyUsedUppcode, false);            // 사용중인 시간권 상품 사용상태 전환.
	//					stopCalculateScheduler(alreadyUsedUpp.getPtype());                                      // 사용중인 시간권 상품 시간계산 중단.
	//
	//					uppmapper.convertUsable(id, purchasedUppcode, true);
	//					uppmapper.convertInUsed(id, purchasedUppcode, true);
	//					manageDayPass(id, purchasedUppcode, pType);
	//
	//					seatmapper.checkInSeat(usedSeatNum, id, purchasedUppcode);
	//					log.info("현재 시간권 사용중 구매상품 시작날짜 도달. 시간권 사용종료 후 기간권 사용으로 변경  : " + id + purchasedUppcode + startDateTime);
	//
	//				} else {
	//
	//					// 시작 시간에 구매한 상품의 usable과 inused 상태를 true로 변경
	//					uppmapper.convertUsable(id, purchasedUppcode, true);
	//					uppmapper.convertInUsed(id, purchasedUppcode, true);
	//					manageDayPass(id, purchasedUppcode, pType);
	//
	//					log.info("구매상품 시작날짜 도달. 사용 가능으로 변경 : ", id, purchasedUppcode, startDateTime);
	//				}

}
