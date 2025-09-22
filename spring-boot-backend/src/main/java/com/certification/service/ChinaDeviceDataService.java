package com.certification.service;

import com.certification.crawler.countrydata.cn.cn_registration_api;
import com.certification.entity.common.DeviceRegistrationRecord;
import com.certification.entity.common.CrawlerData.RiskLevel;
import com.certification.repository.common.DeviceRegistrationRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中国医疗器械数据转换和保存服务
 */
@Slf4j
@Service
@Transactional
public class ChinaDeviceDataService {

    @Autowired
    private cn_registration_api chinaRegistrationApi;
    
    @Autowired
    private DeviceRegistrationRecordRepository deviceRegistrationRecordRepository;

    /**
     * 将中国医疗器械数据转换为DeviceRegistrationRecord实体
     */
    public DeviceRegistrationRecord convertToDeviceRegistrationRecord(cn_registration_api.MedicalDevice chinaDevice) {
        if (chinaDevice == null) {
            return null;
        }
        
        log.debug("转换中国医疗器械数据: {}", chinaDevice.getProductName());
        
        DeviceRegistrationRecord record = new DeviceRegistrationRecord();
        
        // ========== 数据源标识 ==========
        record.setDataSource("CN_NMPA");  // 中国国家药品监督管理局
        record.setJdCountry("CN");         // 中国
        
        // ========== 核心标识字段 ==========
        record.setRegistrationNumber(chinaDevice.getRegistrationNumber());
        record.setFeiNumber(null); // 中国数据没有对应的FEI号
        
        // ========== 制造商信息 ==========
        record.setManufacturerName(chinaDevice.getManufacturerRe());
        
        // ========== 设备信息 ==========
        record.setDeviceName(chinaDevice.getProductName());
        record.setProprietaryName(chinaDevice.getProductName()); // 使用产品名称作为专有名称
        record.setDeviceClass(chinaDevice.getCategory()); // 管理类别（Ⅱ、Ⅲ）
        record.setRiskClass(mapCategoryToRiskClass(chinaDevice.getCategory()));
        
        // ========== 状态信息 ==========
        record.setStatusCode(chinaDevice.getProductState()); // 器械状态
        record.setCreatedDate(chinaDevice.getApprovalDate()); // 批准日期
        
        // ========== 分析字段（按要求设置） ==========
        record.setRiskLevel(RiskLevel.MEDIUM);  // 全部设置为中等风险
        record.setKeywords(null);               // 不设置关键词
        
        // ========== 元数据 ==========
        record.setCrawlTime(LocalDateTime.now());
        
        return record;
    }

    /**
     * 将管理类别映射为风险分类描述
     */
    private String mapCategoryToRiskClass(String category) {
        if (category == null) {
            return "未知";
        }
        
        switch (category) {
            case "Ⅰ":
                return "一类医疗器械";
            case "Ⅱ":
                return "二类医疗器械";
            case "Ⅲ":
                return "三类医疗器械";
            default:
                return "未分类";
        }
    }

    /**
     * 批量转换并保存中国医疗器械数据
     */
    @Transactional
    public Map<String, Object> convertAndSaveAllData() {
        log.info("开始批量转换并保存中国医疗器械数据");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 解析文件数据
            cn_registration_api.ChinaListedResponse response = chinaRegistrationApi.parseDataFromFile();
            
            if (response == null || response.getList() == null || response.getList().isEmpty()) {
                result.put("success", false);
                result.put("message", "没有数据可转换，请检查数据文件");
                return result;
            }
            
            List<cn_registration_api.MedicalDevice> chinaDevices = response.getList();
            log.info("从文件解析到 {} 条中国医疗器械数据", chinaDevices.size());
            
            // 2. 转换数据
            List<DeviceRegistrationRecord> records = new ArrayList<>();
            int convertedCount = 0;
            int skippedCount = 0;
            List<String> skippedReasons = new ArrayList<>();
            
            for (cn_registration_api.MedicalDevice chinaDevice : chinaDevices) {
                try {
                    // 检查必要字段
                    if (chinaDevice.getRegistrationNumber() == null || 
                        chinaDevice.getRegistrationNumber().trim().isEmpty()) {
                        skippedReasons.add("注册证号为空: " + chinaDevice.getProductName());
                        skippedCount++;
                        continue;
                    }
                    
                    // 检查是否已存在相同注册证号的记录
                    List<DeviceRegistrationRecord> existingRecords = 
                        deviceRegistrationRecordRepository.findByRegistrationNumber(chinaDevice.getRegistrationNumber());
                    
                    if (!existingRecords.isEmpty()) {
                        log.debug("跳过已存在的注册证号: {}", chinaDevice.getRegistrationNumber());
                        skippedReasons.add("已存在: " + chinaDevice.getRegistrationNumber());
                        skippedCount++;
                        continue;
                    }
                    
                    DeviceRegistrationRecord record = convertToDeviceRegistrationRecord(chinaDevice);
                    if (record != null) {
                        records.add(record);
                        convertedCount++;
                        log.debug("成功转换: {}", chinaDevice.getProductName());
                    }
                } catch (Exception e) {
                    log.error("转换设备数据失败: {}, 错误: {}", chinaDevice.getProductName(), e.getMessage());
                    skippedReasons.add("转换失败: " + chinaDevice.getProductName() + " - " + e.getMessage());
                    skippedCount++;
                }
            }
            
            log.info("数据转换完成: 转换 {} 条, 跳过 {} 条", convertedCount, skippedCount);
            
            // 3. 批量保存到数据库
            if (!records.isEmpty()) {
                List<DeviceRegistrationRecord> savedRecords = deviceRegistrationRecordRepository.saveAll(records);
                log.info("成功保存 {} 条记录到数据库", savedRecords.size());
                
                result.put("success", true);
                result.put("totalParsed", chinaDevices.size());
                result.put("converted", convertedCount);
                result.put("skipped", skippedCount);
                result.put("saved", savedRecords.size());
                result.put("skippedReasons", skippedReasons);
                result.put("message", String.format("成功转换并保存 %d 条中国医疗器械数据", savedRecords.size()));
                
                // 返回保存的记录ID列表
                List<Long> savedIds = savedRecords.stream()
                    .map(DeviceRegistrationRecord::getId)
                    .toList();
                result.put("savedIds", savedIds);
                
                // 返回第一条保存的记录作为示例
                if (!savedRecords.isEmpty()) {
                    result.put("sampleRecord", createSampleRecordInfo(savedRecords.get(0)));
                }
            } else {
                result.put("success", false);
                result.put("message", "没有有效数据可保存");
                result.put("skippedReasons", skippedReasons);
            }
            
        } catch (Exception e) {
            log.error("批量转换并保存数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "转换保存失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 创建示例记录信息（避免返回完整实体）
     */
    private Map<String, Object> createSampleRecordInfo(DeviceRegistrationRecord record) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", record.getId());
        info.put("deviceName", record.getDeviceName());
        info.put("registrationNumber", record.getRegistrationNumber());
        info.put("manufacturerName", record.getManufacturerName());
        info.put("deviceClass", record.getDeviceClass());
        info.put("riskLevel", record.getRiskLevel());
        info.put("dataSource", record.getDataSource());
        info.put("jdCountry", record.getJdCountry());
        info.put("crawlTime", record.getCrawlTime());
        return info;
    }

    /**
     * 获取转换统计信息
     */
    public Map<String, Object> getConversionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 统计中国数据源的记录数
            List<DeviceRegistrationRecord> cnRecords = deviceRegistrationRecordRepository.findByDataSource("CN_NMPA");
            stats.put("cnNmpaRecords", cnRecords.size());
            
            // 统计所有数据源
            long totalRecords = deviceRegistrationRecordRepository.count();
            stats.put("totalRecords", totalRecords);
            
            // 统计按数据源分布
            long cnCount = cnRecords.size();
            List<DeviceRegistrationRecord> usFdaRecords = deviceRegistrationRecordRepository.findByDataSource("US_FDA");
            List<DeviceRegistrationRecord> euRecords = deviceRegistrationRecordRepository.findByDataSource("EU_EUDAMED");
            
            Map<String, Long> sourceDistribution = new HashMap<>();
            sourceDistribution.put("CN_NMPA", (long) cnCount);
            sourceDistribution.put("US_FDA", (long) usFdaRecords.size());
            sourceDistribution.put("EU_EUDAMED", (long) euRecords.size());
            sourceDistribution.put("其他", totalRecords - cnCount - usFdaRecords.size() - euRecords.size());
            stats.put("sourceDistribution", sourceDistribution);
            
            // 计算中国数据占比
            double cnPercentage = totalRecords > 0 ? (double) cnCount / totalRecords * 100 : 0;
            stats.put("cnDataPercentage", String.format("%.2f%%", cnPercentage));
            
            // 统计中国数据的风险等级分布
            Map<String, Long> cnRiskDistribution = new HashMap<>();
            cnRiskDistribution.put("MEDIUM", (long) cnCount); // 按要求全部设置为MEDIUM
            stats.put("cnRiskDistribution", cnRiskDistribution);
            
        } catch (Exception e) {
            log.error("获取转换统计信息失败: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }

    /**
     * 清理所有中国数据源的记录（用于重新导入）
     */
    @Transactional
    public Map<String, Object> clearChinaData() {
        log.info("清理所有中国数据源的记录");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<DeviceRegistrationRecord> cnRecords = deviceRegistrationRecordRepository.findByDataSource("CN_NMPA");
            int deleteCount = cnRecords.size();
            
            if (deleteCount > 0) {
                deviceRegistrationRecordRepository.deleteAll(cnRecords);
                log.info("成功删除 {} 条中国数据源记录", deleteCount);
                
                result.put("success", true);
                result.put("deletedCount", deleteCount);
                result.put("message", String.format("成功清理 %d 条中国数据", deleteCount));
            } else {
                result.put("success", true);
                result.put("deletedCount", 0);
                result.put("message", "没有中国数据需要清理");
            }
            
        } catch (Exception e) {
            log.error("清理中国数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "清理数据失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 重新导入中国数据（先清理再导入）
     */
    @Transactional
    public Map<String, Object> reimportChinaData() {
        log.info("重新导入中国医疗器械数据");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 清理现有数据
            Map<String, Object> clearResult = clearChinaData();
            int deletedCount = (Integer) clearResult.getOrDefault("deletedCount", 0);
            
            // 2. 导入新数据
            Map<String, Object> importResult = convertAndSaveAllData();
            
            if ((Boolean) importResult.getOrDefault("success", false)) {
                result.put("success", true);
                result.put("deletedCount", deletedCount);
                result.put("importedCount", importResult.get("saved"));
                result.put("message", String.format("重新导入完成: 删除 %d 条, 导入 %d 条", 
                    deletedCount, importResult.get("saved")));
                result.putAll(importResult);
            } else {
                result.put("success", false);
                result.put("message", "导入新数据失败: " + importResult.get("message"));
            }
            
        } catch (Exception e) {
            log.error("重新导入中国数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "重新导入失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 打印转换对比信息
     */
    public void printConversionMapping(cn_registration_api.MedicalDevice chinaDevice, DeviceRegistrationRecord record) {
        System.out.println("=== 数据转换映射 ===");
        System.out.println("中国数据 -> DeviceRegistrationRecord");
        System.out.println("product_name: " + chinaDevice.getProductName() + " -> deviceName: " + record.getDeviceName());
        System.out.println("registration_number: " + chinaDevice.getRegistrationNumber() + " -> registrationNumber: " + record.getRegistrationNumber());
        System.out.println("manufacturer_re: " + chinaDevice.getManufacturerRe() + " -> manufacturerName: " + record.getManufacturerName());
        System.out.println("category: " + chinaDevice.getCategory() + " -> deviceClass: " + record.getDeviceClass());
        System.out.println("category: " + chinaDevice.getCategory() + " -> riskClass: " + record.getRiskClass());
        System.out.println("product_state: " + chinaDevice.getProductState() + " -> statusCode: " + record.getStatusCode());
        System.out.println("approval_date: " + chinaDevice.getApprovalDate() + " -> createdDate: " + record.getCreatedDate());
        System.out.println("固定值 CN_NMPA -> dataSource: " + record.getDataSource());
        System.out.println("固定值 CN -> jdCountry: " + record.getJdCountry());
        System.out.println("固定值 MEDIUM -> riskLevel: " + record.getRiskLevel());
        System.out.println("固定值 null -> keywords: " + record.getKeywords());
        System.out.println("当前时间 -> crawlTime: " + record.getCrawlTime());
        System.out.println("========================");
    }
}
