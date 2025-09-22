package com.certification.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * API调试工具类
 * 用于诊断第三方API连接问题
 */
@Slf4j
public class ApiDebugUtil {

    /**
     * 测试API端点的可访问性
     */
    public static Map<String, Object> testApiEndpoint(String baseUrl, String endpoint, String apiKey) {
        Map<String, Object> result = new HashMap<>();
        RestTemplate restTemplate = new RestTemplate();
        
        try {
            // 1. 测试基础URL
            log.info("测试基础URL: {}", baseUrl);
            try {
                ResponseEntity<String> baseResponse = restTemplate.getForEntity(baseUrl, String.class);
                result.put("baseUrlStatus", baseResponse.getStatusCode().value());
                result.put("baseUrlAccessible", true);
                log.info("基础URL可访问，状态码: {}", baseResponse.getStatusCode());
            } catch (Exception e) {
                result.put("baseUrlAccessible", false);
                result.put("baseUrlError", e.getMessage());
                log.error("基础URL不可访问: {}", e.getMessage());
            }
            
            // 2. 测试完整API端点（GET请求）
            String fullUrl = baseUrl + endpoint;
            log.info("测试API端点（GET）: {}", fullUrl);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-API-Key", apiKey);
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                
                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<String> getResponse = restTemplate.exchange(
                    fullUrl, HttpMethod.GET, entity, String.class);
                
                result.put("getRequestStatus", getResponse.getStatusCode().value());
                result.put("getRequestAccessible", true);
                log.info("GET请求成功，状态码: {}", getResponse.getStatusCode());
            } catch (Exception e) {
                result.put("getRequestAccessible", false);
                result.put("getRequestError", e.getMessage());
                log.error("GET请求失败: {}", e.getMessage());
            }
            
            // 3. 测试POST请求（空参数）
            log.info("测试API端点（POST）: {}", fullUrl);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);
                headers.set("X-API-Key", apiKey);
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                headers.set("Referer", "https://open.bcpmdata.com");
                
                Map<String, Object> emptyBody = new HashMap<>();
                emptyBody.put("page", 1);
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emptyBody, headers);
                ResponseEntity<String> postResponse = restTemplate.exchange(
                    fullUrl, HttpMethod.POST, entity, String.class);
                
                result.put("postRequestStatus", postResponse.getStatusCode().value());
                result.put("postRequestAccessible", true);
                result.put("postResponsePreview", 
                    postResponse.getBody() != null && postResponse.getBody().length() > 100 ?
                    postResponse.getBody().substring(0, 100) + "..." : postResponse.getBody());
                log.info("POST请求成功，状态码: {}", postResponse.getStatusCode());
            } catch (Exception e) {
                result.put("postRequestAccessible", false);
                result.put("postRequestError", e.getMessage());
                log.error("POST请求失败: {}", e.getMessage());
            }
            
            result.put("success", true);
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            log.error("API调试测试失败: {}", e.getMessage());
        }
        
        return result;
    }

    /**
     * 尝试不同的认证方式
     */
    public static Map<String, Object> tryDifferentAuthMethods(String url, String apiKey) {
        Map<String, Object> result = new HashMap<>();
        RestTemplate restTemplate = new RestTemplate();
        
        // 方法1: Bearer Token
        try {
            HttpHeaders headers1 = new HttpHeaders();
            headers1.setContentType(MediaType.APPLICATION_JSON);
            headers1.set("Authorization", "Bearer " + apiKey);
            
            Map<String, Object> body = new HashMap<>();
            body.put("page", 1);
            
            HttpEntity<Map<String, Object>> entity1 = new HttpEntity<>(body, headers1);
            ResponseEntity<String> response1 = restTemplate.exchange(url, HttpMethod.POST, entity1, String.class);
            
            result.put("bearerTokenMethod", "成功");
            result.put("bearerTokenStatus", response1.getStatusCode().value());
            return result;
        } catch (Exception e) {
            result.put("bearerTokenMethod", "失败: " + e.getMessage());
        }
        
        // 方法2: X-API-Key
        try {
            HttpHeaders headers2 = new HttpHeaders();
            headers2.setContentType(MediaType.APPLICATION_JSON);
            headers2.set("X-API-Key", apiKey);
            
            Map<String, Object> body = new HashMap<>();
            body.put("page", 1);
            
            HttpEntity<Map<String, Object>> entity2 = new HttpEntity<>(body, headers2);
            ResponseEntity<String> response2 = restTemplate.exchange(url, HttpMethod.POST, entity2, String.class);
            
            result.put("xApiKeyMethod", "成功");
            result.put("xApiKeyStatus", response2.getStatusCode().value());
            return result;
        } catch (Exception e) {
            result.put("xApiKeyMethod", "失败: " + e.getMessage());
        }
        
        // 方法3: 查询参数
        try {
            String urlWithKey = url + "?api_key=" + apiKey;
            HttpHeaders headers3 = new HttpHeaders();
            headers3.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("page", 1);
            
            HttpEntity<Map<String, Object>> entity3 = new HttpEntity<>(body, headers3);
            ResponseEntity<String> response3 = restTemplate.exchange(urlWithKey, HttpMethod.POST, entity3, String.class);
            
            result.put("queryParamMethod", "成功");
            result.put("queryParamStatus", response3.getStatusCode().value());
            return result;
        } catch (Exception e) {
            result.put("queryParamMethod", "失败: " + e.getMessage());
        }
        
        result.put("allMethodsFailed", true);
        return result;
    }
}
