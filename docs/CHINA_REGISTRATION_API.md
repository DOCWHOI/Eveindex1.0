# 中国医疗器械注册API使用说明

## 📋 **API概述**

中国上市医疗器械爬虫，通过产品备案/注册证号实时调取全国药品上市信息数据。

- **数据来源**: 全国31个省级行政区、100+地市级药监部门
- **API提供商**: https://open.bcpmdata.com
- **API Key**: `sk-acbc13b3d4ea353caa2142be3e22157a282dabbf`
- **过期时间**: 2025-09-22 23:59:59

## 🔧 **使用方法**

### 1. **基本搜索**

#### 根据产品名称搜索
```java
@Autowired
private cn_registration_api chinaRegistrationApi;

// 搜索血糖仪相关产品
ChinaListedResponse response = chinaRegistrationApi.searchByProductName("血糖仪", 1);
if (response != null && response.getList() != null) {
    log.info("找到 {} 条记录", response.getTotal());
    for (MedicalDevice device : response.getList()) {
        chinaRegistrationApi.printDeviceInfo(device);
    }
}
```

#### 根据注册证号搜索
```java
SearchParams params = new SearchParams();
params.setRegistrationNumberRemark("国械注准20210001");
ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, 1);
```

### 2. **高级搜索**

#### 多条件组合搜索
```java
SearchParams params = new SearchParams();
params.setCategory("Ⅲ");           // 三类医疗器械
params.setType("进口");             // 进口产品
params.setWhetherYibao("是");       // 纳入医保
params.setProductState("有效");     // 有效状态

ChinaListedResponse response = chinaRegistrationApi.getChinaListedDevices(params, 1);
```

### 3. **批量获取数据**

```java
// 批量获取最多5页数据
SearchParams params = new SearchParams();
params.setProductName("血压");
ChinaListedResponse allData = chinaRegistrationApi.getAllChinaListedDevices(params, 5);

log.info("批量获取完成，总计 {} 条记录", allData.getList().size());
```

## 🌐 **REST API接口**

### 1. **测试连接**
```http
GET /api/china-registration/test
```

### 2. **产品名称搜索**
```http
GET /api/china-registration/search/product?productName=血糖仪&page=1
```

### 3. **注册证号搜索**
```http
GET /api/china-registration/search/registration?registrationNumber=国械注准&page=1
```

### 4. **管理类别查询**
```http
GET /api/china-registration/category/Ⅲ?page=1
```

### 5. **类型查询**
```http
GET /api/china-registration/type/国产?page=1
```

### 6. **高级搜索**
```http
POST /api/china-registration/search/advanced?page=1
Content-Type: application/json

{
    "category": "Ⅲ",
    "type": "进口",
    "whetherYibao": "是",
    "productState": "有效"
}
```

### 7. **批量获取**
```http
GET /api/china-registration/batch?maxPages=3&category=Ⅲ&type=国产
```

## 📊 **数据字段说明**

### MedicalDevice 主要字段：

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `productName` | String | 产品名称 |
| `registrationNumber` | String | 产品备案/注册证号 |
| `manufacturerRe` | String | 注册/备案人名称 |
| `category` | String | 管理类别（Ⅱ、Ⅲ） |
| `type` | String | 国产/进口 |
| `scopeAndUse` | String | 适用范围/预期用途 |
| `approvalDate` | String | 批准日期/备案日期 |
| `validUntil` | String | 有效期至 |
| `productState` | String | 器械状态（已注销、已过期、有效） |
| `whetherYibao` | String | 是否纳入医保（是、否） |
| `province` | String | 省份 |
| `city` | String | 城市 |

### SearchParams 搜索参数：

| 参数名 | 类型 | 说明 | 可选值 |
|--------|------|------|--------|
| `productName` | String | 产品名称 | - |
| `manufacturerRe` | String | 注册/备案人名称 | - |
| `registrationNumberRemark` | String | 产品备案/注册证号 | - |
| `category` | String | 管理类别 | Ⅱ、Ⅲ |
| `type` | String | 国产/进口 | 国产、进口 |
| `productState` | String | 器械状态 | 已注销、已过期、有效 |
| `whetherYibao` | String | 是否纳入医保 | 是、否 |

## ⚠️ **注意事项**

1. **API Key管理**
   - 当前API Key将于 2025-09-22 23:59:59 过期
   - 请及时更新API Key
   - 不要将API Key提交到公共仓库

2. **请求频率限制**
   - 建议在请求间添加1秒延迟
   - 避免短时间内大量请求

3. **数据使用**
   - 当前版本不保存到数据库
   - 仅用于数据获取和测试
   - 如需持久化，请参考其他爬虫实现

4. **错误处理**
   - API可能返回空数据
   - 网络异常时会返回null
   - 建议添加重试机制

## 🧪 **测试方法**

### 运行单元测试
```bash
cd spring-boot-backend
mvn test -Dtest=ChinaRegistrationApiTest
```

### 通过Controller测试
```bash
# 启动Spring Boot应用
mvn spring-boot:run

# 测试API连接
curl http://localhost:8080/api/china-registration/test

# 搜索产品
curl "http://localhost:8080/api/china-registration/search/product?productName=血糖仪"
```

## 📝 **开发计划**

- [ ] 添加数据持久化功能
- [ ] 集成到现有的设备数据管理系统
- [ ] 添加定时任务自动更新数据
- [ ] 实现增量更新机制
- [ ] 添加数据质量验证
- [ ] 集成到前端DeviceData.vue页面
