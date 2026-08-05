package com.nova.bioconnect.device.controller;

import com.nova.bioconnect.device.protocol.DmlTcpServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * DML Server Management Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/dml/server")
@RequiredArgsConstructor
public class DmlServerController {

    private final DmlTcpServer tcpServer;

    /**
     * Get server status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("port", tcpServer.getPort());
        status.put("running", tcpServer.isRunning());
        status.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }
}