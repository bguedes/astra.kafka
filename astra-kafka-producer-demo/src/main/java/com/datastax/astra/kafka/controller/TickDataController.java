package com.datastax.astra.kafka.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.datastax.astra.kafka.pojo.TickData;

@RestController
public class TickDataController {

    @Autowired
    KafkaTemplate<String,TickData> kafkaTemplate;
    
	@Value("${kafka.topic.name}")
	private String topicName;    

    @PostMapping("/publish")
    public String publishMessage(@RequestBody TickData tick)
    {
        kafkaTemplate.send(topicName, tick);
        return "Published Successfully!";
    }    
}
