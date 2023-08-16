package com.example.demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@EnableBatchProcessing //배치 잡 수행에 필요한 인프라스트럭처 제공
@SpringBootApplication
public class DemoApplication {
    // 잡 빌더
    @Autowired
    private JobBuilderFactory jobBuilderFactory;

    // 스텝 빌더
    @Autowired
    private StepBuilderFactory stepBuilderFactory;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // 잡 빈 정의->jobBuilderFactory->jobBuilder->잡구성
    @Bean
    public Job job() {
        return this.jobBuilderFactory.get("basicJob")
                .start(step1())
                .build();
    }

    //스텝 빈 정의 -> stepBuilderFactory->stepBuilder -> 스텝 정의
    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
                .tasklet(((contribution, chunkContext) -> {
                    System.out.println("Hello, World");
                    return RepeatStatus.FINISHED;
                })).build();
    }

}
