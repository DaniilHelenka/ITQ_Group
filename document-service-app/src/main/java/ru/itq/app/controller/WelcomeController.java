package ru.itq.app.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Корневой URL — сервис только REST API, статической главной страницы нет.
 * GET / возвращает краткую справку по API.
 */
@RestController
public class WelcomeController {

    @GetMapping(value = {"/", "/api"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> welcome() {
        return ResponseEntity.ok(Map.of(
                "service", "Document Service (ITQ Group)",
                "description", "REST API для работы с документами: создание, смена статусов, история, реестр утверждений.",
                "apiBase", "/api/documents",
                "endpoints", Map.of(
                        "POST /api/documents", "Создать документ (DRAFT)",
                        "GET /api/documents/{id}", "Получить документ с историей",
                        "GET /api/documents?ids=...", "Пакетное получение по ID",
                        "POST /api/documents/submit", "Отправить на согласование (DRAFT → SUBMITTED)",
                        "POST /api/documents/approve", "Утвердить (SUBMITTED → APPROVED)",
                        "GET /api/documents/search", "Поиск по статусу, автору, периоду",
                        "POST /api/documents/concurrent-approve-test", "Тест конкурентного утверждения"
                )
        ));
    }
}
