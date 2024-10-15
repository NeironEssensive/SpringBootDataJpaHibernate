package org.example.springbootdatajpa.controller;

import org.example.springbootdatajpa.dao.EmployeeRepository;
import org.example.springbootdatajpa.kafka.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    @Autowired
    private final KafkaProducer kafkaProducer;
    private final EmployeeRepository employeeRepository;

    public Controller(KafkaProducer kafkaProducer, EmployeeRepository employeeRepository) {
        this.kafkaProducer = kafkaProducer;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping("/kafka/send")
    private String send(@RequestParam int id) {
        var employee = employeeRepository.findById(id).orElseThrow();
        kafkaProducer.sendMessage(employee.toString());
        return "Success!";
    }
}
