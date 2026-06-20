package com.eventledger.gateway.controller;

import com.eventledger.gateway.dto.HealthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    
    @Autowired
    private DataSource dataSource;

    

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        
            return null;
        }
    }

