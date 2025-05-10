package org.ipan.nrgyrent.telegram.mapdb;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class DbMapConfig {

    @Value("${app.mapdb.db-path}")
    String dbPath = "cache/nrgyrent.db";

    @Bean
    public DB db() {
        File file = new File(dbPath);
        file.getParentFile().mkdirs(); // create folders if they do not exist
        return DBMaker.fileDB(file)
                .fileMmapEnable()
                .fileMmapPreclearDisable()
                .closeOnJvmShutdown()
                .make();

        // alternetive: use WAL(Write Ahead Log) Slower but is more durable.
//        DB db = DBMaker
//                .fileDB(file)
//                .transactionEnable()
//                .make();

    }
}
