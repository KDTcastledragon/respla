package com.res.pla.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor // 모든값을 초기화하는 생성자 : 모든값을 초기화?????? 무엇으로? 0?
@NoArgsConstructor	// default 생성자
@Data
public class UserPurchasedProductDTO {

	private String uppcode;
	private LocalDateTime purchasedate;

	private String id;
	private int productcode;
	private String ptype;

	private int initialtimevalue;
	private int usedtime;
	private int availabletime;

	private LocalDateTime startdate;
	private int initialdayvalue;
	private int usedday;
	private int availableday;
	private LocalDateTime enddate;

	private int price;

	private boolean inused;
	private boolean calculated;
	private boolean usable;

	private String payment;
	private int extratime;
	private int extraday;
	private boolean refunded;

}
