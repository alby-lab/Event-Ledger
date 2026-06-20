package com.eventledger.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class EventGatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventGatewayServiceApplication.class, args);
	}

}
