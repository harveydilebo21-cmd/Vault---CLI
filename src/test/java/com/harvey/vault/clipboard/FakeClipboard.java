package com.harvey.vault.clipboard;

/** An in-memory clipboard for tests. */
public class FakeClipboard implements ClipboardAccess {

    public String content;          // null means empty
    public boolean available = true;

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void write(String text) {
        content = text;
    }

    @Override
    public String read() {
        return content;
    }
}