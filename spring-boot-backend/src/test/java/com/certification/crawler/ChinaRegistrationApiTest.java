package com.certification.crawler;

import com.certification.crawler.countrydata.cn.cn_registration_api;
import com.certification.crawler.countrydata.cn.cn_registration_api.ChinaListedResponse;
import com.certification.crawler.countrydata.cn.cn_registration_api.MedicalDevice;
import com.certification.crawler.countrydata.cn.cn_registration_api.SearchParams;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 中国医疗器械注册API测试类
 * 
 * 使用说明：
 * 1. 确保API Key未过期
 * 2. 确保网络连接正常
 * 3. 运行前请检查API配额
 */
@Slf4j
@SpringBootTest
public class ChinaRegistrationApiTest {

    private final cn_registration_api chinaRegistrationApi = new cn_registration_api();

    /**
     * 测试API连接
     */
    @Test
    public void testApiConnection() {
        log.info("=== 测试API连接 ===");
        boolean connected = chinaRegistrationApi.testApiConnection();
        log.info("API连接测试结果: {}", connected ? "成功" : "失败");
        
        // 打印API统计信息
        log.info("API统计信息: {}", chinaRegistrationApi.getDataStatistics());
    }

    /**
     * 测试根据产品名称搜索
     */
    @Test
    public void testSearchByProductName() {
        log.info("=== 测试根据产品名称搜索 ===");
        
        String productName = "血糖仪";
        int page = 1;
        
        ChinaListedResponse response = chinaRegistrationApi.searchByProductName(productName, page);
        
        if (response != null && response.getList() != null) {
            log.info("搜索 '{}' 找到 {} 条记录", productName, response.getTotal());
            
            // 打印前3条记录
            for (int i = 0; i < Math.min(3, response.getList().size()); i++) {
                MedicalDevice device = response.getList().get(i);
                log.info("第 {} 条记录:", i + 1);
                chinaRegistrationApi.printDeviceInfo(device);
            }
        } else {
            log.error("搜索失败");
        }
    }

    /**
     * 测试根据注册证号搜索
     */
    @Test
    public void testSearchByRegistrationNumber() {
        log.info("=== 测试根据注册证号搜索 ===");
        
        // 使用一个示例注册证号（实际使用时需要真实的证号）
        String registrationNumber = "国械注准";
        int page = 1;
        
        SearchParams params = new SearchParams();
        params.setRegistrationNumberRemark(registrationNumber);
        
        ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, page);
        
        if (response != null && response.getList() != null) {
            log.info("搜索注册证号包含 '{}' 找到 {} 条记录", registrationNumber, response.getTotal());
            
            if (!response.getList().isEmpty()) {
                MedicalDevice device = response.getList().get(0);
                chinaRegistrationApi.printDeviceInfo(device);
            }
        } else {
            log.error("搜索失败");
        }
    }

    /**
     * 测试根据管理类别查询
     */
    @Test
    public void testGetDevicesByCategory() {
        log.info("=== 测试根据管理类别查询 ===");
        
        String category = "Ⅲ"; // 三类医疗器械
        int page = 1;
        
        SearchParams params = new SearchParams();
        params.setCategory(category);
        
        ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, page);
        
        if (response != null && response.getList() != null) {
            log.info("查询 {} 类医疗器械找到 {} 条记录", category, response.getTotal());
            
            if (!response.getList().isEmpty()) {
                MedicalDevice device = response.getList().get(0);
                chinaRegistrationApi.printDeviceInfo(device);
            }
        } else {
            log.error("查询失败");
        }
    }

    /**
     * 测试根据国产/进口类型查询
     */
    @Test
    public void testGetDevicesByType() {
        log.info("=== 测试根据国产/进口类型查询 ===");
        
        String type = "国产";
        int page = 1;
        
        SearchParams params = new SearchParams();
        params.setType(type);
        
        ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, page);
        
        if (response != null && response.getList() != null) {
            log.info("查询 {} 医疗器械找到 {} 条记录", type, response.getTotal());
            
            if (!response.getList().isEmpty()) {
                MedicalDevice device = response.getList().get(0);
                chinaRegistrationApi.printDeviceInfo(device);
            }
        } else {
            log.error("查询失败");
        }
    }

    /**
     * 测试批量获取数据
     */
    @Test
    public void testGetAllDevices() {
        log.info("=== 测试批量获取数据 ===");
        
        SearchParams params = new SearchParams();
        params.setProductName("血压"); // 搜索血压相关产品
        
        int maxPages = 3; // 限制为3页，避免请求过多
        
        ChinaListedResponse response = chinaRegistrationApi.getAllChinaListedDevices(params, maxPages);
        
        if (response != null && response.getList() != null) {
            log.info("批量获取完成，总计 {} 条记录", response.getList().size());
            
            // 统计不同类型的数量
            long domesticCount = response.getList().stream()
                .filter(device -> "国产".equals(device.getType()))
                .count();
            long importedCount = response.getList().stream()
                .filter(device -> "进口".equals(device.getType()))
                .count();
            
            log.info("统计结果 - 国产: {} 条, 进口: {} 条", domesticCount, importedCount);
            
            // 打印第一条记录作为示例
            if (!response.getList().isEmpty()) {
                log.info("示例记录:");
                chinaRegistrationApi.printDeviceInfo(response.getList().get(0));
            }
        } else {
            log.error("批量获取失败");
        }
    }

    /**
     * 测试多条件组合搜索
     */
    @Test
    public void testCombinedSearch() {
        log.info("=== 测试多条件组合搜索 ===");
        
        SearchParams params = new SearchParams();
        params.setCategory("Ⅲ");        // 三类医疗器械
        params.setType("进口");          // 进口产品
        params.setWhetherYibao("是");    // 纳入医保
        
        ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, 1);
        
        if (response != null && response.getList() != null) {
            log.info("组合搜索找到 {} 条记录", response.getTotal());
            log.info("搜索条件: 三类医疗器械 + 进口 + 纳入医保");
            
            if (!response.getList().isEmpty()) {
                MedicalDevice device = response.getList().get(0);
                chinaRegistrationApi.printDeviceInfo(device);
            }
        } else {
            log.error("组合搜索失败");
        }
    }
}
