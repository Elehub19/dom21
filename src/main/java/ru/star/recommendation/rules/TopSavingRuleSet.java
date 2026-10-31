package ru.star.recommendation.rules;

import org.springframework.stereotype.Component;
import ru.star.recommendation.dto.RecommendationDto;
import ru.star.recommendation.repository.TransactionsRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class TopSavingRuleSet implements RecommendationRuleSet {

    static final UUID ID = UUID.fromString("59efc529-2fff-41af-baff-90ccd7402925");
    static final String NAME = "Top Saving";
    static final String TEXT = "Откройте свою собственную «Копилку» с нашим банком! "
            + "«Копилка» — это уникальный банковский инструмент, который поможет вам легко и удобно накапливать деньги на важные цели. "
            + "Больше никаких забытых чеков и потерянных квитанций — всё под контролем!\n\n"
            + "Преимущества «Копилки»:\n\n"
            + "Накопление средств на конкретные цели. Установите лимит и срок накопления, "
            + "и банк будет автоматически переводить определенную сумму на ваш счет.\n\n"
            + "Прозрачность и контроль. Отслеживайте свои доходы и расходы, контролируйте процесс накопления "
            + "и корректируйте стратегию при необходимости.\n\n"
            + "Безопасность и надежность. Ваши средства находятся под защитой банка, "
            + "а доступ к ним возможен только через мобильное приложение или интернет-банкинг.\n\n"
            + "Начните использовать «Копилку» уже сегодня и станьте ближе к своим финансовым целям!";

    private static final BigDecimal THRESHOLD = BigDecimal.valueOf(50_000);

    private final TransactionsRepository repository;

    public TopSavingRuleSet(TransactionsRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RecommendationDto> check(UUID userId) {
        if (!repository.usesProductType(userId, Types.DEBIT)) {
            return Optional.empty();
        }
        BigDecimal debitDeposit = repository.sumAmount(userId, Types.DEBIT, Types.DEPOSIT);
        BigDecimal savingDeposit = repository.sumAmount(userId, Types.SAVING, Types.DEPOSIT);
        boolean enoughDeposits = debitDeposit.compareTo(THRESHOLD) >= 0
                || savingDeposit.compareTo(THRESHOLD) >= 0;
        if (!enoughDeposits) {
            return Optional.empty();
        }
        BigDecimal debitWithdraw = repository.sumAmount(userId, Types.DEBIT, Types.WITHDRAW);
        if (debitDeposit.compareTo(debitWithdraw) > 0) {
            return Optional.of(new RecommendationDto(NAME, ID, TEXT));
        }
        return Optional.empty();
    }
}
