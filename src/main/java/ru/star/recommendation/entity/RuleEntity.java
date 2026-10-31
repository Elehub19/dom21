package ru.star.recommendation.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Динамическое правило: рекомендуемый продукт и набор запросов (объединяются по «И»).
 */
@Entity
@Table(name = "rules")
public class RuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_text", nullable = false)
    private String productText;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("queryOrder ASC")
    @BatchSize(size = 100)
    private List<RuleQueryEntity> queries = new ArrayList<>();

    protected RuleEntity() {
    }

    public RuleEntity(String productName, UUID productId, String productText) {
        this.productName = productName;
        this.productId = productId;
        this.productText = productText;
    }

    public void addQuery(RuleQueryEntity query) {
        query.setRule(this);
        query.setQueryOrder(queries.size());
        queries.add(query);
    }

    public Long getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getProductText() {
        return productText;
    }

    public List<RuleQueryEntity> getQueries() {
        return queries;
    }
}
