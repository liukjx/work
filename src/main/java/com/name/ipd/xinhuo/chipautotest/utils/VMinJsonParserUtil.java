
package com.name.ipd.xinhuo.chipautotest.utils;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;

import java.util.*;

/**
 * VMin JSON数据解析工具类
 * 用于解析VMin测试结果的JSON数据，动态生成Excel列配置
 *
 * @author claude
 * @since 2025/8/3
 */
public class VMinJsonParserUtil {

    /**
     * 从JSON字符串中提取所有动态键值对，生成列配置
     *
     * @param jsonStr JSON字符串
     * @param groupName 分组名称
     * @param startOrderNum 起始列序号
     * @return 列配置列表
     */
    public static List<ExcelExportEntity> extractDynamicColumns(String jsonStr, String groupName, int startOrderNum) {
        List<ExcelExportEntity> dynamicColumns = new ArrayList<>();
        
        if (StrUtil.isBlank(jsonStr)) {
            return dynamicColumns;
        }
        
        try {
            Map<String, Object> jsonMap = JSONUtil.toBean(jsonStr, Map.class);
            return extractDynamicColumnsFromMap(jsonMap, groupName, startOrderNum);
        } catch (Exception e) {
            return dynamicColumns;
        }
    }

    /**
     * 从Map中提取所有动态键值对，生成列配置
     *
     * @param jsonMap JSON数据Map
     * @param groupName 分组名称
     * @param startOrderNum 起始列序号
     * @return 列配置列表
     */
    public static List<ExcelExportEntity> extractDynamicColumnsFromMap(Map<String, Object> jsonMap, String groupName, int startOrderNum) {
        List<ExcelExportEntity> dynamicColumns = new ArrayList<>();
        
        if (jsonMap == null || jsonMap.isEmpty()) {
            return dynamicColumns;
        }
        
        int orderNum = startOrderNum;
        
        // 遍历所有键值对
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 跳过已知的固定字段，这些字段已经在其他地方处理
            if (isFixedField(key)) {
                continue;
            }
            
            // 创建列配置
            ExcelExportEntity column = createColumnConfig(key, value, groupName, ++orderNum);
            if (column != null) {
                dynamicColumns.add(column);
            }
        }
        
        return dynamicColumns;
    }

    /**
     * 从JSON字符串中提取所有键值对，生成数据Map
     *
     * @param jsonStr JSON字符串
     * @param prefixKey 键前缀，用于区分不同频点的数据
     * @return 数据Map
     */
    public static Map<String, Object> extractDataMap(String jsonStr, String prefixKey) {
        Map<String, Object> dataMap = new HashMap<>();
        
        if (StrUtil.isBlank(jsonStr)) {
            return dataMap;
        }
        
        try {
            Map<String, Object> jsonMap = JSONUtil.toBean(jsonStr, Map.class);
            return extractDataMapFromJson(jsonMap, prefixKey);
        } catch (Exception e) {
            return dataMap;
        }
    }

    /**
     * 从Map中提取所有键值对，生成数据Map
     *
     * @param jsonMap JSON数据Map
     * @param prefixKey 键前缀
     * @return 数据Map
     */
    public static Map<String, Object> extractDataMapFromJson(Map<String, Object> jsonMap, String prefixKey) {
        Map<String, Object> dataMap = new HashMap<>();
        
        if (jsonMap == null || jsonMap.isEmpty()) {
            return dataMap;
        }
        
        // 遍历所有键值对
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 跳过已知的固定字段
            if (isFixedField(key)) {
                continue;
            }
            
            // 构建列名
            String columnKey = prefixKey + key;
            dataMap.put(columnKey, value);
        }
        
        return dataMap;
    }

    /**
     * 判断是否为固定字段（不需要动态生成的字段）
     *
     * @param key 字段名
     * @return 是否为固定字段
     */
    private static boolean isFixedField(String key) {
        // 已知的固定字段，这些字段已经在代码中显式处理
        Set<String> fixedFields = new HashSet<>(Arrays.asList(
            "vmin", "tsonser", "keyWord", "name"
        ));
        return fixedFields.contains(key);
    }

    /**
     * 创建列配置
     *
     * @param key 字段名
     * @param value 字段值
     * @param groupName 分组名称
     * @param orderNum 列序号
     * @return 列配置
     */
    private static ExcelExportEntity createColumnConfig(String key, Object value, String groupName, int orderNum) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        
        ExcelExportEntity column = new ExcelExportEntity();
        column.setKey(groupName + "|" + key);
        column.setName(key);
        column.setGroupName(groupName);
        column.setOrderNum(orderNum);
        
        // 根据值的类型设置列类型
        if (value instanceof Number) {
            column.setType(10); // DOUBLE_TYPE
        } else if (value instanceof Boolean) {
            column.setType(1); // BOOLEAN_TYPE
        } else {
            column.setType(2); // STRING_TYPE
        }
        
        return column;
    }

    /**
     * 获取所有可能的键名（用于列配置）
     *
     * @param jsonSamples JSON样本集合
     * @return 所有键名的集合
     */
    public static Set<String> getAllPossibleKeys(List<String> jsonSamples) {
        Set<String> allKeys = new LinkedHashSet<>();
        
        if (CollectionUtil.isEmpty(jsonSamples)) {
            return allKeys;
        }
        
        for (String jsonStr : jsonSamples) {
            if (StrUtil.isBlank(jsonStr)) {
                continue;
            }
            
            try {
                Map<String, Object> jsonMap = JSONUtil.toBean(jsonStr, Map.class);
                if (jsonMap != null) {
                    allKeys.addAll(jsonMap.keySet());
                }
            } catch (Exception e) {
                // 忽略解析失败的样本
            }
        }
        
        return allKeys;
    }
}