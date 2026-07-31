package com.chat.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 数学运算验证码服务
 * 存储在内存中，60 秒过期，一次性消费
 */
@Service
public class CaptchaService {

    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    /** 验证码有效期：2 分钟 */
    private static final long TTL_MILLIS = 120_000;

    /**
     * 生成数学验证码
     * @return { captchaKey, captchaQuestion }
     */
    public CaptchaVO generate() {
        int a = ThreadLocalRandom.current().nextInt(1, 20);
        int b = ThreadLocalRandom.current().nextInt(1, 20);
        int answer = a + b;

        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        store.put(key, new CaptchaEntry(answer, System.currentTimeMillis()));

        return new CaptchaVO(key, a + " + " + b + " = ?");
    }

    /**
     * 校验验证码（一次性消费，用完即删）
     */
    public boolean verify(String key, int answer) {
        CaptchaEntry entry = store.remove(key);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() - entry.createdAt > TTL_MILLIS) {
            return false;
        }
        return entry.answer == answer;
    }

    // ---------- 内部类 ----------

    private static class CaptchaEntry {
        final int answer;
        final long createdAt;

        CaptchaEntry(int answer, long createdAt) {
            this.answer = answer;
            this.createdAt = createdAt;
        }
    }

    public static class CaptchaVO {
        private final String captchaKey;
        private final String captchaQuestion;

        public CaptchaVO(String captchaKey, String captchaQuestion) {
            this.captchaKey = captchaKey;
            this.captchaQuestion = captchaQuestion;
        }

        public String getCaptchaKey() { return captchaKey; }
        public String getCaptchaQuestion() { return captchaQuestion; }
    }
}
