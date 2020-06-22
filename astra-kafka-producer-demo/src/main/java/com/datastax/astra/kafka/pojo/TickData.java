package com.datastax.astra.kafka.pojo;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Data;

@Data
public class TickData {

    private static final LocalDateTime BASE_TIME = LocalDateTime.now();
    private static final AtomicLong TIME_OFFSET = new AtomicLong();

    private String name;
    private String symbol;
    private double value;
    private String exchange;
    private String industry;

    public String getDatetime() {
    	return BASE_TIME.plus(TIME_OFFSET.incrementAndGet(), ChronoUnit.SECONDS).toString();
    }
}
