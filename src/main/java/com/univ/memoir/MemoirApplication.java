package com.univ.memoir;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
		servers = {
				@Server(url = "https://memoir.asia", description = "Production server"),
				@Server(url = "http://localhost:8888", description = "Local development server")
		}
)
@SpringBootApplication
@EnableJpaAuditing
public class MemoirApplication {

	public static void main(String[] args) {
		SpringApplication.run(MemoirApplication.class, args);
	}

}
