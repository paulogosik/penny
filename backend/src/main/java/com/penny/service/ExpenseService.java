package com.penny.service;

import com.penny.dto.ExpenseResponse;
import org.springframework.stereotype.Service;

@Service
public class ExpenseService {

    private final GeminiService geminiService;

    public ExpenseService(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    public ExpenseResponse classifyText(String text) {
        ExpenseResponse classified = geminiService.classify(text);
        return classified;
    }
}
