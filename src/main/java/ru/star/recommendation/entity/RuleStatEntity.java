package ru.star.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Счётчик срабатываний динамического правила. Одна запись на правило;
 * при удалении правила запись удаляется каскадно на уровне БД (ON DELETE CASCADE).
 * Увеличивается атомарным SQL-обновлением, см. RuleStatRepository.
 */
@Entity
@Table(name = "rule_stats")
public class RuleStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false, unique = true)
    private RuleEntity rule;

    /** Тот же столбец rule_id — только для чтения, чтобы не загружать правило ради его id. */
    @Column(name = "rule_id", insertable = false, updatable = false)
    private Long ruleId;

    @Column(name = "hit_count", nullable = false)
    private long count;

    protected RuleStatEntity() {
    }

    public RuleStatEntity(RuleEntity rule) {
        this.rule = rule;
        this.count = 0;
    }

    public Long getId() {
        return id;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public long getCount() {
        return count;
    }
}
