package com.eventledger.account.controller;

import java.sql.Connection;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventledger.account.dto.HealthResponse;

@RestController
public class HealthController {

	private static final Logger log = LoggerFactory.getLogger(HealthController.class);

	private final DataSource dataSource;

	public HealthController(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@GetMapping("/health")
	public ResponseEntity<HealthResponse> health() {
		try (Connection conn = dataSource.getConnection()) {
			if (conn.isValid(2)) {
				return ResponseEntity.ok(new HealthResponse("UP", "account-service"));
			}
			return ResponseEntity.status(503).body(new HealthResponse("DOWN", "account-service"));
		} catch (Exception e) {
			log.error("Health check failed: {}", e.getMessage());
			return ResponseEntity.status(503).body(new HealthResponse("DOWN", "account-service"));
		}

	}
}
