package com.com.rename.ipd.hhh.chipautotest.model.autotest;

import java.util.List;

@lombok.NoArgsConstructor
@lombok.Data
public class ChipAutoTestTaskInfoVO {

    @com.fasterxml.jackson.annotation.JsonProperty("projectName")
    private String projectName;
    @com.fasterxml.jackson.annotation.JsonProperty("versionName")
    private String versionName;
    @com.fasterxml.jackson.annotation.JsonProperty("teamName")
    private String teamName;
    @com.fasterxml.jackson.annotation.JsonProperty("chipName")
    private String chipName;
    @com.fasterxml.jackson.annotation.JsonProperty("hpmMirrorId")
    private String hpmMirrorId;
    @com.fasterxml.jackson.annotation.JsonProperty("hpmMirrorXml")
    private String hpmMirrorXml;
    @com.fasterxml.jackson.annotation.JsonProperty("hpmTestSystem")
    private String hpmTestSystem;
    @com.fasterxml.jackson.annotation.JsonProperty("hasDataFit")
    private Boolean hasDataFit;
    @com.fasterxml.jackson.annotation.JsonProperty("environmentTemperatureList")
    private List<Integer> environmentTemperatureList;
    @com.fasterxml.jackson.annotation.JsonProperty("powerDomainList")
    private List<PowerDomainListDTO> powerDomainList;
    @com.fasterxml.jackson.annotation.JsonProperty("environmentId")
    private String environmentId;
    @com.fasterxml.jackson.annotation.JsonProperty("boardInfoList")
    private List<BoardInfoListDTO> boardInfoList;
    @com.fasterxml.jackson.annotation.JsonProperty("instrumentIdList")
    private List<String> instrumentIdList;
    @com.fasterxml.jackson.annotation.JsonProperty("frequencyMap")
    private FrequencyMapDTO frequencyMap;
    @com.fasterxml.jackson.annotation.JsonProperty("projectId")
    private String projectId;
    @com.fasterxml.jackson.annotation.JsonProperty("versionId")
    private String versionId;
    @com.fasterxml.jackson.annotation.JsonProperty("teamId")
    private String teamId;
    @com.fasterxml.jackson.annotation.JsonProperty("runConfigId")
    private Integer runConfigId;
    @com.fasterxml.jackson.annotation.JsonProperty("testType")
    private String testType;
    @com.fasterxml.jackson.annotation.JsonProperty("chipId")
    private Integer chipId;
    @com.fasterxml.jackson.annotation.JsonProperty("taskDescription")
    private String taskDescription;
    @com.fasterxml.jackson.annotation.JsonProperty("pageNum")
    private Integer pageNum;
    @com.fasterxml.jackson.annotation.JsonProperty("pageSize")
    private Integer pageSize;

    @lombok.NoArgsConstructor
    @lombok.Data
    public static class FrequencyMapDTO {
        @com.fasterxml.jackson.annotation.JsonProperty("CPU_A")
        private List<CPUADTO> cpuA;
        @com.fasterxml.jackson.annotation.JsonProperty("CPU_B")
        private List<CPUBDTO> cpuB;

        @lombok.NoArgsConstructor
        @lombok.Data
        public static class CPUADTO {
            @com.fasterxml.jackson.annotation.JsonProperty("powerDomainName")
            private String powerDomainName;
            @com.fasterxml.jackson.annotation.JsonProperty("hasSelected")
            private Boolean hasSelected;
            @com.fasterxml.jackson.annotation.JsonProperty("id")
            private Integer id;
            @com.fasterxml.jackson.annotation.JsonProperty("powerDomainId")
            private Integer powerDomainId;
            @com.fasterxml.jackson.annotation.JsonProperty("corner")
            private String corner;
            @com.fasterxml.jackson.annotation.JsonProperty("powerDomainMhz")
            private String powerDomainMhz;
            @com.fasterxml.jackson.annotation.JsonProperty("testEnabling")
            private Integer testEnabling;
            @com.fasterxml.jackson.annotation.JsonProperty("deflectionStartVoltage")
            private Double deflectionStartVoltage;
            @com.fasterxml.jackson.annotation.JsonProperty("deflectionEndVoltage")
            private Double deflectionEndVoltage;
            @com.fasterxml.jackson.annotation.JsonProperty("testWaitTime")
            private Integer testWaitTime;
            @com.fasterxml.jackson.annotation.JsonProperty("testWaitResultTimeout")
            private Integer testWaitResultTimeout;
            @com.fasterxml.jackson.annotation.JsonProperty("deflectionVoltageMargin")
            private Double deflectionVoltageMargin;
            @com.fasterxml.jackson.annotation.JsonProperty("createBy")
            private String createBy;
            @com.fasterxml.jackson.annotation.JsonProperty("createTime")
            private Long createTime;
            @com.fasterxml.jackson.annotation.JsonProperty("pageNum")
            private Integer pageNum;
            @com.fasterxml.jackson.annotation.JsonProperty("pageSize")
            private Integer pageSize;
        }

        @lombok.NoArgsConstructor
        @lombok.Data
        public static class CPUBDTO {
            @com.fasterxml.jackson.annotation.JsonProperty("powerDomainName")
            private String powerDomainName;
            @com.fasterxml.jackson.annotation.JsonProperty("hasSelected")
            private Boolean hasSelected;
            @com.fasterxml.jackson.annotation.JsonProperty("id")
            private Integer id;
            @com.fasterxml.jackson.annotation.JsonProperty("powerDomainId")
            private Integer powerDomainId;
            @com.fasterxml.jackson.annotation.JsonProperty("corner")
            private String corner;
            @com.fasterxml.jackson.annotation.JsonProperty("powerDomainMhz")
            private String powerDomainMhz;
            @com.fasterxml.jackson.annotation.JsonProperty("testEnabling")
            private Integer testEnabling;
            @com.fasterxml.jackson.annotation.JsonProperty("deflectionStartVoltage")
            private Double deflectionStartVoltage;
            @com.fasterxml.jackson.annotation.JsonProperty("deflectionEndVoltage")
            private Double deflectionEndVoltage;
            @com.fasterxml.jackson.annotation.JsonProperty("testWaitTime")
            private Integer testWaitTime;
            @com.fasterxml.jackson.annotation.JsonProperty("testWaitResultTimeout")
            private Integer testWaitResultTimeout;
            @com.fasterxml.jackson.annotation.JsonProperty("deflectionVoltageMargin")
            private Double deflectionVoltageMargin;
            @com.fasterxml.jackson.annotation.JsonProperty("createBy")
            private String createBy;
            @com.fasterxml.jackson.annotation.JsonProperty("createTime")
            private Long createTime;
            @com.fasterxml.jackson.annotation.JsonProperty("pageNum")
            private Integer pageNum;
            @com.fasterxml.jackson.annotation.JsonProperty("pageSize")
            private Integer pageSize;
        }
    }

    @lombok.NoArgsConstructor
    @lombok.Data
    public static class PowerDomainListDTO {
        @com.fasterxml.jackson.annotation.JsonProperty("powerDomainId")
        private Integer powerDomainId;
        @com.fasterxml.jackson.annotation.JsonProperty("powerDomainName")
        private String powerDomainName;
        @com.fasterxml.jackson.annotation.JsonProperty("mirrorId")
        private String mirrorId;
        @com.fasterxml.jackson.annotation.JsonProperty("mirrorName")
        private String mirrorName;
        @com.fasterxml.jackson.annotation.JsonProperty("mirrorXml")
        private String mirrorXml;
        @com.fasterxml.jackson.annotation.JsonProperty("testSystem")
        private String testSystem;
        @com.fasterxml.jackson.annotation.JsonProperty("config1")
        private String config1;
        @com.fasterxml.jackson.annotation.JsonProperty("config2")
        private String config2;
        @com.fasterxml.jackson.annotation.JsonProperty("allowance")
        private List<?> allowance;
        @com.fasterxml.jackson.annotation.JsonProperty("hasSelected")
        private Boolean hasSelected;
    }

    @lombok.NoArgsConstructor
    @lombok.Data
    public static class BoardInfoListDTO {
        @com.fasterxml.jackson.annotation.JsonProperty("boardId")
        private String boardId;
        @com.fasterxml.jackson.annotation.JsonProperty("boardName")
        private String boardName;
        @com.fasterxml.jackson.annotation.JsonProperty("boardType")
        private String boardType;
        @com.fasterxml.jackson.annotation.JsonProperty("corner")
        private String corner;
        @com.fasterxml.jackson.annotation.JsonProperty("chipNo")
        private String chipNo;
        @com.fasterxml.jackson.annotation.JsonProperty("role")
        private String role;
    }
}
