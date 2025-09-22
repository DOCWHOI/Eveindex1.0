<template>
  <div class="keyword-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1>🔍 关键词管理系统</h1>
        <p>管理爬虫搜索关键词，支持添加、编辑、删除和批量操作</p>
      </div>
      <div class="header-actions">
        <a-space>
          <a-button @click="refreshKeywords" :loading="loading" type="primary">
            <template #icon>
              <ReloadOutlined />
            </template>
            刷新
          </a-button>
          <a-button @click="showAddModal" type="primary">
            <template #icon>
              <PlusOutlined />
            </template>
            添加关键词
          </a-button>
          <a-button @click="showBatchModal" :disabled="keywords.length === 0">
            <template #icon>
              <EditOutlined />
            </template>
            批量编辑
          </a-button>
          <a-button @click="clearAllKeywords" :disabled="keywords.length === 0" danger>
            <template #icon>
              <DeleteOutlined />
            </template>
            清空全部
          </a-button>
        </a-space>
      </div>
    </div>

    <!-- 统计信息 -->
    <div class="stats-section">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="总关键词数"
              :value="keywords.length"
              :value-style="{ color: '#1890ff' }"
            >
              <template #prefix>
                <FileTextOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="平均长度"
              :value="averageLength"
              :precision="1"
              :value-style="{ color: '#52c41a' }"
            >
              <template #prefix>
                <BarChartOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="最长关键词"
              :value="maxLength"
              :value-style="{ color: '#fa8c16' }"
            >
              <template #prefix>
                <LineChartOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="最短关键词"
              :value="minLength"
              :value-style="{ color: '#eb2f96' }"
            >
              <template #prefix>
                <MinusOutlined />
              </template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 关键词列表 -->
    <div class="keywords-section">
      <a-card title="关键词列表" :bordered="false">
        <template #extra>
          <a-space>
            <a-input-search
              v-model:value="searchText"
              placeholder="搜索关键词"
              style="width: 200px"
              @search="handleSearch"
              allow-clear
            />
            <a-button @click="exportKeywords" :disabled="keywords.length === 0">
              <template #icon>
                <DownloadOutlined />
              </template>
              导出
            </a-button>
          </a-space>
        </template>

        <div v-if="loading" class="loading-container">
          <a-spin size="large" />
          <p>加载中...</p>
        </div>

        <div v-else-if="filteredKeywords.length === 0" class="empty-container">
          <a-empty description="暂无关键词数据">
            <a-button type="primary" @click="showAddModal">
              添加第一个关键词
            </a-button>
          </a-empty>
        </div>

        <div v-else class="keywords-list">
          <div 
            v-for="(keyword, index) in filteredKeywords" 
            :key="index"
            class="keyword-item"
          >
            <div class="keyword-content">
              <div class="keyword-index">{{ getOriginalIndex(index) + 1 }}</div>
              <div class="keyword-text" :title="keyword">{{ keyword }}</div>
              <div class="keyword-stats">
                <a-tag size="small" color="blue">{{ keyword.length }} 字符</a-tag>
              </div>
            </div>
            <div class="keyword-actions">
              <a-space>
                <a-button 
                  size="small" 
                  @click="editKeyword(getOriginalIndex(index), keyword)"
                  type="primary"
                  ghost
                >
                  <template #icon>
                    <EditOutlined />
                  </template>
                  编辑
                </a-button>
                <a-button 
                  size="small" 
                  @click="deleteKeyword(getOriginalIndex(index), keyword)"
                  danger
                  ghost
                >
                  <template #icon>
                    <DeleteOutlined />
                  </template>
                  删除
                </a-button>
              </a-space>
            </div>
          </div>
        </div>
      </a-card>
    </div>

    <!-- 添加关键词模态框 -->
    <a-modal
      v-model:open="addModalVisible"
      title="添加关键词"
      @ok="handleAddKeyword"
      :confirm-loading="addLoading"
    >
      <a-form :model="newKeyword" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="关键词" required>
          <a-input
            v-model:value="newKeyword.keyword"
            placeholder="请输入关键词"
            @press-enter="handleAddKeyword"
            ref="keywordInput"
          />
        </a-form-item>
        <a-form-item label="预览">
          <div class="keyword-preview">
            <a-tag color="blue">{{ newKeyword.keyword || '请输入关键词' }}</a-tag>
            <span v-if="newKeyword.keyword" class="length-info">
              ({{ newKeyword.keyword.length }} 字符)
            </span>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 编辑关键词模态框 -->
    <a-modal
      v-model:open="editModalVisible"
      title="编辑关键词"
      @ok="handleEditKeyword"
      :confirm-loading="editLoading"
    >
      <a-form :model="editingKeyword" :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
        <a-form-item label="索引">
          <a-input :value="editingKeyword.index + 1" disabled />
        </a-form-item>
        <a-form-item label="关键词" required>
          <a-input
            v-model:value="editingKeyword.keyword"
            placeholder="请输入关键词"
            @press-enter="handleEditKeyword"
            ref="editKeywordInput"
          />
        </a-form-item>
        <a-form-item label="预览">
          <div class="keyword-preview">
            <a-tag color="green">{{ editingKeyword.keyword || '请输入关键词' }}</a-tag>
            <span v-if="editingKeyword.keyword" class="length-info">
              ({{ editingKeyword.keyword.length }} 字符)
            </span>
          </div>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 批量编辑模态框 -->
    <a-modal
      v-model:open="batchModalVisible"
      title="批量编辑关键词"
      width="800px"
      @ok="handleBatchUpdate"
      :confirm-loading="batchLoading"
    >
      <div class="batch-edit-content">
        <a-alert
          message="批量编辑说明"
          description="每行一个关键词，空行将被忽略，重复的关键词将被去重。"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        
        <a-textarea
          v-model:value="batchKeywordsText"
          placeholder="请输入关键词，每行一个"
          :rows="15"
          style="font-family: monospace;"
        />
        
        <div class="batch-stats" style="margin-top: 16px;">
          <a-space>
            <span>总行数: {{ batchKeywordsText.split('\n').length }}</span>
            <span>有效关键词: {{ getValidBatchKeywords().length }}</span>
            <span>重复关键词: {{ getDuplicateBatchKeywords().length }}</span>
          </a-space>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { message, Modal } from 'ant-design-vue';
import {
  ReloadOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  FileTextOutlined,
  BarChartOutlined,
  LineChartOutlined,
  MinusOutlined,
  DownloadOutlined
} from '@ant-design/icons-vue';
import {
  getAllKeywords,
  addKeyword,
  updateKeyword,
  deleteKeyword,
  batchUpdateKeywords,
  clearAllKeywords
} from '@/api/keywordManagement';

// 响应式数据
const loading = ref(false);
const addLoading = ref(false);
const editLoading = ref(false);
const batchLoading = ref(false);
const keywords = ref<string[]>([]);
const searchText = ref('');
const addModalVisible = ref(false);
const editModalVisible = ref(false);
const batchModalVisible = ref(false);

// 表单数据
const newKeyword = ref({ keyword: '' });
const editingKeyword = ref({ index: -1, keyword: '' });
const batchKeywordsText = ref('');

// 计算属性
const filteredKeywords = computed(() => {
  if (!searchText.value) {
    return keywords.value;
  }
  return keywords.value.filter(keyword => 
    keyword.toLowerCase().includes(searchText.value.toLowerCase())
  );
});

const averageLength = computed(() => {
  if (keywords.value.length === 0) return 0;
  const totalLength = keywords.value.reduce((sum, keyword) => sum + keyword.length, 0);
  return totalLength / keywords.value.length;
});

const maxLength = computed(() => {
  if (keywords.value.length === 0) return 0;
  return Math.max(...keywords.value.map(keyword => keyword.length));
});

const minLength = computed(() => {
  if (keywords.value.length === 0) return 0;
  return Math.min(...keywords.value.map(keyword => keyword.length));
});

// 方法
const refreshKeywords = async () => {
  loading.value = true;
  try {
    const response = await getAllKeywords();
    if (response.success) {
      keywords.value = response.keywords || [];
      message.success(`成功加载 ${keywords.value.length} 个关键词`);
    } else {
      message.error(response.message || '加载关键词失败');
    }
  } catch (error: any) {
    console.error('加载关键词失败:', error);
    message.error('加载关键词失败: ' + error.message);
  } finally {
    loading.value = false;
  }
};

const showAddModal = () => {
  newKeyword.value.keyword = '';
  addModalVisible.value = true;
  // 聚焦到输入框
  setTimeout(() => {
    const input = document.querySelector('.ant-modal .ant-input') as HTMLInputElement;
    if (input) input.focus();
  }, 100);
};

const handleAddKeyword = async () => {
  if (!newKeyword.value.keyword.trim()) {
    message.warning('请输入关键词');
    return;
  }
  
  addLoading.value = true;
  try {
    const response = await addKeyword(newKeyword.value.keyword.trim());
    if (response.success) {
      message.success('关键词添加成功');
      addModalVisible.value = false;
      await refreshKeywords();
    } else {
      message.error(response.message || '添加关键词失败');
    }
  } catch (error: any) {
    console.error('添加关键词失败:', error);
    message.error('添加关键词失败: ' + error.message);
  } finally {
    addLoading.value = false;
  }
};

const editKeyword = (index: number, keyword: string) => {
  editingKeyword.value = { index, keyword };
  editModalVisible.value = true;
  // 聚焦到输入框
  setTimeout(() => {
    const input = document.querySelector('.ant-modal .ant-input') as HTMLInputElement;
    if (input) input.focus();
  }, 100);
};

const handleEditKeyword = async () => {
  if (!editingKeyword.value.keyword.trim()) {
    message.warning('请输入关键词');
    return;
  }
  
  editLoading.value = true;
  try {
    const response = await updateKeyword(editingKeyword.value.index, editingKeyword.value.keyword.trim());
    if (response.success) {
      message.success('关键词更新成功');
      editModalVisible.value = false;
      await refreshKeywords();
    } else {
      message.error(response.message || '更新关键词失败');
    }
  } catch (error: any) {
    console.error('更新关键词失败:', error);
    message.error('更新关键词失败: ' + error.message);
  } finally {
    editLoading.value = false;
  }
};

const deleteKeyword = (index: number, keyword: string) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除关键词 "${keyword}" 吗？`,
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const response = await deleteKeyword(index);
        if (response.success) {
          message.success('关键词删除成功');
          await refreshKeywords();
        } else {
          message.error(response.message || '删除关键词失败');
        }
      } catch (error: any) {
        console.error('删除关键词失败:', error);
        message.error('删除关键词失败: ' + error.message);
      }
    }
  });
};

const showBatchModal = () => {
  batchKeywordsText.value = keywords.value.join('\n');
  batchModalVisible.value = true;
};

const getValidBatchKeywords = () => {
  return batchKeywordsText.value
    .split('\n')
    .map(line => line.trim())
    .filter(line => line.length > 0);
};

const getDuplicateBatchKeywords = () => {
  const validKeywords = getValidBatchKeywords();
  const seen = new Set();
  return validKeywords.filter(keyword => {
    if (seen.has(keyword)) {
      return true;
    }
    seen.add(keyword);
    return false;
  });
};

const handleBatchUpdate = async () => {
  const validKeywords = getValidBatchKeywords();
  if (validKeywords.length === 0) {
    message.warning('请输入至少一个关键词');
    return;
  }
  
  batchLoading.value = true;
  try {
    const response = await batchUpdateKeywords(validKeywords);
    if (response.success) {
      message.success(`批量更新成功，共 ${validKeywords.length} 个关键词`);
      batchModalVisible.value = false;
      await refreshKeywords();
    } else {
      message.error(response.message || '批量更新失败');
    }
  } catch (error: any) {
    console.error('批量更新失败:', error);
    message.error('批量更新失败: ' + error.message);
  } finally {
    batchLoading.value = false;
  }
};

const clearAllKeywords = () => {
  Modal.confirm({
    title: '确认清空',
    content: `确定要清空所有 ${keywords.value.length} 个关键词吗？此操作不可恢复！`,
    okText: '清空',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const response = await clearAllKeywords();
        if (response.success) {
          message.success('所有关键词已清空');
          await refreshKeywords();
        } else {
          message.error(response.message || '清空关键词失败');
        }
      } catch (error: any) {
        console.error('清空关键词失败:', error);
        message.error('清空关键词失败: ' + error.message);
      }
    }
  });
};

const handleSearch = () => {
  // 搜索逻辑已在计算属性中处理
};

const getOriginalIndex = (filteredIndex: number) => {
  const filteredKeyword = filteredKeywords.value[filteredIndex];
  return keywords.value.indexOf(filteredKeyword);
};

const exportKeywords = () => {
  const content = keywords.value.join('\n');
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `searchkeywords_${new Date().toISOString().split('T')[0]}.txt`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  message.success('关键词已导出');
};

// 生命周期
onMounted(() => {
  refreshKeywords();
});
</script>

<style scoped>
.keyword-management {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.header-content h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #262626;
}

.header-content p {
  margin: 0;
  color: #8c8c8c;
  font-size: 14px;
}

.stats-section {
  margin-bottom: 24px;
}

.keywords-section {
  width: 100%;
}

.loading-container {
  text-align: center;
  padding: 40px 0;
}

.empty-container {
  text-align: center;
  padding: 40px 0;
}

.keywords-list {
  max-height: 600px;
  overflow-y: auto;
}

.keyword-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #fff;
  transition: all 0.3s ease;
}

.keyword-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-color: #1890ff;
}

.keyword-content {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.keyword-index {
  width: 40px;
  height: 40px;
  background: #f0f0f0;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #666;
  margin-right: 12px;
  flex-shrink: 0;
}

.keyword-text {
  flex: 1;
  font-size: 14px;
  color: #262626;
  margin-right: 12px;
  word-break: break-all;
  min-width: 0;
}

.keyword-stats {
  flex-shrink: 0;
}

.keyword-actions {
  flex-shrink: 0;
}

.keyword-preview {
  display: flex;
  align-items: center;
  gap: 8px;
}

.length-info {
  color: #8c8c8c;
  font-size: 12px;
}

.batch-edit-content {
  padding: 16px 0;
}

.batch-stats {
  color: #8c8c8c;
  font-size: 12px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .header-actions {
    width: 100%;
  }
  
  .keyword-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .keyword-content {
    width: 100%;
  }
  
  .keyword-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>



























