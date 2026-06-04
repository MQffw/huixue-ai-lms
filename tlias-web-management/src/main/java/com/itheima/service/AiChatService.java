package com.itheima.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AiChatService {

    SseEmitter streamChat(String message, List<Map<String, String>> history);

    String chat(String message, List<Map<String, String>> history);
}
