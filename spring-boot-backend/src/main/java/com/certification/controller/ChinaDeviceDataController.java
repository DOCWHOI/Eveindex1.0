package com.certification.controller;

import com.certification.service.ChinaDeviceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 中国医疗器械数据转换和保存控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/china-device-data")
@Tag(name = "中国医疗器械数据转换", description = "中国医疗器械数据转换为DeviceRegistrationRecord并保存")
public class ChinaDeviceDataController {

    @Autowired
    private ChinaDeviceDataService chinaDeviceDataService;

    /**
     * 转换并保存所有中国医疗器械数据
     */
    @PostMapping("/convert-and-save")
    @Operation(summary = "转换并保存数据", description = "将txt文件中的中国医疗器械数据转换为DeviceRegistrationRecord并保存到数据库")
    public ResponseEntity<Map<String, Object>> convertAndSaveData() {
        log.info("收到转换并保存中国医疗器械数据的请求");
        
        try {
            Map<String, Object> result = chinaDeviceDataService.convertAndSaveAllData();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("转换并保存数据失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "转换并保存数据失败: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * 重新导入中国数据（先清理再导入）
     */
    @PostMapping("/reimport")
    @Operation(summary = "重新导入数据", description = "清理现有中国数据并重新导入")
    public ResponseEntity<Map<String, Object>> reimportData() {
        log.info("收到重新导入中国医疗器械数据的请求");
        
        try {
            Map<String, Object> result = chinaDeviceDataService.reimportChinaData();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("重新导入数据失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "重新导入数据失败: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * 清理中国数据
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清理中国数据", description = "删除所有中国数据源的DeviceRegistrationRecord记录")
    public ResponseEntity<Map<String, Object>> clearChinaData() {
        log.info("收到清理中国医疗器械数据的请求");
        
        try {
            Map<String, Object> result = chinaDeviceDataService.clearChinaData();
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("清理中国数据失败: {}", e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "清理数据失败: " + e.getMessage());
            return ResponseEntity.ok(errorResult);
        }
    }

    /**
     * 获取转换统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取转换统计", description = "获取中国医疗器械数据转换和保存的统计信息")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("获取中国医疗器械数据转换统计");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> stats = chinaDeviceDataService.getConversionStatistics();
            result.put("success", true);
            result.put("data", stats);
            result.put("message", "统计信息获取成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("获取转换统计失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 获取字段映射说明
     */
    @GetMapping("/field-mapping")
    @Operation(summary = "获取字段映射", description = "获取中国医疗器械数据到DeviceRegistrationRecord的字段映射说明")
    public ResponseEntity<Map<String, Object>> getFieldMapping() {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, String> mapping = new HashMap<>();
        mapping.put("product_name", "deviceName (设备名称)");
        mapping.put("registration_number", "registrationNumber (注册证号)");
        mapping.put("manufacturer_re", "manufacturerName (制造商名称)");
        mapping.put("category", "deviceClass (设备类别) + riskClass (风险分类)");
        mapping.put("product_state", "statusCode (状态码)");
        mapping.put("approval_date", "createdDate (创建日期)");
        mapping.put("固定值CN_NMPA", "dataSource (数据源)");
        mapping.put("固定值CN", "jdCountry (数据源国家)");
        mapping.put("固定值MEDIUM", "riskLevel (风险等级评估)");
        mapping.put("固定值null", "keywords (关键词 - 不设置)");
        mapping.put("当前时间", "crawlTime (爬取时间)");
        
        result.put("success", true);
        result.put("fieldMapping", mapping);
        result.put("message", "字段映射说明");
        
        return ResponseEntity.ok(result);
    }
}
