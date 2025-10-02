package com.project.shopapp.services.chatbot;

import com.project.shopapp.configurations.GeminiConfig;
import com.project.shopapp.dtos.ChatRequest;
import com.project.shopapp.dtos.ChatResponse;
import com.project.shopapp.services.ChatBotService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatBotServiceImpl implements ChatBotService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;
    private final HttpSession httpSession;


    // ✅ Mô tả hệ thống (system instruction)
    private static final String DEFAULT_SYSTEM_INSTRUCTION = """
            Bạn là một trợ lý AI chuyên giúp người dùng chọn sản phẩm phù hợp dựa trên các thuộc tính của sản phẩm.
            Hãy đọc mô tả nhu cầu của người dùng và đưa ra 2-3 gợi ý xe phù hợp từ kho xe:
            
            Trả lời ngắn gọn, dễ hiểu, trực tiếp đề xuất tên xe phù hợp nhất.
            """;

    @Override
    public ChatResponse analyzeDescription(ChatRequest request) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                + geminiConfig.getGeminiApiKey();

        // Lấy hội thoại từ session (mỗi người dùng sẽ có session riêng)
        @SuppressWarnings("unchecked")
        List<String> conversation = (List<String>) httpSession.getAttribute("conversation");
        if (conversation == null) {
            conversation = new ArrayList<>();
            httpSession.setAttribute("conversation", conversation);
        }

        // Gộp ngữ cảnh
        String context = conversation.stream().collect(Collectors.joining("\n"));
        String promptText = context + "\nNgười dùng: " + request.getRequest();

        JSONObject prompt = new JSONObject();
        prompt.put("contents", new JSONObject[]{
                new JSONObject().put("parts", new JSONObject[]{
                        new JSONObject().put("text", promptText)
                })
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(prompt.toString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        JSONObject responseJson = new JSONObject(response.getBody());
        String content = responseJson
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        // Lưu lại hội thoại trong session người dùng
        conversation.add("Người dùng: " + request.getRequest());
        conversation.add("Trợ lý: " + content);

        return new ChatResponse(content, LocalDateTime.now());
    }
}
