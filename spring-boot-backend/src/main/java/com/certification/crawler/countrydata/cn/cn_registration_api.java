package com.certification.crawler.countrydata.cn;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中国上市医疗器械数据解析器
 * 从txt文件中读取JSON格式的中国医疗器械注册数据并解析
 */
@Slf4j
@Component
public class cn_registration_api {

    private static final String DATA_FILE_PATH = "src/main/java/com/certification/crawler/countrydata/cn/cn_registration_api.txt";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从文件中解析中国医疗器械注册数据
     */
    public ChinaListedResponse parseDataFromFile() {
        log.info("开始从文件解析中国医疗器械注册数据: {}", DATA_FILE_PATH);
        
        try {
            File dataFile = new File(DATA_FILE_PATH);
            if (!dataFile.exists()) {
                log.error("数据文件不存在: {}", DATA_FILE_PATH);
                return createEmptyResponse("数据文件不存在");
            }
            
            String fileContent = Files.readString(Paths.get(DATA_FILE_PATH));
            if (fileContent == null || fileContent.trim().isEmpty()) {
                log.error("数据文件为空");
                return createEmptyResponse("数据文件为空");
            }
            
            log.info("成功读取文件，内容长度: {} 字符", fileContent.length());
            
            // 清理内容
            String cleanedContent = cleanJsonContent(fileContent);
            
            // 解析JSON数据
            ChinaListedResponse response = objectMapper.readValue(cleanedContent, ChinaListedResponse.class);
            
            if (response != null && response.getList() != null) {
                log.info("成功解析数据，共 {} 条记录", response.getList().size());
                return response;
            } else {
                log.error("解析结果为空");
                return createEmptyResponse("解析结果为空");
            }
            
        } catch (Exception e) {
            log.error("解析数据失败: {}", e.getMessage(), e);
            return createEmptyResponse("解析数据失败: " + e.getMessage());
        }
    }

    /**
     * 清理JSON内容
     */
    private String cleanJsonContent(String content) {
        if (content == null) return "";
        
        // 移除HTML代码块标签
        content = content.replaceAll("<code[^>]*>", "");
        content = content.replaceAll("</code>", "");
        content = content.replaceAll("```json", "");
        content = content.replaceAll("```", "");
        
        // 查找JSON开始和结束位置
        int jsonStart = content.indexOf("{");
        if (jsonStart > 0) {
            content = content.substring(jsonStart);
        }
        
        int jsonEnd = content.lastIndexOf("}");
        if (jsonEnd > 0 && jsonEnd < content.length() - 1) {
            content = content.substring(0, jsonEnd + 1);
        }
        
        return content.trim();
    }

    /**
     * 创建空响应对象
     */
    private ChinaListedResponse createEmptyResponse(String message) {
        ChinaListedResponse response = new ChinaListedResponse();
        response.setList(new ArrayList<>());
        response.setTotal(0);
        response.setErrorMessage(message);
        return response;
    }

    /**
     * 根据产品名称过滤数据
     */
    public List<MedicalDevice> filterByProductName(String productName) {
        ChinaListedResponse allData = parseDataFromFile();
        if (allData == null || allData.getList() == null) {
            return new ArrayList<>();
        }
        
        return allData.getList().stream()
            .filter(device -> device.getProductName() != null && 
                            device.getProductName().toLowerCase().contains(productName.toLowerCase()))
            .toList();
    }

    /**
     * 获取数据统计信息
     */
    public Map<String, Object> getDataStatistics() {
        ChinaListedResponse allData = parseDataFromFile();
        Map<String, Object> stats = new HashMap<>();
        
        if (allData == null || allData.getList() == null) {
            stats.put("totalCount", 0);
            stats.put("error", allData != null ? allData.getErrorMessage() : "解析失败");
            return stats;
        }
        
        List<MedicalDevice> devices = allData.getList();
        stats.put("totalCount", devices.size());
        
        // 按管理类别统计
        Map<String, Long> categoryStats = devices.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                device -> device.getCategory() != null ? device.getCategory() : "未知",
                java.util.stream.Collectors.counting()
            ));
        stats.put("categoryStats", categoryStats);
        
        // 按国产/进口统计
        Map<String, Long> typeStats = devices.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                device -> device.getType() != null ? device.getType() : "未知",
                java.util.stream.Collectors.counting()
            ));
        stats.put("typeStats", typeStats);
        
        // 按省份统计
        Map<String, Long> provinceStats = devices.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                device -> device.getProvince() != null ? device.getProvince() : "未知",
                java.util.stream.Collectors.counting()
            ));
        stats.put("provinceStats", provinceStats);
        
        // 按产品状态统计
        Map<String, Long> stateStats = devices.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                device -> device.getProductState() != null ? device.getProductState() : "未知",
                java.util.stream.Collectors.counting()
            ));
        stats.put("stateStats", stateStats);
        
        return stats;
    }

    /**
     * 打印设备信息
     */
    public void printDeviceInfo(MedicalDevice device) {
        if (device == null) return;
        
        log.info("=== 医疗器械信息 ===");
        log.info("产品名称: {}", device.getProductName());
        log.info("注册证号: {}", device.getRegistrationNumber());
        log.info("注册人: {}", device.getManufacturerRe());
        log.info("管理类别: {}", device.getCategory());
        log.info("分类: {}", device.getClassification());
        log.info("国产/进口: {}", device.getType());
        log.info("器械状态: {}", device.getProductState());
        log.info("省份: {}", device.getProvince());
        log.info("城市: {}", device.getCity());
        log.info("适用范围: {}", cleanHtmlTags(device.getScopeAndUse()));
        log.info("==================");
    }

    /**
     * 将解析结果导出为CSV文件
     */
    public String exportToCsv() {
        log.info("开始导出数据到CSV文件");
        
        try {
            ChinaListedResponse response = parseDataFromFile();
            if (response == null || response.getList() == null || response.getList().isEmpty()) {
                log.error("没有数据可导出");
                return null;
            }
            
            // 生成CSV文件名
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String csvFileName = "china_medical_devices_" + timestamp + ".csv";
            String csvFilePath = "output/" + csvFileName;
            
            // 创建输出目录
            File outputDir = new File("output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // 写入CSV文件
            try (FileWriter writer = new FileWriter(csvFilePath, java.nio.charset.StandardCharsets.UTF_8)) {
                // 写入BOM以支持Excel正确显示中文
                writer.write('\ufeff');
                
                // 写入CSV标题行
                writer.append("序号,产品名称,注册证号,注册人,管理类别,分类,国产进口,适用范围,批准日期,有效期至,器械状态,是否纳入医保,省份,城市,区县\n");
                
                // 写入数据行
                List<MedicalDevice> devices = response.getList();
                for (int i = 0; i < devices.size(); i++) {
                    MedicalDevice device = devices.get(i);
                    
                    writer.append(String.valueOf(i + 1)).append(",");
                    writer.append(escapeCsvField(device.getProductName())).append(",");
                    writer.append(escapeCsvField(device.getRegistrationNumber())).append(",");
                    writer.append(escapeCsvField(device.getManufacturerRe())).append(",");
                    writer.append(escapeCsvField(device.getCategory())).append(",");
                    writer.append(escapeCsvField(device.getClassification())).append(",");
                    writer.append(escapeCsvField(device.getType())).append(",");
                    writer.append(escapeCsvField(cleanHtmlTags(device.getScopeAndUse()))).append(",");
                    writer.append(escapeCsvField(device.getApprovalDate())).append(",");
                    writer.append(escapeCsvField(device.getValidUntil())).append(",");
                    writer.append(escapeCsvField(device.getProductState())).append(",");
                    writer.append(escapeCsvField(device.getWhetherYibao())).append(",");
                    writer.append(escapeCsvField(device.getProvince())).append(",");
                    writer.append(escapeCsvField(device.getCity())).append(",");
                    writer.append(escapeCsvField(device.getRegion())).append("\n");
                }
            }
            
            log.info("CSV文件导出成功: {}", csvFilePath);
            log.info("导出记录数: {}", response.getList().size());
            
            return csvFilePath;
            
        } catch (IOException e) {
            log.error("CSV文件导出失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 转义CSV字段
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        
        // 移除换行符
        field = field.replaceAll("[\r\n]+", " ");
        
        // 如果包含逗号、引号或换行，需要用引号包围并转义内部引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            field = "\"" + field.replace("\"", "\"\"") + "\"";
        }
        
        return field;
    }

    /**
     * 清理HTML标签
     */
    private String cleanHtmlTags(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "").trim();
    }

    // ==================== 数据模型类 ====================

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChinaListedResponse {
        private List<MedicalDevice> list;
        private Integer total;
        private String errorMessage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MedicalDevice {
        private String keyid;
        
        @JsonProperty("product_name")
        private String productName;
        
        @JsonProperty("registration_number")
        private String registrationNumber;
        
        @JsonProperty("registration_number_remark")
        private String registrationNumberRemark;
        
        @JsonProperty("category_year")
        private String categoryYear;
        
        private String classification;
        
        @JsonProperty("first_category")
        private String firstCategory;
        
        @JsonProperty("secondary_category")
        private String secondaryCategory;
        
        private String category;
        
        @JsonProperty("manufacturer_re")
        private String manufacturerRe;
        
        @JsonProperty("registrant_domicile")
        private String registrantDomicile;
        
        @JsonProperty("production_address")
        private String productionAddress;
        
        @JsonProperty("product_storage_conditions_and_expiry_date")
        private String productStorageConditionsAndExpiryDate;
        
        @JsonProperty("scope_and_use")
        private String scopeAndUse;
        
        @JsonProperty("agent_name")
        private String agentName;
        
        private String change;
        
        @JsonProperty("model_specification")
        private String modelSpecification;
        
        @JsonProperty("structure_and_components")
        private String structureAndComponents;
        
        @JsonProperty("approval_department")
        private String approvalDepartment;
        
        @JsonProperty("approval_date")
        private String approvalDate;
        
        @JsonProperty("effective_date")
        private String effectiveDate;
        
        @JsonProperty("valid_until")
        private String validUntil;
        
        private String type;
        
        @JsonProperty("whether_yibao")
        private String whetherYibao;
        
        private String remark;
        
        @JsonProperty("registration_category")
        private String registrationCategory;
        
        private String province;
        private String city;
        private String region;
        private String classify;
        
        @JsonProperty("product_state")
        private String productState;
        
        @JsonProperty("certificate_state")
        private String certificateState;
        
        @JsonProperty("prioritize_innovation")
        private String prioritizeInnovation;
    }

    /**
     * 主函数 - 用于测试和导出数据
     */
    public static void main(String[] args) {
        System.out.println("=== 中国医疗器械注册数据解析器测试 ===");
        
        cn_registration_api parser = new cn_registration_api();
        
        try {
            // 1. 测试数据解析
            System.out.println("\n1. 测试数据文件解析...");
            ChinaListedResponse response = parser.parseDataFromFile();
            
            if (response != null && response.getList() != null) {
                System.out.println("✅ 数据解析成功！");
                System.out.println("📊 总记录数: " + response.getList().size());
                
                // 2. 显示统计信息
                System.out.println("\n2. 数据统计信息:");
                Map<String, Object> stats = parser.getDataStatistics();
                System.out.println("📈 统计结果: " + stats);
                
                // 3. 显示第一条记录
                if (!response.getList().isEmpty()) {
                    System.out.println("\n3. 第一条记录示例:");
                    parser.printDeviceInfo(response.getList().get(0));
                }
                
                // 4. 测试搜索功能
                System.out.println("\n4. 测试搜索功能:");
                List<MedicalDevice> skinDevices = parser.filterByProductName("皮肤");
                System.out.println("🔍 搜索'皮肤'相关产品: " + skinDevices.size() + " 条");
                
                if (!skinDevices.isEmpty()) {
                    System.out.println("搜索结果示例:");
                    for (int i = 0; i < Math.min(3, skinDevices.size()); i++) {
                        MedicalDevice device = skinDevices.get(i);
                        System.out.println("  " + (i+1) + ". " + device.getProductName() + 
                                         " - " + device.getRegistrationNumber() + 
                                         " - " + device.getManufacturerRe());
                    }
                }
                
                // 5. 导出CSV文件
                System.out.println("\n5. 导出CSV文件...");
                String csvPath = parser.exportToCsv();
                if (csvPath != null) {
                    System.out.println("✅ CSV文件导出成功: " + csvPath);
                    System.out.println("📁 文件绝对路径: " + new File(csvPath).getAbsolutePath());
                } else {
                    System.out.println("❌ CSV文件导出失败");
                }
                
            } else {
                System.out.println("❌ 数据解析失败");
                if (response != null) {
                    System.out.println("错误信息: " + response.getErrorMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ 测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== 测试完成 ===");
    }
}