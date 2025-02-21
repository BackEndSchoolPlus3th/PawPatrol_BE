package com.patrol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // ✅ 추가
public class PawpatrolApplication {

    public static void main (String[] args) {
        SpringApplication.run(PawpatrolApplication.class, args);
    }

}
