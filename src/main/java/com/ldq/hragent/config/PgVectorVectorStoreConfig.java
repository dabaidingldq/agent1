package com.ldq.hragent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PgVectorVectorStoreConfig {

    @Bean("pgVectorVectorStore")
    public VectorStore pgVectorVectorStore(
            EmbeddingModel embeddingModel,
            @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate
    ) {
        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
                .dimensions(1536)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .vectorTableName("hr_knowledge_vector")
                .build();
    }
}