package com.harvey.vault.cli;

import java.io.Console;
import java.util.Scanner;

public class ConsoleTerminal implements Terminal {

    private final Console console = System.console();
    private final Scanner scanner;

    public ConsoleTerminal() {
        if (console == null) {
            scanner = new Scanner(System.in);
            System.out.println("WARNING: no secure console found. "
                    + "Anything you type, including passwords, will be visible.");
        } else {
            scanner = null;
        }
    }

    @Override
    public String readLine(String prompt) {
        if (console != null) {
            return console.readLine("%s", prompt);
        }
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    @Override
    public char[] readSecret(String prompt) {
        if (console != null) {
            return console.readPassword("%s", prompt);
        }
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine().toCharArray() : null;
    }

    @Override
    public void println(String text) {
        System.out.println(text);
    }
}