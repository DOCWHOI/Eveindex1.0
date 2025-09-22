package com.certification.service;

import com.certification.crawler.countrydata.cn.cn_registration_api;
import com.certification.entity.common.DeviceRegistrationRecord;
import com.certification.entity.common.CrawlerData.RiskLevel;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * 中国医疗器械数据转换服务测试类
 */
@Slf4j
@SpringBootTest
public class ChinaDeviceDataServiceTest {

    @Autowired
    private ChinaDeviceDataService chinaDeviceDataService;
    
    @Autowired
    private cn_registration_api chinaRegistrationApi;

    /**
     * 测试数据转换功能
     */
    @Test
    public void testDataConversion() {
        log.info("=== 测试中国医疗器械数据转换 ===");
        
        try {
            // 1. 解析文件数据
            cn_registration_api.ChinaListedResponse response = chinaRegistrationApi.parseDataFromFile();
            
            if (response != null && response.getList() != null && !response.getList().isEmpty()) {
                log.info("成功解析 {} 条数据", response.getList().size());
                
                // 2. 转换第一条数据进行测试
                cn_registration_api.MedicalDevice chinaDevice = response.getList().get(0);
                DeviceRegistrationRecord record = chinaDeviceDataService.convertToDeviceRegistrationRecord(chinaDevice);
                
                if (record != null) {
                    log.info("=== 转换结果验证 ===");
                    log.info("设备名称: {}", record.getDeviceName());
                    log.info("注册证号: {}", record.getRegistrationNumber());
                    log.info("制造商: {}", record.getManufacturerName());
                    log.info("设备类别: {}", record.getDeviceClass());
                    log.info("风险分类: {}", record.getRiskClass());
                    log.info("状态码: {}", record.getStatusCode());
                    log.info("创建日期: {}", record.getCreatedDate());
                    log.info("数据源: {}", record.getDataSource());
                    log.info("国家: {}", record.getJdCountry());
                    log.info("风险等级: {}", record.getRiskLevel());
                    log.info("关键词: {}", record.getKeywords());
                    log.info("爬取时间: {}", record.getCrawlTime());
                    
                    // 打印转换对比
                    chinaDeviceDataService.printConversionMapping(chinaDevice, record);
                    
                    // 验证必填字段
                    assert record.getDataSource().equals("CN_NMPA");
                    assert record.getJdCountry().equals("CN");
                    assert record.getRiskLevel() == RiskLevel.MEDIUM;
                    assert record.getKeywords() == null;
                    
                    log.info("✅ 数据转换验证通过");
                } else {
                    log.error("❌ 数据转换失败");
                }
            } else {
                log.error("❌ 无法解析数据文件");
            }
            
        } catch (Exception e) {
            log.error("测试数据转换失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 测试批量转换和保存
     */
    @Test
    public void testBatchConvertAndSave() {
        log.info("=== 测试批量转换和保存 ===");
        
        try {
            Map<String, Object> result = chinaDeviceDataService.convertAndSaveAllData();
            
            log.info("批量转换和保存结果: {}", result);
            
            if ((Boolean) result.getOrDefault("success", false)) {
                log.info("✅ 批量转换和保存成功");
                log.info("总解析: {} 条", result.get("totalParsed"));
                log.info("转换: {} 条", result.get("converted"));
                log.info("跳过: {} 条", result.get("skipped"));
                log.info("保存: {} 条", result.get("saved"));
                
                // 显示示例记录
                @SuppressWarnings("unchecked")
                Map<String, Object> sampleRecord = (Map<String, Object>) result.get("sampleRecord");
                if (sampleRecord != null) {
                    log.info("示例记录: {}", sampleRecord);
                }
            } else {
                log.error("❌ 批量转换和保存失败: {}", result.get("message"));
            }
            
        } catch (Exception e) {
            log.error("测试批量转换和保存失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 测试获取统计信息
     */
    @Test
    public void testGetStatistics() {
        log.info("=== 测试获取统计信息 ===");
        
        try {
            Map<String, Object> stats = chinaDeviceDataService.getConversionStatistics();
            log.info("统计信息: {}", stats);
            
        } catch (Exception e) {
            log.error("测试获取统计信息失败: {}", e.getMessage(), e);
        }
    }
}
