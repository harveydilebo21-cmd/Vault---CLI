package com.harvey.vault.model;

import java.util.Objects;

public record Entry(String site, String username, String password) {

    public Entry {
        Objects.requireNonNull(site, "site");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        if (site.isBlank()) {
            throw new IllegalArgumentException("Site must not be blank");
        }
        site = site.trim();
    }

    @Override
    public String toString() {
        return "Entry[site=" + site + ", username=" + username + ", password=********]";
    }
}