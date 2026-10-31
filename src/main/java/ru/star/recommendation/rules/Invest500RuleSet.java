package ru.star.recommendation.rules;

import org.springframework.stereotype.Component;
import ru.star.recommendation.dto.RecommendationDto;
import ru.star.recommendation.repository.TransactionsRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class Invest500RuleSet implements RecommendationRuleSet {

    static final UUID ID = UUID.fromString("147f6a0f-3b91-413b-ab99-87f081d60d5a");
    static final String NAME = "Invest 500";
    static final String TEXT = "Откройте свой путь к успеху с индивидуальным инвестиционным счетом (ИИС) от нашего банка! "
            + "Воспользуйтесь налоговыми льготами и начните инвестировать с умом. "
            + "Пополните счет до конца года и получите выгоду в виде вычета на взнос в следующем налоговом периоде. "
            + "Не упустите возможность разнообразить свой портфель, снизить риски и следить за актуальными рыночными тенденциями. "
            + "Откройте ИИС сегодня и станьте ближе к финансовой независимости!";

    private static final BigDecimal SAVING_DEPOSIT_THRESHOLD = BigDecimal.valueOf(1000);

    private final TransactionsRepository repository;

    public Invest500RuleSet(TransactionsRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RecommendationDto> check(UUID userId) {
        boolean hasDebit = repository.usesProductType(userId, Types.DEBIT);
        if (!hasDebit) {
            return Optional.empty();
        }
        boolean hasInvest = repository.usesProductType(userId, Types.INVEST);
        if (hasInvest) {
            return Optional.empty();
        }
        BigDecimal sumSaving = repository.sumAmount(userId, Types.SAVING, Types.DEPOSIT);
        if (sumSaving.compareTo(SAVING_DEPOSIT_THRESHOLD) > 0) {
            return Optional.of(new RecommendationDto(NAME, ID, TEXT));
        }
        return Optional.empty();
    }
}
