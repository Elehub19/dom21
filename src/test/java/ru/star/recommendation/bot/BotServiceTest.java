package ru.star.recommendation.bot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.star.recommendation.dto.RecommendationDto;
import ru.star.recommendation.dto.UserInfo;
import ru.star.recommendation.repository.UsersRepository;
import ru.star.recommendation.service.RecommendationService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotServiceTest {

    /** Чат, которому приветствие уже показано. */
    private static final long KNOWN_CHAT = 1L;
    /** Чат, который пишет боту впервые. */
    private static final long NEW_CHAT = 2L;

    private final UUID userId = UUID.randomUUID();
    private final UserInfo user = new UserInfo(userId, "Иван", "Петров");

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private RecommendationService recommendationService;

    private BotService botService;

    @BeforeEach
    void setUp() {
        botService = new BotService(usersRepository, recommendationService);
        botService.handle(KNOWN_CHAT, "/start");
    }

    @Test
    void startShowsGreetingAndHelp() {
        List<String> replies = botService.handle(KNOWN_CHAT, "/start");

        assertEquals(1, replies.size());
        assertTrue(replies.get(0).contains("Здравствуйте"));
        assertTrue(replies.get(0).contains("/recommend username"));
    }

    @Test
    void firstMessageOfNewChatGetsGreetingAndHelp() {
        List<String> replies = botService.handle(NEW_CHAT, "привет");

        assertEquals(List.of(BotService.GREETING_AND_HELP), replies);
    }

    @Test
    void greetingIsShownOnlyOnFirstContact() {
        botService.handle(NEW_CHAT, "привет");

        assertEquals(List.of(BotService.UNKNOWN_COMMAND), botService.handle(NEW_CHAT, "привет"));
    }

    @Test
    void recommendReturnsGreetingWithNameAndProductList() {
        UUID invest = UUID.randomUUID();
        UUID saving = UUID.randomUUID();
        when(usersRepository.findByUsername("ivan")).thenReturn(List.of(user));
        when(recommendationService.getRecommendations(userId)).thenReturn(List.of(
                new RecommendationDto("Invest 500", invest, "Текст про инвестиции"),
                new RecommendationDto("Top Saving", saving, "Текст про копилку")));

        List<String> replies = botService.handle(KNOWN_CHAT, "/recommend ivan");

        assertEquals(1, replies.size());
        String reply = replies.get(0);
        assertTrue(reply.startsWith("Здравствуйте Иван Петров"));
        assertTrue(reply.contains("Новые продукты для вас:"));
        assertTrue(reply.contains("• Invest 500\nТекст про инвестиции"));
        assertTrue(reply.contains("• Top Saving\nТекст про копилку"));
    }

    @Test
    void recommendWithBotNameInCommandWorks() {
        when(usersRepository.findByUsername("ivan")).thenReturn(List.of(user));
        when(recommendationService.getRecommendations(userId)).thenReturn(List.of());

        List<String> replies = botService.handle(KNOWN_CHAT, "/recommend@StarBot ivan");

        assertEquals(1, replies.size());
        assertTrue(replies.get(0).startsWith("Здравствуйте Иван Петров"));
    }

    @Test
    void userWithoutRecommendationsStillGetsGreeting() {
        when(usersRepository.findByUsername("ivan")).thenReturn(List.of(user));
        when(recommendationService.getRecommendations(userId)).thenReturn(List.of());

        List<String> replies = botService.handle(KNOWN_CHAT, "/recommend ivan");

        assertEquals(1, replies.size());
        assertTrue(replies.get(0).startsWith("Здравствуйте Иван Петров"));
    }

    @Test
    void unknownUserGivesNotFoundMessage() {
        when(usersRepository.findByUsername("nobody")).thenReturn(List.of());

        assertEquals(List.of("Пользователь не найден"), botService.handle(KNOWN_CHAT, "/recommend nobody"));
        verifyNoInteractions(recommendationService);
    }

    @Test
    void severalUsersGiveNotFoundMessage() {
        when(usersRepository.findByUsername("ivan"))
                .thenReturn(List.of(user, new UserInfo(UUID.randomUUID(), "Иван", "Сидоров")));

        assertEquals(List.of("Пользователь не найден"), botService.handle(KNOWN_CHAT, "/recommend ivan"));
        verifyNoInteractions(recommendationService);
    }

    @Test
    void recommendWithoutUsernameShowsUsage() {
        assertEquals(List.of(BotService.USAGE), botService.handle(KNOWN_CHAT, "/recommend"));
        assertEquals(List.of(BotService.USAGE), botService.handle(KNOWN_CHAT, "/recommend ivan petrov"));
        verifyNoInteractions(usersRepository);
    }

    @Test
    void firstContactWithRecommendCommandGetsGreetingAndAnswer() {
        when(usersRepository.findByUsername("nobody")).thenReturn(List.of());

        List<String> replies = botService.handle(NEW_CHAT, "/recommend nobody");

        assertEquals(List.of(BotService.GREETING_AND_HELP, "Пользователь не найден"), replies);
    }

    @Test
    void longRecommendationListIsSplitIntoSeveralMessages() {
        String longText = "я".repeat(3000);
        when(usersRepository.findByUsername("ivan")).thenReturn(List.of(user));
        when(recommendationService.getRecommendations(userId)).thenReturn(List.of(
                new RecommendationDto("A", UUID.randomUUID(), longText),
                new RecommendationDto("B", UUID.randomUUID(), longText)));

        List<String> replies = botService.handle(KNOWN_CHAT, "/recommend ivan");

        assertEquals(2, replies.size());
        assertTrue(replies.stream().allMatch(reply -> reply.length() <= 4096));
        assertTrue(replies.get(0).startsWith("Здравствуйте Иван Петров"));
        assertTrue(replies.get(1).startsWith("• B"));
    }
}
