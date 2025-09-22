# 中国医疗器械数据转换说明

## 📋 **功能概述**

将从txt文件解析的中国医疗器械数据转换为`DeviceRegistrationRecord`实体类并保存到数据库。

## 🔄 **字段映射规则**

### 直接映射
- `product_name` → `deviceName` (设备名称)
- `registration_number` → `registrationNumber` (注册证号)
- `manufacturer_re` → `manufacturerName` (制造商名称)
- `category` → `deviceClass` (设备类别)
- `product_state` → `statusCode` (状态码)
- `approval_date` → `createdDate` (创建日期)

### 固定值映射
- `dataSource` = "CN_NMPA" (中国国家药品监督管理局)
- `jdCountry` = "CN" (中国)
- `riskLevel` = `RiskLevel.MEDIUM` (按要求全部设置为中等风险)
- `keywords` = `null` (按要求不设置关键词)
- `feiNumber` = `null` (中国数据无此字段)

### 计算映射
- `riskClass` = 根据category计算 (一类/二类/三类医疗器械)
- `proprietaryName` = 使用`product_name`
- `crawlTime` = 当前时间

## 🚀 **使用方法**

### 1. **准备数据文件**
将中国医疗器械JSON数据保存到：
```
spring-boot-backend/src/main/java/com/certification/crawler/countrydata/cn/cn_registration_api.txt
```

### 2. **API接口调用**

#### 转换并保存所有数据
```http
POST /api/china-device-data/convert-and-save
```

#### 重新导入数据（先清理再导入）
```http
POST /api/china-device-data/reimport
```

#### 清理中国数据
```http
DELETE /api/china-device-data/clear
```

#### 获取转换统计
```http
GET /api/china-device-data/statistics
```

#### 获取字段映射说明
```http
GET /api/china-device-data/field-mapping
```

### 3. **编程方式调用**

```java
@Autowired
private ChinaDeviceDataService chinaDeviceDataService;

// 转换并保存所有数据
Map<String, Object> result = chinaDeviceDataService.convertAndSaveAllData();

// 检查结果
if ((Boolean) result.get("success")) {
    System.out.println("成功保存: " + result.get("saved") + " 条记录");
} else {
    System.out.println("保存失败: " + result.get("message"));
}
```

### 4. **单元测试**

```bash
cd spring-boot-backend
mvn test -Dtest=ChinaDeviceDataServiceTest
```

## 📊 **数据转换示例**

### 输入数据（中国医疗器械）：
```json
{
  "product_name": "皮肤图像处理工作站",
  "registration_number": "湘械注准20252060324",
  "manufacturer_re": "长沙康妃尔得医疗科技有限公司",
  "category": "Ⅱ",
  "product_state": "有效",
  "approval_date": "2025-04-02",
  "type": "国产",
  "province": "湖南省"
}
```

### 输出数据（DeviceRegistrationRecord）：
```java
DeviceRegistrationRecord {
  deviceName: "皮肤图像处理工作站",
  registrationNumber: "湘械注准20252060324",
  manufacturerName: "长沙康妃尔得医疗科技有限公司",
  deviceClass: "Ⅱ",
  riskClass: "二类医疗器械",
  statusCode: "有效",
  createdDate: "2025-04-02",
  dataSource: "CN_NMPA",
  jdCountry: "CN",
  riskLevel: RiskLevel.MEDIUM,
  keywords: null,
  crawlTime: "2025-09-22T10:30:00"
}
```

## ⚠️ **注意事项**

1. **数据去重**: 系统会检查`registrationNumber`是否已存在，避免重复保存
2. **错误处理**: 转换失败的记录会被跳过并记录原因
3. **事务管理**: 使用`@Transactional`确保数据一致性
4. **日志记录**: 详细的转换和保存日志
5. **统计信息**: 提供转换成功/失败的详细统计

## 🔧 **故障排除**

### 常见问题

**1. 数据文件不存在**
- 检查文件路径是否正确
- 确保txt文件中有有效的JSON数据

**2. 转换失败**
- 检查JSON格式是否正确
- 查看日志中的具体错误信息

**3. 保存失败**
- 检查数据库连接
- 确认字段长度限制
- 查看是否有约束违反

**4. 重复数据**
- 系统自动跳过已存在的注册证号
- 使用reimport接口可以先清理再导入

## 📈 **性能优化**

- 使用批量保存 `saveAll()` 提高效率
- 事务管理避免部分失败影响整体
- 内存优化处理大量数据
- 详细的进度日志便于监控
