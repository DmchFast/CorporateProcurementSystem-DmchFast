package com.team.corporate.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan({"com.team.corporate.core.entities", "com.team.corporate.backend.entities"})
public final class BackendMain {
    static void main(String[] args) {
        SpringApplication.run(BackendMain.class, args);
    }
}
