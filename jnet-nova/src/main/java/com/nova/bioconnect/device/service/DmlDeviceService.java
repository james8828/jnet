package com.nova.bioconnect.device.service;

import com.nova.bioconnect.device.entity.DmlDevice;
import com.nova.bioconnect.device.repository.DmlDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DML Device Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DmlDeviceService {

    private final DmlDeviceRepository deviceRepository;

    /**
     * Find device by serial ID
     */
    public Optional<DmlDevice> findBySerialId(String serialId) {
        return deviceRepository.findBySerialId(serialId);
    }

    /**
     * Get all devices
     */
    public List<DmlDevice> findAll() {
        return deviceRepository.findAll();
    }

    /**
     * Save or update device
     */
    @Transactional
    public DmlDevice save(DmlDevice device) {
        return deviceRepository.save(device);
    }

    /**
     * Update or create device by serial ID
     */
    @Transactional
    public DmlDevice upsertDevice(DmlDevice device) {
        return deviceRepository.findBySerialId(device.getSerialId())
                .map(existing -> {
                    existing.setDeviceName(device.getDeviceName());
                    existing.setDeviceType(device.getDeviceType());
                    existing.setDeviceClass(device.getDeviceClass());
                    existing.setFromInstId(device.getFromInstId());
                    existing.setVendorId(device.getVendorId());
                    existing.setSwVersion(device.getSwVersion());
                    existing.setHwVersion(device.getHwVersion());
                    existing.setLocNum(device.getLocNum());
                    existing.setFacName(device.getFacName());
                    existing.setInstNum(device.getInstNum());
                    existing.setSupportsSetTime(device.getSupportsSetTime());
                    existing.setSupportsContinuous(device.getSupportsContinuous());
                    existing.setLastCommDttm(LocalDateTime.now());
                    return deviceRepository.save(existing);
                })
                .orElseGet(() -> {
                    device.setCreatedAt(LocalDateTime.now());
                    device.setUpdatedAt(LocalDateTime.now());
                    device.setLastCommDttm(LocalDateTime.now());
                    return deviceRepository.save(device);
                });
    }

    /**
     * Delete device by serial ID
     */
    @Transactional
    public void deleteBySerialId(String serialId) {
        deviceRepository.deleteBySerialId(serialId);
    }

    /**
     * Get device count
     */
    public long count() {
        return deviceRepository.count();
    }
}