package com.example.botvk.service;

import com.example.botvk.entities.VkEntities;
import com.example.botvk.entities.VkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
@Slf4j
public class LongPollService {

    @Value("${vk.group.id}")
    private String groupId;

    @Value("${vk.access.token}")
    private String accessToken;

    @Value("${vk.api.version}")
    private String apiVersion;

    private final RestTemplate restTemplate = new RestTemplate();
    private String server;
    private String key;
    private int ts;

    /**
     * Запускает службу Long Poll.
     */
    public void start() {
        initialize();

        poll();
    }

    /**
     * Инициализирует соединение с сервером Long Poll.
     * Получает URL-адрес сервера Long Poll, ключ и начальную метку времени.
     */
    private void initialize() {
        String url = String.format(
                "https://api.vk.com/method/groups.getLongPollServer?group_id=%s&access_token=%s&v=%s",
                groupId, accessToken, apiVersion
        );

        try {
            var response = restTemplate.getForObject(url, VkLongPollServerResponse.class);

            log.info("Инициализация прошла успешно");

            if (response != null && response.getResponse() != null) {
                this.server = response.getResponse().getServer();
                this.key = response.getResponse().getKey();
                this.ts = Integer.parseInt(response.getResponse().getTs());
            }

        } catch (ResourceAccessException e) {
            e.printStackTrace();

            log.error("Ошибка соединение с сервером Long Poll в VK при инициализации");
        }
    }

    /**
     * Постоянно опрашивает сервер Long Poll на наличие новых обновлений.
     * Анализирует каждое событие и обрабатывает сообщения.
     */
    private void poll() {
        while (true) {
            String url = String.format(
                    "%s?act=a_check&key=%s&ts=%d&wait=25",
                    server, key, ts
            );

            try {
                log.info("Проверка наличия обновления началась");

                String jsonResponse = restTemplate.getForObject(url, String.class);

                VkEntities entities = parseEvent(jsonResponse);

                if (entities != null) {
                    this.ts = entities.getTs();
                    entities.getUpdates().forEach(this::handleMessage);
                }

                log.info("Проверка наличия закончилась");
            } catch (ResourceAccessException e) {
                e.printStackTrace();

                log.error("Ошибка соединение с сервером Long Poll в VK при проверке обновления");
            }
        }
    }

    /**
     * Парсит JSON ответ в объект VkEntities.
     *
     * @param jsonResponse JSON ответ из Long Poll server.
     * @return Преобразованный объект VkEntities .
     */
    private VkEntities parseEvent(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(jsonResponse, VkEntities.class);

        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }

    /**
     * Обработчик нового сообщения в сообществе.
     *
     * @param vkMessage Сообщение из сообщества в VK.
     */
    private void handleMessage(VkMessage vkMessage) {
        log.info("Обрабатываем новое сообщение в сообществе");

        if ("message_new".equals(vkMessage.getType())) {
            VkMessage.MessageObject messageObject = vkMessage.getObject();
            VkMessage.MessageObject.Message message = messageObject.getMessage();
            String responseMessage = "Вы сказали: " + message.getText();

            sendMessage(message.getPeerId(), responseMessage);
        }
    }

    /**
     * Отправляет ответное сообщение указанному узлу(peer).
     *
     * @param peerId  идентификатор узла, которому нужно отправить сообщение.
     * @param message текст сообщения для отправки.
     */
    private void sendMessage(int peerId, String message) {
        String url = String.format(
                "https://api.vk.com/method/messages.send?peer_id=%d&message=%s&access_token=%s&v=%s&random_id=%d",
                peerId, message, accessToken, apiVersion, System.currentTimeMillis()
        );

        try {
            restTemplate.getForObject(url, String.class);

            log.info("Отправили ответное сообщение в сообщество");
        } catch (ResourceAccessException e) {
            e.printStackTrace();

            log.error("Ошибка соединение с сервером Long Poll в VK при отправке ответного сообщения");
        }
    }

    /**
     * Представляет собой ответ на запрос инициализации сервера VK Long Poll.
     */
    @Data
    private static class VkLongPollServerResponse {
        private LongPollServer response;

        /**
         * Представляет сведения о сервере Long Poll.
         */
        @Data
        public static class LongPollServer {
            private String key;
            private String server;
            private String ts;
        }
    }
}
