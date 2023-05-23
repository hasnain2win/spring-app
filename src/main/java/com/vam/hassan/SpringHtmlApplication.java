package com.vam.hassan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class SpringHtmlApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringHtmlApplication.class, args);
	}

}
