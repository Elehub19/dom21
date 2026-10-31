package ru.star.recommendation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.star.recommendation.dto.ServiceInfo;
import ru.star.recommendation.repository.TransactionsRepository;

import java.util.Optional;

/**
 * Технические методы управления сервисом.
 */
@RestController
@RequestMapping("/management")
public class ManagementController {

    private static final String UNKNOWN_VERSION = "unknown";

    private final TransactionsRepository transactionsRepository;
    private final Optional<BuildProperties> buildProperties;
    private final String applicationName;

    /**
     * BuildProperties появляется после сборки проекта Maven (цель build-info, она запускается при mvn compile/package).
     * Если сервис запущен без неё, возвращаем имя из настроек и версию «unknown».
     */
    public ManagementController(TransactionsRepository transactionsRepository,
                                Optional<BuildProperties> buildProperties,
                                @Value("${spring.application.name}") String applicationName) {
        this.transactionsRepository = transactionsRepository;
        this.buildProperties = buildProperties;
        this.applicationName = applicationName;
    }

    /**
     * Сбрасывает кеши результатов запросов к базе клиентов.
     */
    @PostMapping("/clear-caches")
    public void clearCaches() {
        transactionsRepository.clearCaches();
    }

    @GetMapping("/info")
    public ServiceInfo info() {
        return buildProperties
                .map(build -> new ServiceInfo(build.getName(), build.getVersion()))
                .orElseGet(() -> new ServiceInfo(applicationName, UNKNOWN_VERSION));
    }
}
