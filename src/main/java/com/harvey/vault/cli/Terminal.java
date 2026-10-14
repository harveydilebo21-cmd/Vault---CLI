package com.harvey.vault.cli;

public interface Terminal {

    /** Reads a visible line. Returns null when the input has ended. */
    String readLine(String prompt);

    /** Reads a secret without echoing it. Returns null when the input has ended. */
    char[] readSecret(String prompt);

    void println(String text);
}