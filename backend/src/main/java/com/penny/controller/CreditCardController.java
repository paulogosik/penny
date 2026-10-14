package com.penny.controller;

import com.penny.dto.CreditCardRequest;
import com.penny.dto.CreditCardResponse;
import com.penny.dto.CreditCardStatementResponse;
import com.penny.model.User;
import com.penny.service.CreditCardService;
import com.penny.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/credit-cards")
public class CreditCardController {

    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }

    @GetMapping
    public List<CreditCardResponse> list(
        @CurrentUser User user,
        @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return creditCardService.list(user, includeArchived);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditCardResponse create(@CurrentUser User user, @Valid @RequestBody CreditCardRequest request) {
        return creditCardService.create(user, request);
    }

    @PutMapping("/{id}")
    public CreditCardResponse update(@CurrentUser User user, @PathVariable UUID id, @Valid @RequestBody CreditCardRequest request) {
        return creditCardService.update(user, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser User user, @PathVariable UUID id) {
        creditCardService.delete(user, id);
    }

    @GetMapping("/{id}/statements/{month}")
    public CreditCardStatementResponse statement(
        @CurrentUser User user,
        @PathVariable UUID id,
        @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth month
    ) {
        return creditCardService.statement(user, id, month);
    }
}
