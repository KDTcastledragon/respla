package com.res.pla.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor // 모든값을 초기화하는 생성자 : 모든값을 초기화?????? 무엇으로? 0?
@NoArgsConstructor	// default 생성자
@Data
public class UsageHistoryDTO {
	private String uhcode;
	private String id;

	private LocalDateTime usedatetime;

	private int seatnum;

	private String actiontype;

	private String uppcode;

}
