package com.yupi.yuaiagent.rag;

import com.yupi.yuaiagent.config.HrKnowledgeProperties;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HrRagAdvisorFactory {

    private final VectorStore pgVectorVectorStore;
    private final HrKnowledgeProperties hrKnowledgeProperties;

    public HrRagAdvisorFactory(
            @Qualifier("pgVectorVectorStore") VectorStore pgVectorVectorStore,
            HrKnowledgeProperties hrKnowledgeProperties
    ) {
        this.pgVectorVectorStore = pgVectorVectorStore;
        this.hrKnowledgeProperties = hrKnowledgeProperties;
    }

    public QuestionAnswerAdvisor createDefaultAdvisor() {
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(hrKnowledgeProperties.getTopK())
                .similarityThreshold(hrKnowledgeProperties.getSimilarityThreshold())
                .build();

        return QuestionAnswerAdvisor.builder(pgVectorVectorStore)
                .searchRequest(searchRequest)
                .build();
    }

    public QuestionAnswerAdvisor createPolicyAdvisor() {
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(hrKnowledgeProperties.getTopK())
                .similarityThreshold(hrKnowledgeProperties.getSimilarityThreshold())
                .filterExpression("docType == 'policy'")
                .build();

        return QuestionAnswerAdvisor.builder(pgVectorVectorStore)
                .searchRequest(searchRequest)
                .build();
    }

    public QuestionAnswerAdvisor createProcessAdvisor() {
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(hrKnowledgeProperties.getTopK())
                .similarityThreshold(hrKnowledgeProperties.getSimilarityThreshold())
                .filterExpression("docType == 'process'")
                .build();

        return QuestionAnswerAdvisor.builder(pgVectorVectorStore)
                .searchRequest(searchRequest)
                .build();
    }
}