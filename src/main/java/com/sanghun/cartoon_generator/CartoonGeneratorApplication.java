package com.sanghun.cartoon_generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CartoonGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(CartoonGeneratorApplication.class, args);
	}

}
