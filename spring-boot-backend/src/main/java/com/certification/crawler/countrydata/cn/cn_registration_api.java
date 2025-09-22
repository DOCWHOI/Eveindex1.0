package com.certification.crawler.countrydata.cn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中国上市医疗器械爬虫
 * 
 * API文档: https://open.bcpmdata.com/api/171.html
 * 接入指南: https://open.bcpmdata.com/guide/1.html
 * 
 * 功能：通过产品备案/注册证号实时调取全国药品上市信息数据
 * 覆盖：全国31个省级行政区、100+地市级药监部门
 * 
 * @author Generated
 * @date 2025-09-22
 */
@Slf4j
@Component
public class cn_registration_api {

    // API配置
    private static final String API_KEY = "sk-acbc13b3d4ea353caa2142be3e22157a282dabbf";
    private static final String BASE_URL = "https://open.bcpmdata.com";
    private static final String LIST_ENDPOINT = "/instrument/general/v1/china_listed/list";
    private static final String KEY_EXPIRY = "2025-09-22 23:59:59";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public cn_registration_api() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取中国上市医疗器械列表
     * 
     * @param searchParams 搜索参数
     * @param page 页码（默认每页10条）
     * @return 医疗器械列表响应
     */
    public ChinaListedResponse getChinaListedDevices(SearchParams searchParams, int page) {
        log.info("开始获取中国上市医疗器械数据，页码: {}", page);
        
        try {
            // 检查API Key是否过期
            if (isApiKeyExpired()) {
                log.error("API Key已过期，过期时间: {}", KEY_EXPIRY);
                return null;
            }

            // 构建请求
            String url = BASE_URL + LIST_ENDPOINT;
            HttpHeaders headers = createHeaders();
            
            // 构建请求体
            Map<String, Object> requestBody = buildRequestBody(searchParams, page);
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            log.info("发送请求到: {}", url);
            log.info("请求参数: {}", objectMapper.writeValueAsString(requestBody));
            
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                log.info("API响应成功，响应长度: {}", responseBody != null ? responseBody.length() : 0);
                
                // 解析响应
                ChinaListedResponse result = objectMapper.readValue(responseBody, ChinaListedResponse.class);
                log.info("成功解析数据，共 {} 条记录", result.getTotal());
                
                return result;
            } else {
                log.error("API请求失败，状态码: {}", response.getStatusCode());
                return null;
            }
            
        } catch (Exception e) {
            log.error("获取中国上市医疗器械数据失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 批量获取所有数据
     */
    public ChinaListedResponse getAllChinaListedDevices(SearchParams searchParams, int maxPages) {
        log.info("开始批量获取中国上市医疗器械数据，最大页数: {}", maxPages);
        
        ChinaListedResponse allResults = new ChinaListedResponse();
        allResults.setList(new java.util.ArrayList<>());
        
        int currentPage = 1;
        int totalProcessed = 0;
        
        while (currentPage <= maxPages) {
            ChinaListedResponse pageResult = getChinaListedDevices(searchParams, currentPage);
            
            if (pageResult == null || pageResult.getList() == null || pageResult.getList().isEmpty()) {
                log.info("第 {} 页没有数据，停止获取", currentPage);
                break;
            }
            
            allResults.getList().addAll(pageResult.getList());
            allResults.setTotal(pageResult.getTotal());
            
            totalProcessed += pageResult.getList().size();
            log.info("已处理第 {} 页，本页 {} 条，累计 {} 条", 
                currentPage, pageResult.getList().size(), totalProcessed);
            
            if (pageResult.getList().size() < 10) {
                break;
            }
            
            currentPage++;
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log.info("批量获取完成，总计 {} 条记录", totalProcessed);
        return allResults;
    }

    /**
     * 根据产品名称搜索
     */
    public ChinaListedResponse searchByProductName(String productName, int page) {
        SearchParams params = new SearchParams();
        params.setProductName(productName);
        return getChinaListedDevices(params, page);
    }

    /**
     * 测试API连接
     */
    public boolean testApiConnection() {
        log.info("测试API连接...");
        try {
            SearchParams params = new SearchParams();
            ChinaListedResponse response = getChinaListedDevices(params, 1);
            boolean success = response != null && response.getList() != null;
            log.info("API连接测试结果: {}", success ? "成功" : "失败");
            return success;
        } catch (Exception e) {
            log.error("API连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建HTTP请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.set("User-Agent", "ChinaRegistrationCrawler/1.0");
        return headers;
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(SearchParams searchParams, int page) {
        Map<String, Object> requestBody = new HashMap<>();
        
        if (searchParams != null) {
            Map<String, Object> search = new HashMap<>();
            
            if (searchParams.getProductName() != null) {
                search.put("product_name", searchParams.getProductName());
            }
            if (searchParams.getManufacturerRe() != null) {
                search.put("manufacturer_re", searchParams.getManufacturerRe());
            }
            if (searchParams.getRegistrationNumberRemark() != null) {
                search.put("registration_number_remark", searchParams.getRegistrationNumberRemark());
            }
            if (searchParams.getCategory() != null) {
                search.put("category", searchParams.getCategory());
            }
            if (searchParams.getType() != null) {
                search.put("type", searchParams.getType());
            }
            
            if (!search.isEmpty()) {
                requestBody.put("search", search);
            }
        }
        
        requestBody.put("page", page);
        return requestBody;
    }

    /**
     * 检查API Key是否过期
     */
    private boolean isApiKeyExpired() {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime expiryTime = LocalDateTime.parse(KEY_EXPIRY, formatter);
            return LocalDateTime.now().isAfter(expiryTime);
        } catch (Exception e) {
            log.warn("无法解析API Key过期时间，假设未过期");
            return false;
        }
    }

    // ==================== 数据模型类 ====================

    /**
     * 搜索参数类
     */
    @Data
    public static class SearchParams {
        private String productName;              // 产品名称
        private String manufacturerRe;           // 注册/备案人名称
        private String registrationNumberRemark; // 产品备案/注册证号
        private String category;                 // 管理类别：Ⅱ、Ⅲ
        private String type;                     // 国产/进口：国产、进口
        private String productState;             // 器械状态：已注销、已过期、有效
        private String whetherYibao;             // 是否纳入医保：是、否
    }

    /**
     * API响应类
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChinaListedResponse {
        private List<MedicalDevice> list;
        private Integer total;
    }

    /**
     * 医疗器械数据类
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MedicalDevice {
        private String keyid;
        
        @JsonProperty("product_name")
        private String productName;
        
        @JsonProperty("registration_number")
        private String registrationNumber;
        
        @JsonProperty("manufacturer_re")
        private String manufacturerRe;
        
        private String category;
        
        @JsonProperty("scope_and_use")
        private String scopeAndUse;
        
        @JsonProperty("approval_date")
        private String approvalDate;
        
        @JsonProperty("valid_until")
        private String validUntil;
        
        private String type;
        
        @JsonProperty("whether_yibao")
        private String whetherYibao;
        
        @JsonProperty("product_state")
        private String productState;
        
        private String province;
        private String city;
        private String region;
    }

    /**
     * 打印设备信息（用于调试）
     */
    public void printDeviceInfo(MedicalDevice device) {
        if (device == null) return;
        
        log.info("=== 医疗器械信息 ===");
        log.info("产品名称: {}", device.getProductName());
        log.info("注册证号: {}", device.getRegistrationNumber());
        log.info("注册人: {}", device.getManufacturerRe());
        log.info("管理类别: {}", device.getCategory());
        log.info("国产/进口: {}", device.getType());
        log.info("适用范围: {}", device.getScopeAndUse());
        log.info("批准日期: {}", device.getApprovalDate());
        log.info("有效期至: {}", device.getValidUntil());
        log.info("器械状态: {}", device.getProductState());
        log.info("是否纳入医保: {}", device.getWhetherYibao());
        log.info("==================");
    }
}