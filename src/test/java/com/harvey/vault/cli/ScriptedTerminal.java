package com.harvey.vault.cli;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A fake terminal: answers come from a script, output is recorded.
 * A script item that is a Runnable is run (instead of answering) the next
 * time the program asks for input, e.g. to simulate time passing.
 */
class ScriptedTerminal implements Terminal {

    private final Deque<Object> script;
    private final StringBuilder output = new StringBuilder();

    ScriptedTerminal(Object... script) {
        this.script = new ArrayDeque<>(List.of(script));
    }

    @Override
    public String readLine(String prompt) {
        output.append(prompt).append('\n'); // the user pressing Enter ends the line
        return nextInput();
    }

    @Override
    public char[] readSecret(String prompt) {
        output.append(prompt).append('\n');
        String next = nextInput();
        return next == null ? null : next.toCharArray();
    }

    @Override
    public void println(String text) {
        output.append(text).append('\n');
    }

    String output() {
        return output.toString();
    }

    private String nextInput() {
        while (script.peek() instanceof Runnable action) {
            script.poll();
            action.run();
        }
        return (String) script.poll(); // null once the script runs out
    }
}