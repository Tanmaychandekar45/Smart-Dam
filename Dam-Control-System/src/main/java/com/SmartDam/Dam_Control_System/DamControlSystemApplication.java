package com.SmartDam.Dam_Control_System;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DamControlSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(DamControlSystemApplication.class, args);
	}

}
