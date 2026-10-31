package ru.star.recommendation.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import ru.star.recommendation.dto.ServiceInfo;
import ru.star.recommendation.repository.TransactionsRepository;

import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagementControllerTest {

    @Mock
    private TransactionsRepository repository;

    @Test
    void infoReturnsNameAndVersionFromBuildProperties() {
        Properties properties = new Properties();
        properties.setProperty("name", "star-recommendation");
        properties.setProperty("version", "1.2.3");
        ManagementController controller =
                new ManagementController(repository, Optional.of(new BuildProperties(properties)), "app");

        assertEquals(new ServiceInfo("star-recommendation", "1.2.3"), controller.info());
    }

    @Test
    void infoFallsBackWhenBuildInfoIsMissing() {
        ManagementController controller = new ManagementController(repository, Optional.empty(), "app");

        assertEquals(new ServiceInfo("app", "unknown"), controller.info());
    }

    @Test
    void clearCachesClearsRepositoryCaches() {
        ManagementController controller = new ManagementController(repository, Optional.empty(), "app");

        controller.clearCaches();

        verify(repository).clearCaches();
    }
}
