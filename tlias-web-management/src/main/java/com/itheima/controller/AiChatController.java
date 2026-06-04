package com.itheima.controller;

import com.itheima.pojo.Result;
import com.itheima.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/ai")
@RestController
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody Map<String, Object> requestBody) {
        String message = (String) requestBody.get("message");
        List<Map<String, String>> history = (List<Map<String, String>>) requestBody.get("history");
        log.info("AI 聊天请求: {}", message);
        return aiChatService.streamChat(message, history);
    }

    @PostMapping("/chat/sync")
    public Result chatSync(@RequestBody Map<String, Object> requestBody) {
        String message = (String) requestBody.get("message");
        List<Map<String, String>> history = (List<Map<String, String>>) requestBody.get("history");
        log.info("AI 同步聊天请求: {}", message);
        String response = aiChatService.chat(message, history);
        return Result.success(response);
    }
}
