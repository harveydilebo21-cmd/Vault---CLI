package com.harvey.vault.clipboard;

import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class SystemClipboard implements ClipboardAccess {

    private final Clipboard clipboard;

    public SystemClipboard() {
        Clipboard found = null;
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                found = Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (HeadlessException | SecurityException e) {
                // no clipboard on this machine
            }
        }
        this.clipboard = found;
    }

    @Override
    public boolean isAvailable() {
        return clipboard != null;
    }

    @Override
    public void write(String text) {
        clipboard.setContents(new StringSelection(text), null);
    }

    @Override
    public String read() {
        try {
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return (String) clipboard.getData(DataFlavor.stringFlavor);
            }
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            // another program owns the clipboard right now, or it holds non-text
        }
        return null;
    }
}