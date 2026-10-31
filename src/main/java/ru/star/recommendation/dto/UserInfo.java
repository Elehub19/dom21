package ru.star.recommendation.dto;

import java.util.UUID;

/**
 * Пользователь банка (из базы клиентов).
 */
public record UserInfo(UUID id, String firstName, String lastName) {

    public String fullName() {
        return firstName + " " + lastName;
    }
}
