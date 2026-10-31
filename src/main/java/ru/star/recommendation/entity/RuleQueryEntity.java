package ru.star.recommendation.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

/**
 * Один запрос динамического правила: тип, аргументы и модификатор отрицания.
 */
@Entity
@Table(name = "rule_queries")
public class RuleQueryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false)
    private RuleEntity rule;

    @Column(name = "query_order", nullable = false)
    private int queryOrder;

    @Column(name = "query_type", nullable = false)
    private String queryType;

    @Column(name = "negate", nullable = false)
    private boolean negate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rule_query_arguments", joinColumns = @JoinColumn(name = "query_id"))
    @OrderColumn(name = "arg_order")
    @Column(name = "argument", nullable = false)
    @BatchSize(size = 100)
    private List<String> arguments = new ArrayList<>();

    protected RuleQueryEntity() {
    }

    public RuleQueryEntity(String queryType, boolean negate, List<String> arguments) {
        this.queryType = queryType;
        this.negate = negate;
        this.arguments = new ArrayList<>(arguments);
    }

    void setRule(RuleEntity rule) {
        this.rule = rule;
    }

    void setQueryOrder(int queryOrder) {
        this.queryOrder = queryOrder;
    }

    public Long getId() {
        return id;
    }

    public int getQueryOrder() {
        return queryOrder;
    }

    public String getQueryType() {
        return queryType;
    }

    public boolean isNegate() {
        return negate;
    }

    public List<String> getArguments() {
        return arguments;
    }
}
