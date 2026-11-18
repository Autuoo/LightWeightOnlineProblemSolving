package com.example.oj.service;

import com.example.oj.ai.SecretCipher;
import com.example.oj.ai.llm.LlmCredentials;
import com.example.oj.common.BusinessException;
import com.example.oj.common.ErrorCode;
import com.example.oj.config.AiProperties;
import com.example.oj.dto.AiConfigRequest;
import com.example.oj.dto.AiConfigView;
import com.example.oj.entity.UserAiConfig;
import com.example.oj.mapper.UserAiConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class UserAiConfigService {

    private final UserAiConfigMapper configMapper;
    private final SecretCipher secretCipher;
    private final AiProperties aiProperties;

    public UserAiConfigService(UserAiConfigMapper configMapper,
                               SecretCipher secretCipher,
                               AiProperties aiProperties) {
        this.configMapper = configMapper;
        this.secretCipher = secretCipher;
        this.aiProperties = aiProperties;
    }

    public AiConfigView view(Long userId) {
        UserAiConfig config = configMapper.selectById(userId);
        if (config == null) {
            return new AiConfigView(false, "", aiProperties.getModel(), false, "", null);
        }
        String apiKey = secretCipher.decrypt(config.getApiKeyEnc());
        return new AiConfigView(
                config.getEnabled() != null && config.getEnabled() == 1,
                nullToEmpty(config.getBaseUrl()),
                nullToEmpty(config.getModel()).isBlank() ? aiProperties.getModel() : config.getModel(),
                !apiKey.isBlank(),
                mask(apiKey),
                config.getUpdatedAt());
    }

    public AiConfigView save(Long userId, AiConfigRequest request) {
        String baseUrl = nullToEmpty(request.baseUrl()).trim();
        if (!baseUrl.isEmpty() && !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "端点地址必须以 http:// 或 https:// 开头");
        }

        UserAiConfig config = configMapper.selectById(userId);
        boolean create = config == null;
        if (create) {
            config = new UserAiConfig();
            config.setUserId(userId);
        }
        config.setEnabled(request.enabled() == null || request.enabled() ? 1 : 0);
        config.setBaseUrl(baseUrl);
        String model = nullToEmpty(request.model()).trim();
        config.setModel(model.isEmpty() ? aiProperties.getModel() : model);
        if (request.apiKey() != null) {
            config.setApiKeyEnc(secretCipher.encrypt(request.apiKey().trim()));
        } else if (create) {
            config.setApiKeyEnc("");
        }

        if (create) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        return view(userId);
    }

    public LlmCredentials resolve(Long userId) {
        UserAiConfig config = configMapper.selectById(userId);
        if (config == null || config.getEnabled() == null || config.getEnabled() == 0) {
            return null;
        }
        String apiKey = secretCipher.decrypt(config.getApiKeyEnc());
        if (apiKey.isBlank() || nullToEmpty(config.getBaseUrl()).isBlank()) {
            return null;
        }
        String model = nullToEmpty(config.getModel()).isBlank()
                ? aiProperties.getModel() : config.getModel();
        return new LlmCredentials(config.getBaseUrl(), apiKey, model);
    }

    public void clear(Long userId) {
        configMapper.deleteById(userId);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String mask(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        int prefix = Math.min(6, apiKey.length() - 4);
        return apiKey.substring(0, prefix) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}