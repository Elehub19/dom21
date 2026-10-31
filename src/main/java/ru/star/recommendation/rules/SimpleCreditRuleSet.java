package ru.star.recommendation.rules;

import org.springframework.stereotype.Component;
import ru.star.recommendation.dto.RecommendationDto;
import ru.star.recommendation.repository.TransactionsRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class SimpleCreditRuleSet implements RecommendationRuleSet {

    static final UUID ID = UUID.fromString("ab138afb-f3ba-4a93-b74f-0fcee86d447f");
    static final String NAME = "Простой кредит";
    static final String TEXT = "Откройте мир выгодных кредитов с нами!\n\n"
            + "Ищете способ быстро и без лишних хлопот получить нужную сумму? "
            + "Тогда наш выгодный кредит — именно то, что вам нужно! "
            + "Мы предлагаем низкие процентные ставки, гибкие условия и индивидуальный подход к каждому клиенту.\n\n"
            + "Почему выбирают нас:\n\n"
            + "Быстрое рассмотрение заявки. Мы ценим ваше время, поэтому процесс рассмотрения заявки занимает всего несколько часов.\n\n"
            + "Удобное оформление. Подать заявку на кредит можно онлайн на нашем сайте или в мобильном приложении.\n\n"
            + "Широкий выбор кредитных продуктов. Мы предлагаем кредиты на различные цели: "
            + "покупку недвижимости, автомобиля, образование, лечение и многое другое.\n\n"
            + "Не упустите возможность воспользоваться выгодными условиями кредитования от нашей компании!";

    private static final BigDecimal WITHDRAW_THRESHOLD = BigDecimal.valueOf(100_000);

    private final TransactionsRepository repository;

    public SimpleCreditRuleSet(TransactionsRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RecommendationDto> check(UUID userId) {
        if (repository.usesProductType(userId, Types.CREDIT)) {
            return Optional.empty();
        }
        BigDecimal debitDeposit = repository.sumAmount(userId, Types.DEBIT, Types.DEPOSIT);
        BigDecimal debitWithdraw = repository.sumAmount(userId, Types.DEBIT, Types.WITHDRAW);
        if (debitDeposit.compareTo(debitWithdraw) > 0
                && debitWithdraw.compareTo(WITHDRAW_THRESHOLD) > 0) {
            return Optional.of(new RecommendationDto(NAME, ID, TEXT));
        }
        return Optional.empty();
    }
}
