package ru.star.recommendation.bot;

import org.springframework.stereotype.Service;
import ru.star.recommendation.dto.RecommendationDto;
import ru.star.recommendation.dto.UserInfo;
import ru.star.recommendation.repository.UsersRepository;
import ru.star.recommendation.service.RecommendationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Логика Telegram-бота без привязки к Telegram: получает текст сообщения, возвращает тексты ответов.
 */
@Service
public class BotService {

    static final String USER_NOT_FOUND = "Пользователь не найден";

    static final String GREETING_AND_HELP = """
            Здравствуйте! Я бот банка «Стар»: подберу для вас новые банковские продукты.

            Справка:
            /recommend username — показать рекомендации для пользователя с именем username
            Например: /recommend ivan.petrov""";

    static final String USAGE = "Укажите имя пользователя одним словом: /recommend username";

    static final String UNKNOWN_COMMAND = "Не понимаю это сообщение. Доступна команда:\n/recommend username";

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private static final Pattern START_OR_HELP = Pattern.compile("^/(start|help)(@\\w+)?$");
    private static final Pattern RECOMMEND = Pattern.compile("^/recommend(@\\w+)?(\\s+(.*))?$", Pattern.DOTALL);

    private final UsersRepository usersRepository;
    private final RecommendationService recommendationService;

    /** Чаты, которым уже показывали приветствие и справку. */
    private final Set<Long> greetedChats = ConcurrentHashMap.newKeySet();

    public BotService(UsersRepository usersRepository, RecommendationService recommendationService) {
        this.usersRepository = usersRepository;
        this.recommendationService = recommendationService;
    }

    /**
     * @param chatId идентификатор чата
     * @param text   текст сообщения пользователя
     * @return сообщения, которые нужно отправить в ответ, по порядку
     */
    public List<String> handle(long chatId, String text) {
        String message = text == null ? "" : text.trim();
        boolean firstContact = greetedChats.add(chatId);

        if (START_OR_HELP.matcher(message).matches()) {
            return List.of(GREETING_AND_HELP);
        }

        List<String> replies = new ArrayList<>();
        if (firstContact) {
            replies.add(GREETING_AND_HELP);
        }

        Matcher recommend = RECOMMEND.matcher(message);
        if (recommend.matches()) {
            String argument = recommend.group(3) == null ? "" : recommend.group(3).trim();
            if (argument.isEmpty() || argument.matches(".*\\s.*")) {
                replies.add(USAGE);
            } else {
                replies.addAll(recommendationsFor(argument));
            }
        } else if (!firstContact) {
            replies.add(UNKNOWN_COMMAND);
        }
        return replies;
    }

    private List<String> recommendationsFor(String username) {
        List<UserInfo> users = usersRepository.findByUsername(username);
        if (users.size() != 1) {
            return List.of(USER_NOT_FOUND);
        }
        UserInfo user = users.get(0);
        List<RecommendationDto> recommendations = recommendationService.getRecommendations(user.id());
        return format(user, recommendations);
    }

    private List<String> format(UserInfo user, List<RecommendationDto> recommendations) {
        String greeting = "Здравствуйте " + user.fullName();
        if (recommendations.isEmpty()) {
            return List.of(greeting + "\n\nДля вас пока нет новых продуктов.");
        }

        // Telegram ограничивает длину сообщения, поэтому длинную выдачу делим по продуктам
        List<String> messages = new ArrayList<>();
        StringBuilder current = new StringBuilder(greeting).append("\n\nНовые продукты для вас:");
        for (RecommendationDto recommendation : recommendations) {
            String block = "\n\n• " + recommendation.name() + "\n" + recommendation.text();
            if (current.length() + block.length() > MAX_MESSAGE_LENGTH && current.length() > 0) {
                messages.add(current.toString());
                current = new StringBuilder();
                block = block.stripLeading();
            }
            current.append(block);
        }
        messages.add(current.toString());
        return messages;
    }
}
