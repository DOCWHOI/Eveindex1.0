package com.certification.controller;

import com.certification.crawler.countrydata.cn.cn_registration_api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中国医疗器械注册数据控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/china-registration")
@Tag(name = "中国医疗器械注册数据", description = "中国上市医疗器械数据查询接口")
public class ChinaRegistrationController {

    @Autowired
    private cn_registration_api chinaRegistrationApi;

    /**
     * 测试数据文件解析
     */
    @GetMapping("/test")
    @Operation(summary = "测试数据文件解析", description = "测试从txt文件解析中国医疗器械数据")
    public ResponseEntity<Map<String, Object>> testDataParsing() {
        log.info("测试中国医疗器械数据文件解析");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            cn_registration_api.ChinaListedResponse response = chinaRegistrationApi.parseDataFromFile();
            
            if (response != null && response.getList() != null && !response.getList().isEmpty()) {
                result.put("success", true);
                result.put("message", "数据解析成功");
                result.put("totalRecords", response.getList().size());
                result.put("sampleData", response.getList().get(0));
                result.put("statistics", chinaRegistrationApi.getDataStatistics());
            } else {
                result.put("success", false);
                result.put("message", response != null ? response.getErrorMessage() : "数据解析失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("数据解析测试失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "数据解析测试异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 根据产品名称搜索医疗器械
     */
    @GetMapping("/search/product")
    @Operation(summary = "根据产品名称搜索", description = "根据产品名称搜索中国上市医疗器械")
    public ResponseEntity<Map<String, Object>> searchByProductName(
            @Parameter(description = "产品名称") @RequestParam String productName) {
        
        log.info("根据产品名称搜索医疗器械: {}", productName);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<cn_registration_api.MedicalDevice> devices = chinaRegistrationApi.filterByProductName(productName);
            
            if (devices != null && !devices.isEmpty()) {
                result.put("success", true);
                result.put("data", devices);
                result.put("total", devices.size());
                result.put("message", String.format("找到 %d 条相关记录", devices.size()));
                
                // 打印第一条记录的详细信息
                chinaRegistrationApi.printDeviceInfo(devices.get(0));
            } else {
                result.put("success", false);
                result.put("message", "未找到匹配的记录");
                result.put("data", new ArrayList<>());
                result.put("total", 0);
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
     * 获取所有数据
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有数据", description = "获取文件中的所有医疗器械数据")
    public ResponseEntity<Map<String, Object>> getAllDevices() {
        log.info("获取所有医疗器械数据");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            cn_registration_api.ChinaListedResponse response = chinaRegistrationApi.parseDataFromFile();
            
            if (response != null && response.getList() != null) {
                result.put("success", true);
                result.put("data", response.getList());
                result.put("total", response.getList().size());
                result.put("message", String.format("共获取 %d 条记录", response.getList().size()));
                result.put("statistics", chinaRegistrationApi.getDataStatistics());
            } else {
                result.put("success", false);
                result.put("message", response != null ? response.getErrorMessage() : "获取数据失败");
                result.put("data", new ArrayList<>());
                result.put("total", 0);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取所有数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取数据异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 获取数据统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取数据统计", description = "获取医疗器械数据的统计信息")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("获取医疗器械数据统计");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> stats = chinaRegistrationApi.getDataStatistics();
            result.put("success", true);
            result.put("data", stats);
            result.put("message", "统计信息获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取统计信息失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取统计信息异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 导出数据为CSV文件
     */
    @GetMapping("/export/csv")
    @Operation(summary = "导出CSV文件", description = "将医疗器械数据导出为CSV文件")
    public ResponseEntity<Map<String, Object>> exportToCsv() {
        log.info("导出医疗器械数据为CSV文件");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            String csvFilePath = chinaRegistrationApi.exportToCsv();
            
            if (csvFilePath != null) {
                result.put("success", true);
                result.put("filePath", csvFilePath);
                result.put("absolutePath", new java.io.File(csvFilePath).getAbsolutePath());
                result.put("message", "CSV文件导出成功");
                
                log.info("CSV文件导出成功: {}", csvFilePath);
            } else {
                result.put("success", false);
                result.put("message", "CSV文件导出失败");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("CSV文件导出失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "CSV文件导出异常: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
}