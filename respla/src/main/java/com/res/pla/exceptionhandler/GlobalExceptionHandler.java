package com.res.pla.exceptionhandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler {
	// 예외가 발생했을 때 자동으로 이를 감지하고 @ExceptionHandler 메서드로 전달합니다.
	// 컨트롤러에서 예외를 명시적으로 던지거나 처리하지 않아도, @RestControllerAdvice와 @ExceptionHandler가 이를 처리합니다.
	// 어플리케이션이 시작한 후에 작동한다. 컴파일단계에서 예외가 발생하면 얘는 작동할 수 없다.

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> GeneralExceptionHandler(Exception e) {
		log.info("");
		log.info("예외 발생. 전역처리기 작동");
		log.info("");

		//		log.info("Exception class: " + e.getClass().getName());    // 어떤 류의 예외인지 정확히 파악.
		//		log.info("Exception e.getMsg : " + e.getMessage());        // 예외 메세지 출력. 
		log.info("Exception e.toStr : " + e.toString());           // 예외의 문자열 출력.
		log.info("Exception e.getCause : " + e.getCause());        // 예외 원인 출력.

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("전역처리 : Exception");
	}
}
