package com.vam.hassan.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ProductExceptionController {

	@ExceptionHandler(value = ProductNotfoundException.class)
	public ResponseEntity<Object> handleProductNotFoundException(ProductNotfoundException exception) {
		return new ResponseEntity<>("Product Not found", HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(value = EmployeeNotfoundException.class)
	public ResponseEntity<Object> handleEmployeeNotFoundException(EmployeeNotfoundException exception) {
		return new ResponseEntity<>("Employee Not found", HttpStatus.NOT_FOUND);
	}
}
