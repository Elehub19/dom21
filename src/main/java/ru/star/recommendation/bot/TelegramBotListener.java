package ru.star.recommendation.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Принимает сообщения из Telegram (long polling) и отправляет ответы {@link BotService}.
 */
@Component
@ConditionalOnProperty(prefix = "telegram.bot", name = "token")
public class TelegramBotListener implements UpdatesListener {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotListener.class);

    private final TelegramBot bot;
    private final BotService botService;

    public TelegramBotListener(TelegramBot bot, BotService botService) {
        this.bot = bot;
        this.botService = botService;
    }

    @PostConstruct
    public void start() {
        bot.setUpdatesListener(this);
        log.info("Telegram-бот запущен");
    }

    @PreDestroy
    public void stop() {
        bot.removeGetUpdatesListener();
        bot.shutdown();
    }

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            try {
                handle(update);
            } catch (Exception e) {
                // ошибка в одном сообщении не должна останавливать бота
                log.error("Не удалось обработать сообщение из Telegram", e);
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private void handle(Update update) {
        Message message = update.message();
        if (message == null || message.text() == null) {
            return;
        }
        long chatId = message.chat().id();
        for (String reply : botService.handle(chatId, message.text())) {
            bot.execute(new SendMessage(chatId, reply));
        }
    }
}
