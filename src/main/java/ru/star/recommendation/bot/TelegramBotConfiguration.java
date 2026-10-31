package ru.star.recommendation.bot;

import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Бот включается, только если задан токен (свойство telegram.bot.token
 * или переменная окружения TELEGRAM_BOT_TOKEN). Без токена сервис работает как раньше, без бота.
 */
@Configuration
@ConditionalOnProperty(prefix = "telegram.bot", name = "token")
public class TelegramBotConfiguration {

    @Bean
    public TelegramBot telegramBot(@Value("${telegram.bot.token}") String token) {
        return new TelegramBot(token);
    }
}
