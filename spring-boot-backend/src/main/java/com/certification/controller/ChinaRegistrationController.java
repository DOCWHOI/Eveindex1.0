package com.certification.controller;

import com.certification.crawler.countrydata.cn.cn_registration_api;
import com.certification.crawler.countrydata.cn.cn_registration_api.ChinaListedResponse;
import com.certification.crawler.countrydata.cn.cn_registration_api.MedicalDevice;
import com.certification.crawler.countrydata.cn.cn_registration_api.SearchParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 中国医疗器械注册数据控制器
 * 
 * 提供中国上市医疗器械数据的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/china-registration")
@Tag(name = "中国医疗器械注册数据", description = "中国上市医疗器械数据查询接口")
public class ChinaRegistrationController {

    @Autowired
    private cn_registration_api chinaRegistrationApi;

    /**
     * 测试API连接
     */
    @GetMapping("/test")
    @Operation(summary = "测试API连接", description = "测试中国医疗器械注册API是否可用")
    public ResponseEntity<Map<String, Object>> testConnection() {
        log.info("测试中国医疗器械注册API连接");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean isConnected = chinaRegistrationApi.testApiConnection();
            
            result.put("success", isConnected);
            result.put("message", isConnected ? "API连接成功" : "API连接失败");
            result.put("apiStats", chinaRegistrationApi.getApiStats());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("API连接测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "API连接测试异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 根据产品名称搜索医疗器械
     */
    @GetMapping("/search/product")
    @Operation(summary = "根据产品名称搜索", description = "根据产品名称搜索中国上市医疗器械")
    public ResponseEntity<Map<String, Object>> searchByProductName(
            @Parameter(description = "产品名称") @RequestParam String productName,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page) {
        
        log.info("根据产品名称搜索医疗器械: {}, 页码: {}", productName, page);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            ChinaListedResponse response = chinaRegistrationApi.searchByProductName(productName, page);
            
            if (response != null) {
                result.put("success", true);
                result.put("data", response.getList());
                result.put("total", response.getTotal());
                result.put("page", page);
                result.put("message", String.format("找到 %d 条相关记录", response.getTotal()));
                
                // 打印第一条记录的详细信息（用于调试）
                if (response.getList() != null && !response.getList().isEmpty()) {
                    chinaRegistrationApi.printDeviceInfo(response.getList().get(0));
                }
            } else {
                result.put("success", false);
                result.put("message", "搜索失败，请检查API状态");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("搜索产品失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "搜索异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 根据注册证号搜索医疗器械
     */
    @GetMapping("/search/registration")
    @Operation(summary = "根据注册证号搜索", description = "根据注册证号搜索中国上市医疗器械")
    public ResponseEntity<Map<String, Object>> searchByRegistrationNumber(
            @Parameter(description = "注册证号") @RequestParam String registrationNumber,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page) {
        
        log.info("根据注册证号搜索医疗器械: {}, 页码: {}", registrationNumber, page);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            SearchParams params = new SearchParams();
            params.setRegistrationNumberRemark(registrationNumber);
            
            ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, page);
            
            if (response != null) {
                result.put("success", true);
                result.put("data", response.getList());
                result.put("total", response.getTotal());
                result.put("page", page);
                result.put("message", String.format("找到 %d 条相关记录", response.getTotal()));
            } else {
                result.put("success", false);
                result.put("message", "搜索失败，请检查API状态");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("搜索注册证号失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "搜索异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 根据注册人名称搜索医疗器械
     */
    @GetMapping("/search/manufacturer")
    @Operation(summary = "根据注册人搜索", description = "根据注册人名称搜索中国上市医疗器械")
    public ResponseEntity<Map<String, Object>> searchByManufacturer(
            @Parameter(description = "注册人名称") @RequestParam String manufacturer,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page) {
        
        log.info("根据注册人搜索医疗器械: {}, 页码: {}", manufacturer, page);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            SearchParams params = new SearchParams();
            params.setManufacturerRe(manufacturer);
            
            ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, page);
            
            if (response != null) {
                result.put("success", true);
                result.put("data", response.getList());
                result.put("total", response.getTotal());
                result.put("page", page);
                result.put("message", String.format("找到 %d 条相关记录", response.getTotal()));
            } else {
                result.put("success", false);
                result.put("message", "搜索失败，请检查API状态");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("搜索注册人失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "搜索异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 根据管理类别获取医疗器械
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "根据管理类别查询", description = "根据管理类别（Ⅱ、Ⅲ）查询医疗器械")
    public ResponseEntity<Map<String, Object>> getDevicesByCategory(
            @Parameter(description = "管理类别（Ⅱ、Ⅲ）") @PathVariable String category,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page) {
        
        log.info("根据管理类别查询医疗器械: {}, 页码: {}", category, page);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            SearchParams params = new SearchParams();
            params.setCategory(category);
            
            ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, page);
            
            if (response != null) {
                result.put("success", true);
                result.put("data", response.getList());
                result.put("total", response.getTotal());
                result.put("page", page);
                result.put("category", category);
                result.put("message", String.format("找到 %d 条 %s 类医疗器械", response.getTotal(), category));
            } else {
                result.put("success", false);
                result.put("message", "查询失败，请检查API状态");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("查询管理类别失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "查询异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 根据国产/进口类型获取医疗器械
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "根据类型查询", description = "根据国产/进口类型查询医疗器械")
    public ResponseEntity<Map<String, Object>> getDevicesByType(
            @Parameter(description = "类型（国产、进口）") @PathVariable String type,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page) {
        
        log.info("根据类型查询医疗器械: {}, 页码: {}", type, page);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            SearchParams params = new SearchParams();
            params.setType(type);
            
            ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, page);
            
            if (response != null) {
                result.put("success", true);
                result.put("data", response.getList());
                result.put("total", response.getTotal());
                result.put("page", page);
                result.put("type", type);
                result.put("message", String.format("找到 %d 条 %s 医疗器械", response.getTotal(), type));
            } else {
                result.put("success", false);
                result.put("message", "查询失败，请检查API状态");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("查询类型失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "查询异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 高级搜索
     */
    @PostMapping("/search/advanced")
    @Operation(summary = "高级搜索", description = "使用多个条件进行高级搜索")
    public ResponseEntity<Map<String, Object>> advancedSearch(
            @RequestBody SearchParams searchParams,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page) {
        
        log.info("高级搜索医疗器械，页码: {}", page);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(searchParams, page);
            
            if (response != null) {
                result.put("success", true);
                result.put("data", response.getList());
                result.put("total", response.getTotal());
                result.put("page", page);
                result.put("searchParams", searchParams);
                result.put("message", String.format("找到 %d 条相关记录", response.getTotal()));
            } else {
                result.put("success", false);
                result.put("message", "搜索失败，请检查API状态");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("高级搜索失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "搜索异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 获取API统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "获取API统计", description = "获取API使用统计信息")
    public ResponseEntity<Map<String, Object>> getApiStats() {
        log.info("获取API统计信息");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> stats = chinaRegistrationApi.getApiStats();
            result.put("success", true);
            result.put("data", stats);
            result.put("message", "统计信息获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取API统计失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取统计信息异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 批量获取数据（用于数据收集）
     */
    @GetMapping("/batch")
    @Operation(summary = "批量获取数据", description = "批量获取医疗器械数据，用于数据收集")
    public ResponseEntity<Map<String, Object>> batchGetDevices(
            @Parameter(description = "最大页数") @RequestParam(defaultValue = "5") int maxPages,
            @Parameter(description = "产品名称关键词") @RequestParam(required = false) String productName,
            @Parameter(description = "管理类别") @RequestParam(required = false) String category,
            @Parameter(description = "国产/进口") @RequestParam(required = false) String type) {
        
        log.info("批量获取医疗器械数据，最大页数: {}", maxPages);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            SearchParams params = new SearchParams();
            if (productName != null) params.setProductName(productName);
            if (category != null) params.setCategory(category);
            if (type != null) params.setType(type);
            
            ChinaListedResponse response = chinaRegistrationApi.getAllChinaListedDevices(params, maxPages);
            
            if (response != null) {
                result.put("success", true);
                result.put("totalRecords", response.getList() != null ? response.getList().size() : 0);
                result.put("maxPages", maxPages);
                result.put("searchParams", params);
                result.put("message", String.format("批量获取完成，共获取 %d 条记录", 
                    response.getList() != null ? response.getList().size() : 0));
                
                // 不返回具体数据，只返回统计信息，避免响应过大
                result.put("sampleData", response.getList() != null && !response.getList().isEmpty() ? 
                    response.getList().get(0) : null);
            } else {
                result.put("success", false);
                result.put("message", "批量获取失败，请检查API状态");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("批量获取数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "批量获取异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}
