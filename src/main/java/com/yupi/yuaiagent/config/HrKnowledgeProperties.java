package com.yupi.yuaiagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hr.knowledge")
public class HrKnowledgeProperties {

    /**
     * 是否自动初始化知识库
     */
    private boolean autoInit = true;

    /**
     * 是否启动时强制重建
     */
    private boolean rebuildOnStartup = false;

    /**
     * 默认召回条数
     */
    private int topK = 6;

    /**
     * 相似度阈值
     */
    private double similarityThreshold = 0.65;
}