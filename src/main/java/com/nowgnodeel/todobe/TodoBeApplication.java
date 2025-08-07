package com.nowgnodeel.todobe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class TodoBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoBeApplication.class, args);
    }

}
