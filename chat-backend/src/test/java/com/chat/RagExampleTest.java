package com.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * RAG（Retrieval-Augmented Generation，检索增强生成）示例测试类
 *
 * 演示完整的 RAG 流程：
 * 1. 知识库：将领域知识切分为若干文档片段（这里用内存 List 模拟，实际可存 MySQL/向量库）
 * 2. 检索（Retrieval）：基于字符 bigram 的 Jaccard 相似度，从知识库召回与问题最相关的 Top-K 片段。
 *    纯本地计算，中文场景无需分词即可工作，也无需引入向量数据库。
 * 3. 生成（Generation）：将召回的片段作为上下文注入 system prompt，调用大模型 API（SiliconFlow）
 *    生成只依赖知识库内容的回答，缓解模型幻觉。
 *
 * 运行方式：
 * - 检索测试为纯本地逻辑，直接运行即可：mvn test -Dtest=RagExampleTest
 * - 端到端生成测试需要 API Key：优先读环境变量 DEEPSEEK_API_KEY，
 *   否则自动读取 application.yml 中的 deepseek.api-key；找不到则自动跳过该测试。
 */
class RagExampleTest {

    /** 模拟的知识库文档片段（可按此模式扩展为数据库表 + Embedding 向量检索） */
    private static final List<String> KNOWLEDGE_BASE = List.of(
            "注册登录：用户通过用户名或手机号注册，密码使用 BCrypt 加密存储。登录成功后返回 JWT 令牌，"
                    + "后续所有接口请求均需携带该令牌进行身份认证。",
            "好友系统：支持通过用户名/手机号搜索用户并发送好友请求，对方同意后成为好友。"
                    + "好友请求支持同意、拒绝，好友的上下线状态会通过 WebSocket 实时推送。",
            "私聊：好友之间可以发送文字、图片、语音消息。消息通过 WebSocket 实时推送，"
                    + "支持消息撤回、已读回执、输入中状态提示。",
            "群聊：用户可以创建群聊、邀请好友加入、退出群聊。群消息实时推送给群内所有成员，"
                    + "支持查看群成员列表和群聊历史记录。",
            "AI 助手：聊天框内可召唤 AI 助手对话，基于 SiliconFlow 平台的大模型 API，支持连续多轮对话。",
            "消息通知：新消息到达时，会话列表展示未读红点和最新消息预览，未读消息数量实时累加。",
            "朋友圈：用户可以发布图文动态，好友之间可以点赞、评论，支持查看好友的动态流。",
            "数据存储：所有数据存储在 MySQL 中，消息保留 7 天后由定时任务自动清理，"
                    + "图片文件存储在服务器 uploads 目录。"
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ==================== 一、检索（Retrieval） ====================

    /** 将文本转为字符 bigram 集合（中文无需分词，bigram 即可捕获语义单元） */
    private static Set<String> toBigrams(String text) {
        Set<String> grams = new HashSet<>();
        String clean = text.replaceAll("\\s+", "");
        if (clean.length() < 2) {
            if (!clean.isEmpty()) {
                grams.add(clean);
            }
            return grams;
        }
        for (int i = 0; i < clean.length() - 1; i++) {
            grams.add(clean.substring(i, i + 2));
        }
        return grams;
    }

    /** 计算两个文本的 Jaccard 相似度：|交集| / |并集|，取值 0~1 */
    private static double jaccard(String a, String b) {
        Set<String> ga = toBigrams(a);
        Set<String> gb = toBigrams(b);
        if (ga.isEmpty() || gb.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(ga);
        intersection.retainAll(gb);
        Set<String> union = new HashSet<>(ga);
        union.addAll(gb);
        return (double) intersection.size() / union.size();
    }

    /** 检索：返回与问题最相似的前 topK 个文档片段（降序） */
    private static List<String> retrieve(String question, int topK) {
        return KNOWLEDGE_BASE.stream()
                .sorted(Comparator.comparingDouble((String doc) -> jaccard(question, doc)).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    // ==================== 二、生成（Generation） ====================

    /** 将召回片段组装成 system prompt，约束模型只依据资料回答 */
    private static String buildRagPrompt(List<String> contexts) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个基于知识库回答问题的助手。请只根据下面提供的【参考资料】回答用户问题；\n")
                .append("如果参考资料中没有相关信息，请明确回答\"资料中没有相关信息\"，不要编造。\n")
                .append("请使用简洁清晰的中文回答。\n\n")
                .append("【参考资料】\n");
        for (int i = 0; i < contexts.size(); i++) {
            sb.append(i + 1).append(". ").append(contexts.get(i)).append('\n');
        }
        return sb.toString();
    }

    /** 调用 SiliconFlow 大模型 API（与 AiChatController 相同的调用方式） */
    private static String callLlm(String apiKey, String systemPrompt, String question) throws Exception {
        String apiUrl = System.getenv().getOrDefault("DEEPSEEK_API_URL",
                "https://api.siliconflow.cn/v1/chat/completions");
        String model = System.getenv().getOrDefault("DEEPSEEK_MODEL", "Qwen/Qwen2.5-7B-Instruct");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", question));

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);
        body.put("temperature", 0.3); // RAG 场景用较低温度，减少编造
        body.put("max_tokens", 1024);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = new RestTemplate()
                .exchange(apiUrl, HttpMethod.POST, entity, String.class);

        JsonNode node = OBJECT_MAPPER.readTree(response.getBody());
        return node.path("choices").path(0).path("message").path("content").asText();
    }

    /** 解析 API Key：优先环境变量，其次回退到 application.yml 的 deepseek.api-key */
    private static String resolveApiKey() {
        String key = System.getenv("DEEPSEEK_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        try (InputStream in = RagExampleTest.class.getResourceAsStream("/application.yml")) {
            if (in != null) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("deepseek:\\s*\\R\\s*api-key:\\s*(\\S+)").matcher(content);
                if (m.find()) {
                    return m.group(1);
                }
            }
        } catch (Exception ignored) {
            // 忽略解析失败，交由 assumeTrue 跳过端到端测试
        }
        return null;
    }

    // ==================== 三、测试用例 ====================

    @Test
    @DisplayName("RAG 检索：能从知识库召回与问题相关的文档片段")
    void retrieval_召回相关文档() {
        List<String> hits = retrieve("怎么添加好友？", 2);
        System.out.println("【问题】怎么添加好友？");
        System.out.println("【Top-2 召回】" + hits);

        assertFalse(hits.isEmpty());
        boolean hasFriendDoc = hits.stream().anyMatch(doc -> doc.contains("好友"));
        assertTrue(hasFriendDoc, "召回结果应包含\"好友系统\"相关文档，实际: " + hits);
    }

    @Test
    @DisplayName("RAG 检索：不同主题的问题召回不同的文档")
    void retrieval_不同问题召回不同文档() {
        String friendHit = retrieve("如何发送好友申请？", 1).get(0);
        String groupHit = retrieve("怎么创建一个群聊？", 1).get(0);

        System.out.println("【好友问题召回】" + friendHit);
        System.out.println("【群聊问题召回】" + groupHit);

        assertTrue(friendHit.contains("好友"), "好友问题应召回好友文档，实际: " + friendHit);
        assertTrue(groupHit.contains("群聊"), "群聊问题应召回群聊文档，实际: " + groupHit);
    }

    @Test
    @DisplayName("RAG 端到端：检索增强 → 调用大模型生成基于知识库的回答")
    void rag_端到端生成() throws Exception {
        String apiKey = resolveApiKey();
        assumeTrue(apiKey != null && !apiKey.isBlank(),
                "未找到 API Key（环境变量 DEEPSEEK_API_KEY 或 application.yml 的 deepseek.api-key），跳过端到端测试");

        String question = "这个系统怎么添加好友？对方需要同意吗？";
        List<String> contexts = retrieve(question, 2);
        String answer = callLlm(apiKey, buildRagPrompt(contexts), question);

        assertNotNull(answer, "AI 返回内容为空");
        assertFalse(answer.isBlank(), "AI 返回内容为空");

        System.out.println("【问题】" + question);
        System.out.println("【检索到的上下文】");
        for (int i = 0; i < contexts.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + contexts.get(i));
        }
        System.out.println("【AI 回答】" + answer);
    }
}
