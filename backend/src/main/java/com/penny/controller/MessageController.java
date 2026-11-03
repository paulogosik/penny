package com.penny.controller;

import com.penny.dto.MessageResponse;
import com.penny.dto.TextMessageRequest;
import com.penny.model.User;
import com.penny.service.MessageService;
import com.penny.web.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/text")
    public ResponseEntity<MessageResponse> handleText(@CurrentUser User user, @Valid @RequestBody TextMessageRequest request) {
        MessageResponse response = messageService.handleText(user, request.text());
        return ResponseEntity.status(response.created() ? HttpStatus.CREATED : HttpStatus.OK).body(response);
    }
}
