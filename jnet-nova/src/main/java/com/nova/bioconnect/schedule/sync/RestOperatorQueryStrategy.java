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
 * RESTful API 查询策略 - 通过 HTTP 接口查询医护人员数据
 *
 * <p>启用条件：bioconnect.sync.operator.strategy=rest
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "bioconnect.sync.operator.strategy", havingValue = "rest", matchIfMissing = false)
public class RestOperatorQueryStrategy implements OperatorQueryStrategy {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public RestOperatorQueryStrategy(
            @Value("${bioconnect.sync.operator.rest-url:http://his-server:8080/api}") String baseUrl,
            @Value("${bioconnect.sync.operator.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<OperatorData> fetchActiveOperators() {
        log.info("REST strategy: fetching active operators...");
        try {
            String response = restClient.get()
                    .uri("/operators?status=active")
                    .retrieve()
                    .body(String.class);
            return parseOperatorList(response);
        } catch (Exception e) {
            log.error("Failed to fetch active operators from REST: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<OperatorData> fetchChangedOperators(LocalDateTime since) {
        log.info("REST strategy: fetching changed operators since {}...", since);
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/operators/changed")
                            .queryParam("since", since.format(DATE_FMT))
                            .build())
                    .retrieve()
                    .body(String.class);
            return parseOperatorList(response);
        } catch (Exception e) {
            log.error("Failed to fetch changed operators from REST: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public OperatorData fetchOperator(String operatorId) {
        log.info("REST strategy: fetching operator id={}", operatorId);
        try {
            String response = restClient.get()
                    .uri("/operators/{id}", operatorId)
                    .retrieve()
                    .body(String.class);
            return parseOperator(response);
        } catch (Exception e) {
            log.error("Failed to fetch operator from REST: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String strategyName() {
        return "rest";
    }

    private List<OperatorData> parseOperatorList(String json) {
        List<OperatorData> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode operators = root.isArray() ? root : root.get("data");
            if (operators != null && operators.isArray()) {
                for (JsonNode node : operators) {
                    results.add(parseOperatorNode(node));
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse operator list JSON: {}", e.getMessage(), e);
        }
        return results;
    }

    private OperatorData parseOperator(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isObject()) {
                return parseOperatorNode(node);
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse operator JSON: {}", e.getMessage(), e);
        }
        return null;
    }

    private OperatorData parseOperatorNode(JsonNode node) {
        return new OperatorData(
                textOrNull(node, "operatorId"),
                textOrNull(node, "operatorName"),
                textOrNull(node, "firstName"),
                textOrNull(node, "lastName"),
                textOrNull(node, "department"),
                textOrNull(node, "location"),
                textOrNull(node, "locNum"),
                textOrNull(node, "title"),
                textOrNull(node, "privilegeLevel"),
                boolOrFalse(node, "isSupervisor"),
                textOrNull(node, "email"),
                textOrNull(node, "phone"),
                textOrNull(node, "status"),
                textOrNull(node, "facility"),
                parseDateTime(node, "effectiveStart"),
                parseDateTime(node, "effectiveEnd"),
                parseDateTime(node, "lastUpdateTime")
        );
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    private boolean boolOrFalse(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v != null && !v.isNull() && v.asBoolean(false);
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
