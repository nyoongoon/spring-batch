package com.example.demo;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;

import java.util.Date;

public class DailyJobTimestamper implements JobParametersIncrementer {
    @Override
    public JobParameters getNext(JobParameters parameters){

        return new JobParametersBuilder(parameters) // 빌더 사용!
                .addDate("currentDate", new Date())
                .toJobParameters();
    }
}
