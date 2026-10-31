package ru.star.recommendation.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.star.recommendation.dto.UserInfo;

import java.util.List;
import java.util.UUID;

/**
 * Пользователи банка в базе клиентов (H2, только чтение).
 */
@Repository
public class UsersRepository {

    private static final String FIND_BY_USERNAME_SQL = """
            SELECT id, first_name, last_name
            FROM users
            WHERE username = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public UsersRepository(@Qualifier("recommendationsJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @return всех пользователей с данным username (0, 1 или несколько)
     */
    public List<UserInfo> findByUsername(String username) {
        return jdbcTemplate.query(FIND_BY_USERNAME_SQL,
                (rs, rowNum) -> new UserInfo(
                        rs.getObject("id", UUID.class),
                        rs.getString("first_name"),
                        rs.getString("last_name")),
                username);
    }
}
