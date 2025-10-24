package com.univ.memoir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableAspectJAutoProxy
public class MemoirApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemoirApplication.class, args);
	}

}
