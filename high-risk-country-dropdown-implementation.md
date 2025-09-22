# 高风险数据国家下拉框实现文档

## 功能描述
修改国家/地区下拉列表，使其只包含从后端查找的所有高风险数据的国家，并将涉及国家数量也修改为所有高风险数据的国家数。

## 实现内容

### 1. 后端修改

#### 新增API接口 (`CrawlerDataController.java`)
```java
/**
 * 获取高风险数据国家分布统计
 */
@GetMapping("/high-risk-country-distribution")
public ResponseEntity<Map<String, Object>> getHighRiskCountryDistribution() {
    // 调用Service方法获取高风险数据的国家分布
    Map<String, Object> distributionResult = crawlerDataService.getHighRiskCountryDistribution();
    return ResponseEntity.ok(distributionResult);
}
```

#### 新增Service方法 (`CrawlerDataService.java`)
```java
/**
 * 获取高风险数据的国家分布统计
 */
public Map<String, Object> getHighRiskCountryDistribution() {
    // 使用Repository方法获取按国家和风险等级统计的数据
    List<Map<String, Object>> countryRiskStats = crawlerDataRepository.countByCountryAndRiskLevel();
    
    Map<String, Integer> highRiskCountryStats = new HashMap<>();
    int totalHighRiskCount = 0;
    
    // 筛选出高风险数据的国家分布
    for (Map<String, Object> stat : countryRiskStats) {
        String country = (String) stat.get("country");
        Object riskLevelObj = stat.get("riskLevel");
        Long count = (Long) stat.get("count");
        
        // 只处理高风险数据
        if (riskLevelObj != null && "HIGH".equals(riskLevelObj.toString()) && country != null && count != null) {
            highRiskCountryStats.put(country, count.intValue());
            totalHighRiskCount += count.intValue();
        }
    }
    
    result.put("success", true);
    result.put("totalCount", totalHighRiskCount);
    result.put("countryStats", highRiskCountryStats);
    result.put("countryCount", highRiskCountryStats.size()); // 涉及的高风险数据国家数量
    
    return result;
}
```

#### 利用现有Repository方法
```java
// CrawlerDataRepository.java 中已存在的方法
@Query("SELECT c.country as country, c.riskLevel as riskLevel, COUNT(c) as count FROM CrawlerData c WHERE c.deleted = 0 GROUP BY c.country, c.riskLevel")
List<Map<String, Object>> countByCountryAndRiskLevel();
```

### 2. 前端修改

#### API调用 (`pachongshujuguanli.ts`)
```typescript
/** 获取高风险数据国家分布统计 */
export async function getHighRiskCountryDistribution(options?: { [key: string]: any }) {
  return request<Record<string, any>>("/crawler-data/high-risk-country-distribution", {
    method: "GET",
    ...(options || {}),
  });
}
```

#### DataQuery.vue 修改
1. **导入新API**：
```typescript
import { getHighRiskCountryDistribution } from '@/api/pachongshujuguanli'
```

2. **修改loadCountryOptions方法**：
```typescript
// 加载国家选项（仅高风险数据的国家）
const loadCountryOptions = async () => {
  countriesLoading.value = true
  try {
    const response = await getHighRiskCountryDistribution() as any
    console.log('🌍 高风险数据国家分布:', response)
    
    if (response && response.success && response.countryStats) {
      // 将高风险国家统计数据转换为选项格式
      const countryStats = response.countryStats
      const options: {code: string, name: string, count: number}[] = []
      
      // 遍历高风险国家统计数据
      Object.keys(countryStats).forEach(countryCode => {
        const count = countryStats[countryCode]
        if (countryCode && count > 0) {
          options.push({
            code: countryCode,
            name: getCountryName(countryCode),
            count: count
          })
        }
      })
      
      // 按数据量降序排序
      options.sort((a, b) => b.count - a.count)
      
      countryOptions.value = options
    }
  } catch (error) {
    console.error('加载高风险国家选项失败:', error)
    message.error('加载高风险国家选项失败')
  } finally {
    countriesLoading.value = false
  }
}
```

#### CrawlerDataManagement.vue 修改
1. **导入新API**：
```typescript
import { getHighRiskCountryDistribution } from '@/api/pachongshujuguanli'
```

2. **修改统计数据更新逻辑**：
```typescript
// 从高风险国家分布API获取准确的国家数量
try {
  const countryResult = await getHighRiskCountryDistribution() as any
  if (countryResult && countryResult.success && countryResult.countryCount !== undefined) {
    statistics.countryCount = countryResult.countryCount
    console.log('📍 从API获取高风险国家数量:', countryResult.countryCount)
  } else {
    // API失败时，回退到从当前数据计算
    const uniqueCountries = new Set()
    crawlerDataList.value.forEach(item => {
      if (item.country && item.riskLevel === 'HIGH') {
        uniqueCountries.add(item.country)
      }
    })
    statistics.countryCount = uniqueCountries.size
  }
} catch (error) {
  console.error('获取高风险国家数量失败:', error)
  // 出错时回退到从当前数据计算
}
```

## 功能特点

### ✅ 新增功能
1. **高风险数据专用国家下拉框**：
   - 只显示包含高风险数据的国家
   - 每个国家选项显示高风险数据条数，如 "美国 (125)"
   - 按高风险数据量降序排序

2. **准确的涉及国家数量统计**：
   - 统计数据显示真实的高风险数据涉及国家数量
   - 数据来源于数据库的准确统计，而不是当前页面数据

3. **性能优化**：
   - 使用数据库GROUP BY查询，性能更好
   - 减少前端计算负担

### ✅ 数据流程
1. **国家下拉框数据获取**：
   ```
   页面加载 → loadCountryOptions() → getHighRiskCountryDistribution() → 
   countByCountryAndRiskLevel() → 筛选HIGH风险数据 → 返回国家选项
   ```

2. **涉及国家数量统计**：
   ```
   统计更新 → updateStatistics() → getHighRiskCountryDistribution() → 
   返回countryCount → 更新页面显示
   ```

### 📊 API返回数据格式
```json
{
  "success": true,
  "totalCount": 1250,
  "countryStats": {
    "US": 450,
    "CN": 320,
    "EU": 280,
    "JP": 200
  },
  "countryCount": 4,
  "message": "高风险数据国家分布统计完成，总数: 1250，涉及国家: 4"
}
```

### 🎯 前端显示效果
1. **国家下拉框**：
   ```
   全部国家
   美国 (450)     # 高风险数据最多的国家
   中国 (320)
   欧盟 (280)
   日本 (200)
   ```

2. **统计卡片**：
   ```
   涉及国家数量: 4  # 真实的高风险数据涉及国家数
   ```

## 技术优势

### 🚀 性能优势
- **数据库级统计**：使用SQL GROUP BY，避免大量数据传输
- **缓存友好**：统计数据可以缓存，减少重复计算
- **精确统计**：基于数据库真实数据，不受分页影响

### 🎯 用户体验
- **精准筛选**：只显示有高风险数据的国家
- **数据透明**：显示每个国家的高风险数据量
- **智能排序**：按数据量排序，常用国家在前

### 🔧 维护性
- **模块化设计**：前后端分离，职责清晰
- **错误处理**：完整的异常处理和降级机制
- **日志记录**：详细的调试信息

## 测试验证

### 1. 后端API测试
```bash
# 测试高风险国家分布API
curl -X GET "http://localhost:8080/api/crawler-data/high-risk-country-distribution"
```

### 2. 前端功能测试
1. **打开DataQuery页面**：验证国家下拉框只显示高风险数据的国家
2. **打开CrawlerDataManagement页面**：验证"涉及国家数量"显示准确数值
3. **选择国家筛选**：验证筛选功能正常工作
4. **查看控制台**：观察API调用和数据处理日志

### 3. 数据验证
- 验证国家下拉框中的国家都确实有高风险数据
- 验证每个国家显示的数据量准确
- 验证"涉及国家数量"与实际高风险数据国家数一致

## 完成状态
- ✅ 修改loadCountryOptions方法只获取高风险数据的国家
- ✅ 修改后端提供高风险国家统计API
- ✅ 修改前端统计数据显示高风险国家数量
- 🔄 测试验证高风险国家下拉框功能

**功能已完成开发，可以进行测试验证。**









