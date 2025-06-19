package com.sanghun.cartoon_generator.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class RandomNumberService {

    public int generateRandomNumber() {
        return new Random().nextInt(100) + 1; // 1 to 100
    }
} 