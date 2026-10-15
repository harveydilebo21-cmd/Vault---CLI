package com.harvey.vault.cli;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** A fake terminal: answers come from a script, output is recorded. */
class ScriptedTerminal implements Terminal {

    private final Deque<String> inputs;
    private final StringBuilder output = new StringBuilder();

    ScriptedTerminal(String... inputs) {
        this.inputs = new ArrayDeque<>(List.of(inputs));
    }


    @Override
    public String readLine(String prompt) {
        output.append(prompt).append('\n');
        return inputs.poll();
    }

    @Override
    public char[] readSecret(String prompt) {
        output.append(prompt).append('\n');
        String next = inputs.poll();
        return next == null ? null : next.toCharArray();
    }



    @Override
    public void println(String text) {
        output.append(text).append('\n');
    }

    String output() {
        return output.toString();
    }
}