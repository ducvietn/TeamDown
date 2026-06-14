package com.teamup.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

/**
 * MongoDB configuration for TeamUp.
 *
 * GridFS is used to store large binary files (PDF submissions) that exceed
 * the 16 MB BSON document limit of regular MongoDB documents.
 *
 * This coexists alongside the existing PostgreSQL database — each handles
 * different data:
 *   PostgreSQL  → Tasks, Users, Groups, Peer Reviews, Notifications
 *   MongoDB     → Submission files (PDFs via GridFS)
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:teamup}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public MongoClient mongoClient() {
        return new com.mongodb.client.MongoClients.Builder(mongoUri).build();
    }

    /**
     * GridFsTemplate — the primary interface for GridFS file operations.
     * Used by GridFSService to store and retrieve PDF submissions.
     */
    @Bean
    public GridFsTemplate gridFsTemplate(
            MongoDatabaseFactory mongoDbFactory,
            MappingMongoConverter converter) {
        return new GridFsTemplate(mongoDbFactory, converter);
    }

    /**
     * GridFSBucket — a lower-level API for streaming large files efficiently.
     * Used for large PDF downloads where buffered streaming is preferred.
     */
    @Bean
    public GridFSBucket gridFSBucket(MongoClient mongoClient) {
        return GridFSBuckets.create(mongoClient.getDatabase(databaseName));
    }
}
