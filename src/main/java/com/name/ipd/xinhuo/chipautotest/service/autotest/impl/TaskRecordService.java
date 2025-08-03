

package com.name.ipd.xinhuo.chipautotest.service.autotest.impl;

import com.com.rename.ipd.hhh.chipautotest.model.autotest.ChipAutoTestTaskInfoVO;
import com.hhh.ipd.xinhuo.chipautotest.dao.autotest.ChipAutoTestDataAnalysisDao;
import com.hhh.ipd.xinhuo.chipautotest.dao.autotest.ChipAutoTestSubtasksDao;
import com.hhh.ipd.xinhuo.chipautotest.dao.autotest.ChipAutoTestTaskInfoDao;
import com.hhh.ipd.xinhuo.chipautotest.dao.configmanage.HpmConfigDao;
import com.hhh.ipd.xinhuo.chipautotest.dao.configmanage.PowerHpmConfigDao;
import com.hhh.ipd.xinhuo.chipautotest.model.BusinessException;
import com.hhh.ipd.xinhuo.chipautotest.model.XinHuoUserInfo;
import com.hhh.ipd.xinhuo.chipautotest.model.autotest.ChipAutoTestSubtasks;
import com.hhh.ipd.xinhuo.chipautotest.model.autotest.ChipAutoTestTaskInfo;
import com.hhh.ipd.xinhuo.chipautotest.model.autotest.PowerDomainHpmVO;
import com.hhh.ipd.xinhuo.chipautotest.model.configmanage.HpmConfigVO;
import com.hhh.ipd.xinhuo.chipautotest.model.configmanage.PowerDomainFrequencyVO;
import com.hhh.ipd.xinhuo.chipautotest.model.constants.CommonConstants;
import com.hhh.ipd.xinhuo.chipautotest.model.record.HpmSheetData;
import com.hhh.ipd.xinhuo.chipautotest.model.record.HpmTestResult;
import com.hhh.ipd.xinhuo.chipautotest.model.record.SheetData;
import com.hhh.ipd.xinhuo.chipautotest.model.record.VMinSheetData;
import com.hhh.ipd.xinhuo.chipautotest.service.autotest.IChipAutoTestSubtasksService;
import com.hhh.ipd.xinhuo.chipautotest.service.autotest.IChipAutoTestTaskInfoService;
import com.hhh.ipd.xinhuo.chipautotest.service.autotest.IDataArchivingService;
import com.hhh.ipd.xinhuo.chipautotest.service.autotest.ITaskRecordService;
import com.hhh.ipd.xinhuo.chipautotest.utils.FileUtils;
import com.hhh.ipd.xinhuo.chipautotest.utils.basicdata.UserUtil;
import com.hhh.ipd.xinhuo.chipautotest.utils.excel.easypoi.ExcelUtil;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.afterturn.easypoi.entity.BaseTypeConstants;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.export.ExcelExportService;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * 任务记录service
 *
 * @author aiqi
 * @since 2023/11/2 17:07
 */
@Slf4j
@Service
public class TaskRecordService implements ITaskRecordService {
    @Value("${pythonService.dataFittingUrl}")
    private String dataFittingUrl;

    @Value("${pythonService.changeHpmDataFittingUrl}")
    private String changeHpmDataFittingUrl;

    @Value("${pythonService.dataFittingExportUrl}")
    private String dataFittingExportUrl;

    @Resource
    private ChipAutoTestTaskInfoDao taskInfoDao;

    @Resource
    private ChipAutoTestSubtasksDao subtasksDao;

    @Resource
    private PowerHpmConfigDao powerHpmConfigDao;

    @Resource
    private HpmConfigDao hpmConfigDao;

    @Resource
    private IDataArchivingService archivingService;

    @Resource
    private IChipAutoTestTaskInfoService testTaskInfoService;

    @Resource
    private IChipAutoTestSubtasksService subtasksService;

    @Resource
    private ChipAutoTestDataAnalysisDao analysisDao;

    /**
     * 数据下载
     *
     * @param taskInfoVO 参数
     * @param response http响应
     * @throws BusinessException 业务异常
     * @author aiqi
     * @date 2023/11/14 11:46
     **/
    @Override
    public void dataDownload(ChipAutoTestTaskInfoVO taskInfoVO, HttpServletResponse response) throws BusinessException {
        log.info(String.format("-----------------------数据下载开始，%s", JSONUtil.toJsonStr(taskInfoVO)));
        String testType = taskInfoVO.getTestType();

        if (StrUtil.isBlank(testType)) {
            throw new BusinessException("测试类型不能为空！");
        }

        if (CommonConstants.CHIP_TEST_TYPE_LD.equals(taskInfoVO.getTestType()) && taskInfoVO.getIsTaskGroup() == null) {
            throw new BusinessException("是否是任务组不能为空！");
        }

        Integer taskId = null;
        if (!taskInfoVO.getIsTaskGroup()) {
            if (taskInfoVO.getId() == null) {
                throw new BusinessException("任务id不能为空！");
            }
            taskId = taskInfoVO.getId();
            taskInfoVO.setTaskIds(Arrays.asList(taskId));
        }

        // 获取测试结果
        Object taskData = getTaskData(taskInfoVO, true);

        // 1、创建一个Workbook（XSSFWorkbook）
        Workbook workbook = new XSSFWorkbook();

        String fileName;
        LocalDateTime now = LocalDateTime.now();
        if (CommonConstants.CHIP_TEST_TYPE_VMIN.equals(testType)) {
            if (CommonConstants.POWER_DOMAIN_HPM.equals(testType)) {
                // 生成hpm wordbook
                createHpmWorkbook(taskId, taskData, workbook);
                fileName = "hpm测试数据" + now;
            } else {
                // 生成vMin wordbook
                createVMinWorkbook(taskId, taskData, workbook);
                fileName = "vMin测试数据" + now;
            }
        } else if (CommonConstants.CHIP_TEST_TYPE_HPM.equals(testType)) {
            // 生成hpm wordbook
            createHpmWorkbook(taskId, taskData, workbook);
            fileName = "hpm测试数据" + now;
        } else if (CommonConstants.CHIP_TEST_TYPE_LD.equals(testType)) {
            // 生成漏电 wordbook
            workbook = createElectricLeakageWorkbook(taskInfoVO, taskData);
            fileName = "漏电测试数据" + now;
        } else if (CommonConstants.CHIP_TEST_TYPE_WHOLE.equals(testType)) {
            // 生成整芯片 wordbook
            workbook = createWholeChipWorkbook(taskId, taskData);
            fileName = "整芯片测试数据" + now;
        } else {
            throw new BusinessException("没有该测试类型！");
        }

        // 返回文件流，浏览器弹窗下载文件
        FileUtils.browserDownloadExcel(response, fileName, workbook);
        log.info(String.format("-----------------------数据下载完成，%s", JSONUtil.toJsonStr(taskInfoVO)));
    }

    private void createVMinWorkbook(Integer taskId, Object taskData, Workbook workbook) {
        // vMin数据下载
        Map<Integer, Map<String, VMinSheetData>> testDataMap = (Map<Integer, Map<String, VMinSheetData>>) taskData;

        // 根据任务id获取vMin测试数据
        Map<String, VMinSheetData> sheetDataMap = testDataMap.get(taskId);

        // 遍历vMin测试数据创建sheet页
        for (Map.Entry<String, VMinSheetData> vMinSheetDataEntry : sheetDataMap.entrySet()) {
            // 以电源域名称为页签名称
            String powerDomainName = vMinSheetDataEntry.getKey();

            // 获取导出excel配置
            ExportParams exportParams = ExcelUtil.getExportParams(powerDomainName);

            if (CommonConstants.POWER_DOMAIN_HPM.equals(powerDomainName)
                    || CommonConstants.POWER_DOMAIN_PASENSOR.equals(powerDomainName)) {
                // 若是hpm sheet页则跳过
                continue;
            }

            VMinSheetData vMinSheetData = vMinSheetDataEntry.getValue();
            List<Map<String, Object>> rowData = vMinSheetData.getRowData();

            // 描述行，总数据行数+2
            int writeIndex = rowData.size() + 2;

            // 创建sheet页
            ExcelExportService service = new ExcelExportService();
            service.createSheetForMap(workbook, exportParams, vMinSheetData.getExcelExportEntities(), rowData);

            // 增加描述，合并单元格
            mergedCellRegion(powerDomainName, workbook, writeIndex, vMinSheetData);

            // 设置自适应宽度
            Sheet sheet = workbook.getSheet(powerDomainName);
            ExcelUtil.setSizeColumn(sheet);
        }
    }

    private void mergedCellRegion(String powerDomainName, Workbook workbook, int writeIndex,
                                  VMinSheetData vMinSheetData) {
        // 在sheet页，行数据后增加描述
        Sheet sheetAt = workbook.getSheet(powerDomainName);

        // 获取第一行的单元格数量-1
        int columnCount = sheetAt.getRow(0).getLastCellNum() - 1;
        int rowIndex = writeIndex;

        Row testPersonRow = sheetAt.createRow(rowIndex);
        Cell testPersonCell = testPersonRow.createCell(0);
        testPersonCell.setCellValue(CommonConstants.EXPAND_ROWS_TEST_PEOPLE + vMinSheetData.getTestPerson());
        // 创建合并单元格的区域
        CellRangeAddress testPersonRegion = new CellRangeAddress(rowIndex, rowIndex, 0, columnCount);
        // 合并单元格
        sheetAt.addMergedRegion(testPersonRegion);

        rowIndex++;
        Row temperatureRow = sheetAt.createRow(rowIndex);
        Cell temperatureCell = temperatureRow.createCell(0);
        temperatureCell
                .setCellValue(CommonConstants.EXPAND_ROWS_TEMPERATURE + vMinSheetData.getEnvironmentTemperatureList());
        CellRangeAddress temperatureRegion = new CellRangeAddress(rowIndex, rowIndex, 0, columnCount);
        sheetAt.addMergedRegion(temperatureRegion);

        rowIndex++;
        Row testTimeRow = sheetAt.createRow(rowIndex);
        Cell testTimeCell = testTimeRow.createCell(0);
        testTimeCell.setCellValue(CommonConstants.EXPAND_ROWS_TEST_TIME + vMinSheetData.getTestTime());
        CellRangeAddress testTimeRegion = new CellRangeAddress(rowIndex, rowIndex, 0, columnCount);
        sheetAt.addMergedRegion(testTimeRegion);

        rowIndex++;
        Row softwareVersionRow = sheetAt.createRow(rowIndex);
        Cell softwareVersionCell = softwareVersionRow.createCell(0);
        softwareVersionCell.setCellValue(CommonConstants.EXPAND_ROWS_SOFT_VERSION + vMinSheetData.getSoftwareVersion());
        CellRangeAddress softwareVersionRegion = new CellRangeAddress(rowIndex, rowIndex, 0, columnCount);
        sheetAt.addMergedRegion(softwareVersionRegion);
    }

    /**
     * 获取测试结果
     *
     * @param taskInfoVO 任务参数
     * @param isExcel 是否返回excel数据
     * @return T
     * @throws BusinessException 业务异常
     * @author aiqi
     * @date 2023/12/19 20:29
     **/
    @Override
    public <T> T getTaskData(ChipAutoTestTaskInfoVO taskInfoVO, boolean isExcel) throws BusinessException {
        log.info(String.format("-------------------------获取测试任务结果%s开始：", JSONUtil.toJsonStr(taskInfoVO)));
        if (taskInfoVO.getIsTaskGroup() != null && taskInfoVO.getIsTaskGroup()) {
            // 若是任务组
            return (T) getLDTaskData(taskInfoVO);
        } else {
            if (CollectionUtil.isEmpty(taskInfoVO.getTaskIds())) {
                throw new BusinessException("taskIds不能为空！");
            }
        }

        List<Integer> taskIds = taskInfoVO.getTaskIds();

        // 获取任务信息
        List<ChipAutoTestTaskInfo> taskInfoList = taskInfoDao.selectBatchIds(taskIds);

        // 获取测试完成子任务信息
        LambdaQueryWrapper<ChipAutoTestSubtasks> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ChipAutoTestSubtasks::getParentTaskId, taskIds);
        queryWrapper.eq(ChipAutoTestSubtasks::getOperatingStatus, CommonConstants.TASK_STATUS_COMPLETED);
        List<ChipAutoTestSubtasks> subtasksList = subtasksDao.selectList(queryWrapper);

        // 若没有已完成的任务
        if (CollectionUtil.isEmpty(subtasksList)) {
            throw new BusinessException("当前任务没有已完成的测试项！");
        }

        // 获取任务数据
        Map<Integer, Map<String, SheetData>> taskData = getSheetData(taskInfoList, subtasksList, isExcel);

        log.info(String.format("-----------------获取测试任务结果%s完成：%s", taskIds, JSONUtil.toJsonStr(taskData)));
        return (T) taskData;
    }

    private Map<Integer, Map<String, SheetData>> getSheetData(List<ChipAutoTestTaskInfo> taskInfoList,
                                                              List<ChipAutoTestSubtasks> subtasksList, boolean isExcel) {
        // 任务结果，key为任务id，值为任务对应的key为电源域，value为sheet页的Excel数据对象
        Map<Integer, Map<String, SheetData>> taskData = new HashMap<>(3);

        // 对子任务信息按任务id分组
        Map<Integer, List<ChipAutoTestSubtasks>> subtasksGroup = subtasksList.stream()
                .collect(Collectors.groupingBy(ChipAutoTestSubtasks::getParentTaskId));

        // 遍历任务id组装任务数据
        for (ChipAutoTestTaskInfo taskInfo : taskInfoList) {
            Integer taskInfoId = taskInfo.getId();

            // 获取任务的子任务数据
            List<ChipAutoTestSubtasks> subtasks = subtasksGroup.get(taskInfoId);

            if (CollectionUtil.isEmpty(subtasks)) {
                continue;
            }

            String testType = taskInfo.getTestType();
            Map<String, SheetData> sheetDataMap = new HashMap<>(1);
            if (CommonConstants.CHIP_TEST_TYPE_VMIN.equals(testType)) {
                // vMin sheet页数据组装
                Map<String, SheetData> vminSheetDataMap = vMinDataAssemble(taskInfo, subtasks, isExcel);
                sheetDataMap.putAll(vminSheetDataMap);

                // hpm sheet页数据组装
                Map<String, SheetData> hpmSheetDataMap = hpmDataAssemble(taskInfo, subtasks);
                sheetDataMap.putAll(hpmSheetDataMap);
            } else if (CommonConstants.CHIP_TEST_TYPE_HPM.equals(testType)) {
                // hpm sheet页数据组装
                sheetDataMap = hpmDataAssemble(taskInfo, subtasks);
            } else if (CommonConstants.CHIP_TEST_TYPE_LD.equals(testType)) {
                // 漏电 sheet页数据组装
                sheetDataMap = ldDataAssemble(subtasks);
            } else if (CommonConstants.CHIP_TEST_TYPE_WHOLE.equals(testType)) {
                // 整芯片 sheet页数据组装
                sheetDataMap = wholeChipDataAssemble(taskInfo, subtasks);
            } else {
                return taskData;
            }
            taskData.put(taskInfoId, sheetDataMap);
        }

        return taskData;
    }

    private Map<String, SheetData> hpmDataAssemble(ChipAutoTestTaskInfo taskInfo, List<ChipAutoTestSubtasks> subtasks) {
        // 获取温度list
        ChipAutoTestTaskInfoVO taskInfoVO = JSONUtil.toBean(taskInfo.getSubmitTaskParam(),
                ChipAutoTestTaskInfoVO.class);

        // 过滤选择框数据中未选择的数据
        AutoTestService.filterSelectedData(taskInfoVO);

        // 获取温度数据并排序
        List<Double> temperatureList = taskInfoVO.getEnvironmentTemperatureList();
        if (CollectionUtil.isNotEmpty(temperatureList)) {
            temperatureList = temperatureList.stream().sorted(Comparator.comparing(Double::doubleValue))
                    .collect(Collectors.toList());
        }

        // 获取公共sheet页列信息
        List<ExcelExportEntity> excelExportEntities = getCommonColum();

        // hpm sheet
        Map<String, SheetData> sheetDataMap = new HashMap<>();

        // 获取子任务中的hpm数据,并按单板分组
        Map<String, List<ChipAutoTestSubtasks>> boardGroup = subtasks.stream()
                .filter(subtasks1 -> CommonConstants.POWER_DOMAIN_HPM.equals(subtasks1.getPowerDomainName()))
                .collect(Collectors.groupingBy(ChipAutoTestSubtasks::getBoardId));

        // 获取芯片的hpm配置
        HpmConfigVO hpmConfig = new HpmConfigVO();
        hpmConfig.setTeamId(taskInfo.getTeamId());
        hpmConfig.setChipId(taskInfo.getChipId());
        List<HpmConfigVO> hpmConfigs = hpmConfigDao.inquireHpmConfig(hpmConfig);
        HpmConfigVO hpmConfigVO = hpmConfigs.get(0);

        // 遍历每个单板子任务，根据温度动态设置hpm行数据
        boolean isAssembleColumn = false;
        for (Map.Entry<String, List<ChipAutoTestSubtasks>> boardEntry : boardGroup.entrySet()) {
            List<ChipAutoTestSubtasks> hpmSubtasks = boardEntry.getValue();
            if (!isAssembleColumn) {
                // 列信息只需组装一次就行
                isAssembleColumn = assembleHpmColumn(excelExportEntities, temperatureList, hpmSubtasks, sheetDataMap);
            }

            // 组装hpm sheet页行数据
            assembleHpmRow(hpmSubtasks, sheetDataMap, hpmConfigVO);
        }

        return sheetDataMap;
    }

    private void assembleHpmRow(List<ChipAutoTestSubtasks> hpmSubtasks, Map<String, SheetData> sheetDataMap,
                                HpmConfigVO hpmConfigVO) {
        // 获取第一条数据作为基础数据
        ChipAutoTestSubtasks baseSubtasks = hpmSubtasks.get(0);
        Map<String, Object> rowData = new HashMap<>(10);
        // 设置固定行数据
        getHpmRowData(rowData, baseSubtasks);
        // 遍历每个单板子任务，根据温度动态设置hpm行数据
        LinkedHashMap<String, Map<String, Object>> hpmRowDataMap = new LinkedHashMap<>(3);
        LinkedHashMap<String, Map<String, Object>> paSensorRowDataMap = new LinkedHashMap<>(3);
        for (ChipAutoTestSubtasks subtask : hpmSubtasks) {
            setHpmRowData(subtask, rowData, hpmRowDataMap, paSensorRowDataMap);
        }
        // 将map转为list行数据
        List<Map<String, Object>> hpmCollect = hpmRowDataMap.entrySet().stream().map(Map.Entry::getValue)
                .collect(Collectors.toList());
        List<Map<String, Object>> paSensorCollect = paSensorRowDataMap.entrySet().stream().map(Map.Entry::getValue)
                .collect(Collectors.toList());

        // 将行数据添加到sheet页对象
        if (sheetDataMap.get(CommonConstants.POWER_DOMAIN_HPM) instanceof HpmSheetData) {
            HpmSheetData hpmSheetData = (HpmSheetData) sheetDataMap.get(CommonConstants.POWER_DOMAIN_HPM);
            List<Map<String, Object>> rowDataList = hpmSheetData.getRowData();
            if (rowDataList == null) {
                hpmSheetData.setRowData(hpmCollect);
            } else {
                rowDataList.addAll(hpmCollect);
            }
            hpmSheetData.setRemark(hpmConfigVO.getRemark());
        }
        if (sheetDataMap.get(CommonConstants.POWER_DOMAIN_PASENSOR) instanceof HpmSheetData) {
            HpmSheetData paSensorSheetData = (HpmSheetData) sheetDataMap.get(CommonConstants.POWER_DOMAIN_PASENSOR);
            List<Map<String, Object>> rowDataList = paSensorSheetData.getRowData();
            if (rowDataList == null) {
                paSensorSheetData.setRowData(paSensorCollect);
            } else {
                rowDataList.addAll(paSensorCollect);
            }
            paSensorSheetData.setRemark(hpmConfigVO.getRemark());
        }
    }

    private void setHpmRowData(ChipAutoTestSubtasks subtask, Map<String, Object> rowData,
                               LinkedHashMap<String, Map<String, Object>> hpmRow, LinkedHashMap<String, Map<String, Object>> paSensor) {
        Double temperature = subtask.getTemperature();
        // 获取子任务hpm测试结果
        HpmTestResult hpmTestResult = JSON.parseObject(subtask.getTestResultJson(), HpmTestResult.class);
        List<LinkedHashMap<String, String>> hpmResult = hpmTestResult.getHpmResult();
        List<LinkedHashMap<String, String>> paSensorResult = hpmTestResult.getPaSensorResult();
        hpmResult = hpmResult == null ? new ArrayList<>() : hpmResult;
        paSensorResult = paSensorResult == null ? new ArrayList<>() : paSensorResult;
        for (int i = 0; i < hpmResult.size() + paSensorResult.size(); i++) {
            paSensorResult.remove(null);
            hpmResult.remove(null);
        }
        hpmTestResult.setHpmResult(hpmResult);
        hpmTestResult.setPaSensorResult(paSensorResult);
        for (LinkedHashMap<String, String> hpmMap : hpmResult) {
            assembleHpmRowData(rowData, hpmMap, temperature, hpmRow);
        }
        for (LinkedHashMap<String, String> paSensorMap : paSensorResult) {
            assemblePaSensorRowData(rowData, paSensorMap, temperature, paSensor);
        }
    }

    private void getHpmRowData(Map<String, Object> rowData, ChipAutoTestSubtasks baseSubtasks) {
        rowData.put(CommonConstants.TEST_RESULT_COLUM_BOARD_ID, baseSubtasks.getBoardId());
        rowData.put(CommonConstants.TEST_RESULT_COLUM_CHIP_NO, baseSubtasks.getChipNo());
        rowData.put(CommonConstants.TEST_RESULT_COLUM_BOARD_NO, baseSubtasks.getBoardName());
        rowData.put(CommonConstants.TEST_RESULT_COLUM_CORNER, baseSubtasks.getCorner());
        rowData.put(CommonConstants.TEST_RESULT_COLUM_BOARD_TYPE, baseSubtasks.getBoardType());
        rowData.put(CommonConstants.TEST_RESULT_COLUM_PARENT_TASK_ID, baseSubtasks.getParentTaskId());
    }

    private void assemblePaSensorRowData(Map<String, Object> rowData, LinkedHashMap<String, String> paSensorMap,
                                         Double temperature, LinkedHashMap<String, Map<String, Object>> paSensorRowDataMap) {
        Map<String, Object> paSensorRowMap = new HashMap<>(10);
        paSensorRowMap.putAll(rowData);
        String paSensor = paSensorMap.get(CommonConstants.POWER_DOMAIN_PASENSOR);
        for (Map.Entry<String, String> entry : paSensorMap.entrySet()) {
            String colum = entry.getKey();
            if (!CommonConstants.POWER_DOMAIN_PASENSOR.equals(colum) && temperature != null) {
                // vmin测试不是hpm电源域的有温度区分，hpm测试没有温度区分
                colum = getHpmColum(temperature, colum);
            }
            // 设置paSensor行数据
            paSensorRowMap.put(colum, entry.getValue());
        }

        Map<String, Object> paSensorRow = paSensorRowDataMap.get(paSensor);
        if (paSensorRow == null) {
            paSensorRowDataMap.put(paSensor, paSensorRowMap);
        } else {
            paSensorRow.putAll(paSensorRowMap);
        }
    }

    private void assembleHpmRowData(Map<String, Object> rowData, LinkedHashMap<String, String> hpmMap,
                                    Double temperature, LinkedHashMap<String, Map<String, Object>> hpmRowDataMap) {
        Map<String, Object> hpmRowData = new LinkedHashMap<>(10);
        hpmRowData.putAll(rowData);
        String hpm = hpmMap.get(CommonConstants.POWER_DOMAIN_HPM);
        for (Map.Entry<String, String> entry : hpmMap.entrySet()) {
            String colum = entry.getKey();
            if (!CommonConstants.POWER_DOMAIN_HPM.equals(colum) && temperature != null) {
                // vmin测试不是hpm电源域的有温度区分，hpm测试没有温度区分
                colum = getHpmColum(temperature, colum);
            }
            // 设置hpm行数据
            hpmRowData.put(colum, entry.getValue());
        }

        Map<String, Object> hpmRow = hpmRowDataMap.get(hpm);
        if (hpmRow == null) {
            hpmRowDataMap.put(hpm, hpmRowData);
        } else {
            hpmRow.putAll(hpmRowData);
        }
    }

    private boolean assembleHpmColumn(List<ExcelExportEntity> commonColum, List<Double> temperatureList,
                                      List<ChipAutoTestSubtasks> subtasks, Map<String, SheetData> sheetDataMap) {
        // 获取第一条数据，解析hpm测试结果，获取列信息
        ChipAutoTestSubtasks testSubtasks = subtasks.get(0);
        HpmTestResult hpmTestResult = JSON.parseObject(testSubtasks.getTestResultJson(), HpmTestResult.class);

        // hpm列信息
        List<ExcelExportEntity> hpmExportEntities = new ArrayList<>();
        hpmExportEntities.addAll(commonColum);

        // paSensor列信息
        List<ExcelExportEntity> paSensorExportEntities = new ArrayList<>();
        paSensorExportEntities.addAll(commonColum);

        if (CollectionUtil.isEmpty(temperatureList)) {
            // 若是hpm测试，没有温度，设置动态列信息
            setHpmColum(hpmTestResult, hpmExportEntities);

            // 设置paSensor列信息
            setPaSensorColum(hpmTestResult, paSensorExportEntities);
        } else {
            // 设置hpm列信息
            setHpmColum(hpmTestResult, hpmExportEntities, temperatureList);

            // 设置paSensor列信息
            setPaSensorColum(hpmTestResult, paSensorExportEntities, temperatureList);
        }

        // 设置每个sheet页的列信息
        SheetData hpmSheetData = new HpmSheetData();
        hpmSheetData.setExcelExportEntities(hpmExportEntities);
        SheetData paSensorSheetData = new HpmSheetData();
        paSensorSheetData.setExcelExportEntities(paSensorExportEntities);

        // 设置sheet页对象
        sheetDataMap.put(CommonConstants.POWER_DOMAIN_HPM, hpmSheetData);
        sheetDataMap.put(CommonConstants.POWER_DOMAIN_PASENSOR, paSensorSheetData);
        return true;
    }

    private void setPaSensorColum(HpmTestResult hpmTestResult, List<ExcelExportEntity> paSensorExportEntities) {
        List<LinkedHashMap<String, String>> paSensorResult = hpmTestResult.getPaSensorResult();

        // 列顺序
        int orderNum = 4;
        for (Map.Entry<String, String> entry : paSensorResult.get(0).entrySet()) {
            String colum = entry.getKey();
            ExcelExportEntity hpmExcelExportEntity = new ExcelExportEntity();
            hpmExcelExportEntity.setKey(colum);
            hpmExcelExportEntity.setName(colum);
            hpmExcelExportEntity.setOrderNum(++orderNum);
            paSensorExportEntities.add(hpmExcelExportEntity);
        }
    }

    private void setHpmColum(HpmTestResult hpmTestResult, List<ExcelExportEntity> hpmExportEntities) {
        List<LinkedHashMap<String, String>> hpmResult = hpmTestResult.getHpmResult();
        LinkedHashMap<String, String> hpmResultMap = hpmResult.get(0);

        // 列顺序
        int orderNum = 4;
        for (Map.Entry<String, String> entry : hpmResultMap.entrySet()) {
            String colum = entry.getKey();
            ExcelExportEntity hpmExcelExportEntity = new ExcelExportEntity();
            hpmExcelExportEntity.setKey(colum);
            hpmExcelExportEntity.setName(colum);
            hpmExcelExportEntity.setOrderNum(++orderNum);
            hpmExportEntities.add(hpmExcelExportEntity);
        }
    }

    private void setPaSensorColum(HpmTestResult hpmTestResult, List<ExcelExportEntity> paSensorExportEntities,
                                  List<Double> temperatureList) {
        List<LinkedHashMap<String, String>> paSensorResult = hpmTestResult.getPaSensorResult();
        paSensorResult = paSensorResult == null ? new ArrayList<>() : paSensorResult;
        paSensorResult = new ArrayList<>(paSensorResult);
        for (int i = 0; i < paSensorResult.size(); i++) {
            paSensorResult.remove(null);
        }
        hpmTestResult.setPaSensorResult(paSensorResult);
        if (paSensorResult.isEmpty()) {
            return;
        }
        // 列顺序
        int orderNum = 4;
        for (Map.Entry<String, String> entry : paSensorResult.get(0).entrySet()) {
            String colum = entry.getKey();
            if (CommonConstants.POWER_DOMAIN_PASENSOR.equals(colum)) {
                // PASENSOR列无温度
                ExcelExportEntity paSensorExportEntity = new ExcelExportEntity();
                paSensorExportEntity.setKey(colum);
                paSensorExportEntity.setName(colum);
                paSensorExportEntity.setOrderNum(++orderNum);
                paSensorExportEntities.add(paSensorExportEntity);
            } else {
                for (int i = 0; i < temperatureList.size(); i++) {
                    // 非PASENSOR列有温度区分
                    colum = getHpmColum(temperatureList.get(i), entry.getKey());

                    ExcelExportEntity hpmExcelExportEntity = new ExcelExportEntity();
                    hpmExcelExportEntity.setKey(colum);
                    hpmExcelExportEntity.setName(colum);
                    hpmExcelExportEntity.setOrderNum(++orderNum);
                    paSensorExportEntities.add(hpmExcelExportEntity);
                }
            }
        }
    }

    private void setHpmColum(HpmTestResult hpmTestResult, List<ExcelExportEntity> hpmExportEntities,
                             List<Double> temperatureList) {
        List<LinkedHashMap<String, String>> hpmResult = hpmTestResult.getHpmResult();
        hpmResult = hpmResult == null ? new ArrayList<>() : hpmResult;
        hpmResult = new ArrayList<>(hpmResult);
        for (int i = 0; i < hpmResult.size(); i++) {
            hpmResult.remove(null);
        }
        hpmTestResult.setHpmResult(hpmResult);
        if (hpmResult.isEmpty()) {
            return;
        }
        // 列顺序
        int orderNum = 4;
        for (Map.Entry<String, String> entry : hpmResult.get(0).entrySet()) {
            String colum = entry.getKey();
            if (CommonConstants.POWER_DOMAIN_HPM.equals(colum)) {
                // HPM列无温度
                ExcelExportEntity hpmExcelExportEntity = new ExcelExportEntity();
                hpmExcelExportEntity.setKey(colum);
                hpmExcelExportEntity.setName(colum);
                hpmExcelExportEntity.setOrderNum(++orderNum);
                hpmExportEntities.add(hpmExcelExportEntity);
            } else {
                for (int i = 0; i < temperatureList.size(); i++) {
                    // 非HPM列有温度区分
                    colum = getHpmColum(temperatureList.get(i), entry.getKey());

                    ExcelExportEntity hpmExcelExportEntity = new ExcelExportEntity();
                    hpmExcelExportEntity.setKey(colum);
                    hpmExcelExportEntity.setName(colum);
                    hpmExcelExportEntity.setOrderNum(++orderNum);
                    hpmExportEntities.add(hpmExcelExportEntity);
                }
            }
        }
    }

    private String getHpmColum(Double temperature, String colum) {
        return colum + "(" + temperature + ")";
    }

    private List<ExcelExportEntity> getCommonColum() {
        List<ExcelExportEntity> excelExportEntities = new ArrayList<>();
        ExcelExportEntity chipNoExcelExportEntity = ExcelUtil
                .getMergeExportEntity(CommonConstants.TEST_RESULT_COLUM_CHIP_NO);
        chipNoExcelExportEntity.setOrderNum(1);
        excelExportEntities.add(chipNoExcelExportEntity);

        ExcelExportEntity boardNameExcelExportEntity = ExcelUtil
                .getMergeExportEntity(CommonConstants.TEST_RESULT_COLUM_BOARD_NO);
        boardNameExcelExportEntity.setOrderNum(2);
        excelExportEntities.add(boardNameExcelExportEntity);

        ExcelExportEntity cornerExcelExportEntity = ExcelUtil
                .getMergeExportEntity(CommonConstants.TEST_RESULT_COLUM_CORNER);
        cornerExcelExportEntity.setOrderNum(3);
        excelExportEntities.add(cornerExcelExportEntity);

        ExcelExportEntity boardTypeExcelExportEntity = ExcelUtil
                .getMergeExportEntity(CommonConstants.TEST_RESULT_COLUM_BOARD_TYPE);
        boardTypeExcelExportEntity.setOrderNum(4);
        excelExportEntities.add(boardTypeExcelExportEntity);
        return excelExportEntities;
    }

    private Map<String, SheetData> vMinDataAssemble(ChipAutoTestTaskInfo taskInfo, List<ChipAutoTestSubtasks> subtasks,
                                                    boolean isExcel) {
        // 获取hpm测试数据
        Map<String, List<Map<String, String>>> hpmDataMap = getHpmData(subtasks);

        // 解析任务提交参数
        ChipAutoTestTaskInfoVO taskInfoVO = JSONUtil.toBean(taskInfo.getSubmitTaskParam(),
                ChipAutoTestTaskInfoVO.class);

        // 过滤选择框数据中未选择的数据
        AutoTestService.filterSelectedData(taskInfoVO);

        // 获取电源域hpm配置map
        Map<String, PowerDomainHpmVO> powerDomainHpmMap = getPowerDomainHpmMap(taskInfoVO);

        // 子任务按电源域分组
        Map<String, List<ChipAutoTestSubtasks>> powerDomainGroup = subtasks.stream()
                .collect(Collectors.groupingBy(ChipAutoTestSubtasks::getPowerDomainName));

        // sheet页数据
        Map<String, SheetData> sheetData = new HashMap<>();

        // 获取测试人员信息，即创建人
        XinHuoUserInfo testPerson = UserUtil.getUserByUuid(taskInfo.getCreateBy());

        // 遍历电源域分组，每个电源域为一个sheet页，组装成Excel数据
        for (Map.Entry<String, List<ChipAutoTestSubtasks>> entry : powerDomainGroup.entrySet()) {
            String powerDomainName = entry.getKey();
            if (CommonConstants.POWER_DOMAIN_HPM.equals(powerDomainName)) {
                // 若是hpm数据则跳过
                continue;
            }

            // 获取电源域对应的hpm配置
            PowerDomainHpmVO powerDomainHpmVO = powerDomainHpmMap.get(powerDomainName);
            if (powerDomainHpmVO == null) {
                continue;
            }

            // 获取电源域频点信息
            Map<String, List<PowerDomainFrequencyVO>> cornerFrequencyMap = getPowerDomainFrequencyMap(taskInfoVO,
                    powerDomainName);

            // sheet页行数据list
            List<Map<String, Object>> rowDataS = new ArrayList<>();

            // 遍历电源域分组，组装excel列信息，行数据
            List<ChipAutoTestSubtasks> subtasksList = entry.getValue();

            // 获取温度list
            List<Double> temperatureList = subtasksList.stream().map(subtasks1 -> subtasks1.getTemperature()).distinct()
                    .sorted(Comparator.comparing(Double::doubleValue)).collect(Collectors.toList());

            // 组装sheet页列信息
            List<ExcelExportEntity> excelExportEntities = assembleVMinColumn(temperatureList, cornerFrequencyMap,
                    powerDomainHpmVO, powerDomainName);

            // 根据单板分组
            Map<String, List<ChipAutoTestSubtasks>> boardGroup = subtasksList.stream()
                    .collect(Collectors.groupingBy(ChipAutoTestSubtasks::getBoardId));

            for (Map.Entry<String, List<ChipAutoTestSubtasks>> boardEntry : boardGroup.entrySet()) {
                // 行数据map
                Map<String, Object> rowData = new HashMap<>(10);

                // 组装行数据
                assembleRowData(rowData, boardEntry.getValue(), hpmDataMap, powerDomainHpmVO, isExcel);
                rowDataS.add(rowData);
            }

            VMinSheetData vMinSheetData = new VMinSheetData();
            vMinSheetData.setExcelExportEntities(excelExportEntities);
            vMinSheetData.setRowData(rowDataS);
            vMinSheetData.setTestPerson(testPerson.getUserFullNameCn());
            vMinSheetData.setEnvironmentTemperatureList(taskInfoVO.getEnvironmentTemperatureList());
            vMinSheetData.setTestTime(taskInfo.getCreateTime());
            vMinSheetData.setSoftwareVersion(powerDomainHpmVO.getMirrorName());
            sheetData.put(powerDomainName, vMinSheetData);
        }

        return sheetData;
    }

    private void assembleRowData(Map<String, Object> rowData, List<ChipAutoTestSubtasks> subtasksList,
                                 Map<String, List<Map<String, String>>> hpmDataMap, PowerDomainHpmVO powerDomainHpmVO, boolean isExcel) {
        for (ChipAutoTestSubtasks subtask : subtasksList) {
            // 设置固定行数据
            rowData.put(CommonConstants.TEST_RESULT_COLUM_CHIP_NO, subtask.getChipNo());
            rowData.put(CommonConstants.TEST_RESULT_COLUM_BOARD_NO, subtask.getBoardName());
            rowData.put(CommonConstants.TEST_RESULT_COLUM_CORNER, subtask.getCorner());
            rowData.put(CommonConstants.TEST_RESULT_COLUM_BOARD_TYPE, subtask.getBoardType());
            rowData.put(CommonConstants.TEST_RESULT_COLUM_BOARD_ID, subtask.getBoardId());
            rowData.put(CommonConstants.TEST_RESULT_COLUM_PARENT_TASK_ID, subtask.getParentTaskId());

            // 根据温度组装行数据
            Double temperature = subtask.getTemperature();

            // 获取该温度下hpm值
            String hpmValue = getHpmValue(subtask, hpmDataMap, powerDomainHpmVO);
            String hpmColum = getVMinHpmColum(temperature, powerDomainHpmVO);
            rowData.put(hpmColum, hpmValue);

            // vMin列分组名称
            String vMinColumGroupName = getVMinColumGroupName(temperature);

            // 获取该温度下vMin测试结果json
            String testResultJson = subtask.getTestResultJson();
            Map<String, Map> testResultMap = JSONUtil.toBean(testResultJson, Map.class);
            for (Map.Entry<String, Map> vMinEntry : testResultMap.entrySet()) {
                // 电源域频点
                String powerDomainFrequency = vMinEntry.getKey();

                if ("name".equals(powerDomainFrequency)) {
                    // 若不是电源域频点则跳过
                    continue;
                }

                // 获取频点测试值、结温测试值,将测试结果转为map
                Map valueMap = vMinEntry.getValue();

                // 频点测试值
                Object vMinValue;
                if (isExcel) {
                    // 若是excel数据，则只需要vmin测试值
                    vMinValue = valueMap.get(CommonConstants.TEST_RESULT_COLUM_V_MIN);
                } else {
                    // 若是拟合所需数据，则返回所有
                    vMinValue = valueMap;
                }

                // 获取vmin列名
                String vMinColumName = getVMinColumName(powerDomainFrequency, CommonConstants.TEST_RESULT_SUFFIX_VMIN);

                // 设置vMin 频点行数据
                String vMinColumKey = getVMinColumKey(vMinColumGroupName, vMinColumName);
                rowData.put(vMinColumKey, vMinValue);

                // 结温列key=频点key+结温
                String endTemperatureColumName = getVMinColumName(powerDomainFrequency,
                        CommonConstants.TEST_RESULT_COLUM_END_TEMPERATURE);
                String endTemperatureColumKey = getVMinColumKey(vMinColumGroupName, endTemperatureColumName);

                // 结温测试值
                Object endTemperatureValue = valueMap.get("tsonser");

                // 设置结温行数据
                rowData.put(endTemperatureColumKey, endTemperatureValue);

                // 结 KeyWord key=频点key+KeyWord
                String keyWordColumName = getVMinColumName(powerDomainFrequency,
                        CommonConstants.TEST_RESULT_COLUM_KEY_WORD);
                String keyWordColumKey = getVMinColumKey(vMinColumGroupName, keyWordColumName);

                // keyWord值
                Object keyWordValue = valueMap.get("keyWord");

                // 设置keyWord行数据
                rowData.put(keyWordColumKey, keyWordValue);

                // 动态处理其他键值对
                if (isExcel && valueMap != null) {
                    for (Map.Entry<Object, Object> dynamicEntry : valueMap.entrySet()) {
                        String dynamicKey = String.valueOf(dynamicEntry.getKey());
                        Object dynamicValue = dynamicEntry.getValue();
                        
                        // 跳过已经处理的固定字段
                        if ("vmin".equals(dynamicKey) || "tsonser".equals(dynamicKey) || "keyWord".equals(dynamicKey) || "name".equals(dynamicKey)) {
                            continue;
                        }
                        
                        // 构建动态列名和键
                        String dynamicColumName = getVMinColumName(powerDomainFrequency, dynamicKey);
                        String dynamicColumKey = getVMinColumKey(vMinColumGroupName, dynamicColumName);
                        rowData.put(dynamicColumKey, dynamicValue);
                    }
                }
            }
        }
    }

    private String getHpmValue(ChipAutoTestSubtasks subtask, Map<String, List<Map<String, String>>> hpmDataMap,
                               PowerDomainHpmVO powerDomainHpmVO) {
        String hpmKey = getHpmKey(subtask);

        // 获取单板、温度对应的hpm数据
        List<Map<String, String>> hpmData = hpmDataMap.get(hpmKey);

        String hpmConfig1 = powerDomainHpmVO.getConfig1();
        String hpmConfig2 = powerDomainHpmVO.getConfig2();

        String hpmValue = null;

        if (hpmData == null) {
            return hpmValue;
        }

        // 通过hpm配置获取hpm对应的值
        Object hpmValueByConfig = getHpmValueByConfig(hpmConfig1, hpmConfig2, hpmData);
        if (hpmValueByConfig == null) {
            log.info(String.format("获取hpm值失败", ""));
        }
        return hpmValueByConfig.toString();
    }

    /**
     * 通过hpm配置获取hpm对应的值
     *
     * @param hpmConfig1 hpm配置1
     * @param hpmConfig2 hpm配置2
     * @param hpmData hpm map
     * @return java.lang.Object
     * @author aiqi
     * @date 2024/3/14 10:28
     **/
    private Object getHpmValueByConfig(String hpmConfig1, String hpmConfig2, List<Map<String, String>> hpmData) {
        // 遍历hpm数据，通过hpm配置获取hpm测试值
        Object hpmValue = null;
        for (Map<String, String> hpmDatum : hpmData) {
            String hpmConfig = hpmDatum.get(CommonConstants.POWER_DOMAIN_HPM);
            if (hpmConfig.equals(hpmConfig1)) {
                hpmValue = hpmDatum.get(hpmConfig2);
                break;
            }
        }
        return hpmValue;
    }

    private List<ExcelExportEntity> assembleVMinColumn(List<Double> temperatureList,
                                                       Map<String, List<PowerDomainFrequencyVO>> cornerFrequencyMap, PowerDomainHpmVO powerDomainHpmVO,
                                                       String powerDomainName) {
        List<ExcelExportEntity> excelExportEntities = getCommonColum();

        // 根据温度设置hpm动态列
        for (Double temperature : temperatureList) {
            // hpm动态列设置
            String hpmColum = getVMinHpmColum(temperature, powerDomainHpmVO);

            ExcelExportEntity hpmExcelExportEntity = new ExcelExportEntity();
            hpmExcelExportEntity.setKey(hpmColum);
            hpmExcelExportEntity.setName(hpmColum);
            // 设置单元格格式为数字
            hpmExcelExportEntity.setType(BaseTypeConstants.DOUBLE_TYPE);
            hpmExcelExportEntity.setOrderNum(excelExportEntities.size() + 1);
            excelExportEntities.add(hpmExcelExportEntity);

            // vMin列分组名称
            String vMinColumGroupName = getVMinColumGroupName(temperature);

            // vMin动态列设置（包含vmin、tsonser、keyWord和动态列）
            vMinColumConfig(vMinColumGroupName, cornerFrequencyMap, excelExportEntities, powerDomainName);
        }

        return excelExportEntities;
    }

    private String getVMinColumGroupName(Double temperature) {
        return CommonConstants.TEST_RESULT_GROUP_PREFIX_VMIN + temperature + "℃)";
    }

    private void vMinColumConfig(String vMinColumGroupName,
                                 Map<String, List<PowerDomainFrequencyVO>> cornerFrequencyMap, List<ExcelExportEntity> excelExportEntities,
                                 String powerDomainName) {
        int orderNum = excelExportEntities.size();

        // 默认不同corner电源域频点相同
        for (Map.Entry<String, List<PowerDomainFrequencyVO>> entry : cornerFrequencyMap.entrySet()) {
            List<PowerDomainFrequencyVO> frequencyVOS = entry.getValue();

            for (PowerDomainFrequencyVO frequencyVO : frequencyVOS) {
                // 电源域频点
                String powerDomainMhz = frequencyVO.getPowerDomainMhz();

                // 设置频点列
                ExcelExportEntity vMinExcelExportEntity = new ExcelExportEntity();
                vMinExcelExportEntity.setGroupName(vMinColumGroupName);
                // vMin列名
                String vMinColumName = getVMinColumName(powerDomainMhz, CommonConstants.TEST_RESULT_SUFFIX_VMIN);
                // 获取频点列key
                String vMinColumKey = getVMinColumKey(vMinColumGroupName, vMinColumName);
                vMinExcelExportEntity.setKey(vMinColumKey);
                vMinExcelExportEntity.setName(vMinColumName);
                // 设置单元格格式为数字
                vMinExcelExportEntity.setType(BaseTypeConstants.DOUBLE_TYPE);
                vMinExcelExportEntity.setOrderNum(++orderNum);
                // 将列信息添加到list
                excelExportEntities.add(vMinExcelExportEntity);

                // 设置结温列
                ExcelExportEntity endTemperature = new ExcelExportEntity();
                endTemperature.setGroupName(vMinColumGroupName);
                String endTemperatureColumName = getVMinColumName(powerDomainMhz,
                        CommonConstants.TEST_RESULT_COLUM_END_TEMPERATURE);
                // 获取结温列key
                String endTemperatureColumKey = getVMinColumKey(vMinColumGroupName, endTemperatureColumName);
                endTemperature.setKey(endTemperatureColumKey);
                endTemperature.setName(endTemperatureColumName);
                // 设置单元格格式为数字
                endTemperature.setType(BaseTypeConstants.DOUBLE_TYPE);
                endTemperature.setOrderNum(++orderNum);
                excelExportEntities.add(endTemperature);

                // 若电源域不是CPU、GPU、DDR则添加keyWord列
                if (!"CPU".equals(powerDomainName) && !"GPU".equals(powerDomainName)
                        && !"DDR".equals(powerDomainName)) {
                    // 设置KeyWord列
                    ExcelExportEntity keyWord = new ExcelExportEntity();
                    keyWord.setGroupName(vMinColumGroupName);
                    // 获取KeyWord列key
                    String keyWordColumName = getVMinColumName(powerDomainMhz,
                            CommonConstants.TEST_RESULT_COLUM_KEY_WORD);
                    String keyWordColumKey = getVMinColumKey(vMinColumGroupName, keyWordColumName);
                    keyWord.setKey(keyWordColumKey);
                    keyWord.setName(keyWordColumName);
                    keyWord.setOrderNum(++orderNum);
                    excelExportEntities.add(keyWord);
                }

                // 为动态列预留位置，在数据填充时根据实际JSON数据生成
                // 动态列将在assembleRowData中处理
            }

            // 第一次执行完获取列就终止
            break;
        }
    }

    private String getVMinColumKey(String vMinColumGroupName, String vMinColumName) {
        return vMinColumGroupName + "|" + vMinColumName;
    }

    private String getVMinColumName(String powerDomainMhz, String suffix) {
        return powerDomainMhz + "_" + suffix;
    }

    private String getVMinHpmColum(Double temperature, PowerDomainHpmVO powerDomainHpmVO) {
        String hpmConfig1 = powerDomainHpmVO.getConfig1();
        String hpmConfig2 = powerDomainHpmVO.getConfig2();

        // hpm列名
        return hpmConfig1 + "-" + hpmConfig2 + "(" + temperature + ")";
    }

    private Map<String, List<PowerDomainFrequencyVO>> getPowerDomainFrequencyMap(ChipAutoTestTaskInfoVO taskInfoVO,
                                                                                 String powerDomainName) {
        Map<String, List<PowerDomainFrequencyVO>> frequencyMap = taskInfoVO.getFrequencyMap();

        // 以电源域、corner为key的电源域频点配置
        List<PowerDomainFrequencyVO> frequencyVOS = frequencyMap.get(powerDomainName);

        // 对电源域分组的数据再以corner分组
        return frequencyVOS.stream().collect(Collectors.groupingBy(PowerDomainFrequencyVO::getCorner));
    }

    private String getHpmKey(ChipAutoTestSubtasks subtask) {
        return subtask.getBoardId() + "|" + subtask.getTemperature();
    }

    private Map<String, PowerDomainHpmVO> getPowerDomainHpmMap(ChipAutoTestTaskInfoVO taskInfoVO) {
        // 电源域hpm配置map
        Map<String, PowerDomainHpmVO> powerDomainHpmMap = new HashMap<>(3);

        // 遍历电源域hpm配置，获取每个单板温度对应的hpm配置
        for (PowerDomainHpmVO powerDomainHpmVO : taskInfoVO.getPowerDomainList()) {
            String powerDomainName = powerDomainHpmVO.getPowerDomainName();

            // 通过hpm配置2获取对应的hpm值
            powerDomainHpmMap.put(powerDomainName, powerDomainHpmVO);
        }
        return powerDomainHpmMap;
    }

    private Map<String, List<Map<String, String>>> getHpmData(List<ChipAutoTestSubtasks> subtasks) {
        // 获取子任务中的hpm数据
        List<ChipAutoTestSubtasks> hpmSubtasks = subtasks.stream()
                .filter(subtasks1 -> CommonConstants.POWER_DOMAIN_HPM.equals(subtasks1.getPowerDomainName()))
                .collect(Collectors.toList());

        // 单板id+温度作为key
        Map<String, List<Map<String, String>>> hpmDataMap = new HashMap<>();

        // 遍历hpm数据，解析hpm测试结果
        for (ChipAutoTestSubtasks hpmSubtask : hpmSubtasks) {
            // 获取hpm测试结果
            Map hpmResultMap = JSONUtil.toBean(hpmSubtask.getTestResultJson(), Map.class);
            List<Map<String, String>> hpmResult = (List<Map<String, String>>) hpmResultMap
                    .get(CommonConstants.TEST_RESULT_HPM_RESULT);

            // 组装hpm结果map，单板id+温度作为key
            String key = getHpmKey(hpmSubtask);
            hpmDataMap.put(key, hpmResult);
        }

        return hpmDataMap;
    }
}