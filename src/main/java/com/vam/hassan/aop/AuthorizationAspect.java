package com.vam.hassan.aop;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class AuthorizationAspect {

	@Before("execution(* com.vam.hassan.controller.ProductServiceController.updateProduct(..))")
	public void authorizeLoanUpdate(JoinPoint joinPoint) {
		System.out.println("authorizeLoanUpdate method Before");
	}

	@After("execution(* com.vam.hassan.controller.ProductServiceController.updateProduct(..))")
	public void loggerAfter(JoinPoint joinPoint) {
		System.out.println("--------updateProduct end --------");
	}

	@AfterReturning(pointcut = "execution(* com.vam.hassan.controller.ProductServiceController.updateProduct(..))", returning = "result")
	public void logAfterReturning(JoinPoint joinPoint, Object result) {

		System.out.println("logAfterReturning() is running!");
		System.out.println("hijacked : " + joinPoint.getSignature().getName());
		System.out.println("Method returned value is : " + result);

	}

	@AfterThrowing(pointcut = "execution(* com.vam.hassan.controller.ProductServiceController.updateProduct(..)))", throwing = "error")
	public void logAfterThrowing(JoinPoint joinPoint, Throwable error) {

		System.out.println("logAfterThrowing() is running!");
		System.out.println("hijacked : " + joinPoint.getSignature().getName());
		System.out.println("Exception : " + error);
		System.out.println("******");

	}

	@Around("execution(* com.vam.hassan.controller.ProductServiceController.updateProduct(..)))")
	public void logAround(ProceedingJoinPoint joinPoint) throws Throwable {

		System.out.println("logAround() is running!");
		System.out.println("hijacked method : " + joinPoint.getSignature().getName());
		System.out.println("hijacked arguments : " + Arrays.toString(joinPoint.getArgs()));

		System.out.println("Around before is running!");
		joinPoint.proceed(); // continue on the intercepted method
		System.out.println("Around after is running!");

		System.out.println("******");

	}
}
