package ru.star.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.star.recommendation.entity.RuleStatEntity;

public interface RuleStatRepository extends JpaRepository<RuleStatEntity, Long> {

    /**
     * Атомарно увеличивает счётчик на 1 одним UPDATE: без чтения и записи значения в приложении,
     * поэтому параллельные запросы не теряют срабатывания.
     *
     * @return количество обновлённых строк (0, если у правила нет счётчика)
     */
    @Modifying
    @Query(value = "UPDATE rule_stats SET hit_count = hit_count + 1 WHERE rule_id = :ruleId", nativeQuery = true)
    int incrementCount(@Param("ruleId") Long ruleId);
}
