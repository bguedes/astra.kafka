package com.datastax.astra.kafka.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.datastax.astra.kafka.pojo.TickData;

@RestController
public class TickDataController {

    @Autowired
    KafkaTemplate<String,TickData> kafkaTemplate;
    
    private static final String TOPIC = "json-stream";

    @PostMapping("/publish")
    public String publishMessage(@RequestBody TickData tick)
    {
        kafkaTemplate.send(TOPIC, tick);
        return "Published Successfully!";
    }    
}
