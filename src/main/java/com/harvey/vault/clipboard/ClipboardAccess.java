package com.harvey.vault.clipboard;

public interface ClipboardAccess {

    /** False on machines with no clipboard (no display, headless). */
    boolean isAvailable();

    void write(String text);

    /** The text currently on the clipboard, or null if it is empty or not text. */
    String read();
}