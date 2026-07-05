package com.chessdna.chessdnaanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChessDnaAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChessDnaAnalyzerApplication.class, args);
    }

}
