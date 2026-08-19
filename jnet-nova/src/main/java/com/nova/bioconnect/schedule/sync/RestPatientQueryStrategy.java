package com.nova.bioconnect.schedule.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * RESTful API 查询策略 - 通过 HTTP 接口查询患者数据
 *
 * <p>启用条件：bioconnect.sync.patient.strategy=rest
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bioconnect.sync.patient.strategy", havingValue = "rest", matchIfMissing = false)
public class RestPatientQueryStrategy implements PatientQueryStrategy {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RestPatientQueryStrategy(
            @Value("${bioconnect.sync.patient.rest-url:http://his-server:8080/api}") String baseUrl,
            @Value("${bioconnect.sync.patient.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<PatientData> fetchActivePatients() {
        log.info("REST strategy: fetching active patients...");
        try {
            String response = restClient.get()
                    .uri("/patients?status=active")
                    .retrieve()
                    .body(String.class);
            return parsePatientList(response);
        } catch (Exception e) {
            log.error("Failed to fetch active patients from REST: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<PatientData> fetchChangedPatients(LocalDateTime since) {
        log.info("REST strategy: fetching changed patients since {}...", since);
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/patients/changed")
                            .queryParam("since", since.format(DATE_FMT))
                            .build())
                    .retrieve()
                    .body(String.class);
            return parsePatientList(response);
        } catch (Exception e) {
            log.error("Failed to fetch changed patients from REST: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public PatientData fetchPatient(String patientId, String medrecNum) {
        log.info("REST strategy: fetching patient id={}, medrec={}", patientId, medrecNum);
        try {
            String response;
            if (patientId != null) {
                response = restClient.get()
                        .uri("/patients/{id}", patientId)
                        .retrieve()
                        .body(String.class);
            } else {
                response = restClient.get()
                        .uri("/patients?medrecNum={medrecNum}", medrecNum)
                        .retrieve()
                        .body(String.class);
            }
            return parsePatient(response);
        } catch (Exception e) {
            log.error("Failed to fetch patient from REST: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String strategyName() {
        return "rest";
    }

    private List<PatientData> parsePatientList(String json) {
        List<PatientData> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode patients = root.isArray() ? root : root.get("data");
            if (patients != null && patients.isArray()) {
                for (JsonNode node : patients) {
                    results.add(parsePatientNode(node));
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse patient list JSON: {}", e.getMessage(), e);
        }
        return results;
    }

    private PatientData parsePatient(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isObject()) {
                return parsePatientNode(node);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse patient JSON: {}", e.getMessage(), e);
        }
        return null;
    }

    private PatientData parsePatientNode(JsonNode node) {
        return new PatientData(
                textOrNull(node, "patientId"),
                textOrNull(node, "medrecNum"),
                textOrNull(node, "patientName"),
                textOrNull(node, "firstName"),
                textOrNull(node, "lastName"),
                textOrNull(node, "sex"),
                parseDateTime(node, "birthDate"),
                textOrNull(node, "visitNum"),
                textOrNull(node, "accountNum"),
                textOrNull(node, "visitType"),
                textOrNull(node, "location"),
                textOrNull(node, "room"),
                textOrNull(node, "bed"),
                textOrNull(node, "attendingDoctor"),
                parseDateTime(node, "admitTime"),
                parseDateTime(node, "dischargeTime"),
                textOrNull(node, "status"),
                textOrNull(node, "facility"),
                textOrNull(node, "idCard"),
                textOrNull(node, "phone"),
                parseDateTime(node, "lastUpdateTime")
        );
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    private LocalDateTime parseDateTime(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        try {
            return LocalDateTime.parse(v.asText(), DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
