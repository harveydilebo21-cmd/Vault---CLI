package com.harvey.vault.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;

public class Vault {

    private final Map<String, Entry> entries = new TreeMap<>();

    private static String key(String site) {
        return site.trim().toLowerCase(Locale.ROOT);
    }

    public void add(Entry entry) {
        String k = key(entry.site());
        if (entries.containsKey(k)) {
            throw new IllegalArgumentException(
                    "Entry already exists for site: " + entry.site());
        }
        entries.put(k, entry);
    }

    public Optional<Entry> get(String site) {
        return Optional.ofNullable(entries.get(key(site)));
    }

    public void update(Entry entry) {
        String k = key(entry.site());
        if (!entries.containsKey(k)) {
            throw new NoSuchElementException("No entry for site: " + entry.site());
        }
        entries.put(k, entry);
    }

    public boolean delete(String site) {
        return entries.remove(key(site)) != null;
    }

    public List<Entry> all() {
        return List.copyOf(entries.values());
    }

    public int size() {
        return entries.size();
    }
}