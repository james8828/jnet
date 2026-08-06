package com.nova.bioconnect.icpmgr.controller;

import com.nova.bioconnect.icpmgr.entity.DmlDevice;
import com.nova.bioconnect.icpmgr.protocol.DmlServerHandler;
import com.nova.bioconnect.icpmgr.service.DmlDeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DML Device REST API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/dml/devices")
@RequiredArgsConstructor
public class DmlDeviceController {

    private final DmlDeviceService deviceService;
    private final DmlServerHandler serverHandler;

    /**
     * Get all devices
     */
    @GetMapping
    public ResponseEntity<List<DmlDevice>> getAllDevices() {
        List<DmlDevice> devices = deviceService.findAll();
        return ResponseEntity.ok(devices);
    }

    /**
     * Get device by serial ID
     */
    @GetMapping("/{serialId}")
    public ResponseEntity<DmlDevice> getDevice(@PathVariable String serialId) {
        return deviceService.findBySerialId(serialId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update device
     */
    @PostMapping
    public ResponseEntity<DmlDevice> createDevice(@RequestBody DmlDevice device) {
        DmlDevice saved = deviceService.upsertDevice(device);
        return ResponseEntity.ok(saved);
    }

    /**
     * Delete device
     */
    @DeleteMapping("/{serialId}")
    public ResponseEntity<Void> deleteDevice(@PathVariable String serialId) {
        deviceService.deleteBySerialId(serialId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get device statistics
     */
    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDevices", deviceService.count());
        stats.put("activeSessions", serverHandler.getActiveSessionCount());
        stats.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get all active sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getSessions() {
        Map<String, Object> sessions = new HashMap<>();
        sessions.put("activeSessions", serverHandler.getAllSessions().keySet());
        sessions.put("count", serverHandler.getActiveSessionCount());
        return ResponseEntity.ok(sessions);
    }

    /**
     * Close a session by ID
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> closeSession(@PathVariable String sessionId) {
        boolean closed = serverHandler.closeSession(sessionId);
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("closed", closed);
        return ResponseEntity.ok(result);
    }
}