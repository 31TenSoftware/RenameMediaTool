package com.thirtyonetensoftware.renamemediatool;

import javafx.animation.AnimationTimer;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageConsumer extends AnimationTimer {

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private final BlockingQueue<String> mMessageQueue = new LinkedBlockingQueue<>();

    private final TextArea mTextArea;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public MessageConsumer(TextArea textArea) {
        mTextArea = textArea;
    }

    // ------------------------------------------------------------------------
    // Super Methods
    // ------------------------------------------------------------------------

    @Override
    public void handle(long now) {
        List<String> messages = new ArrayList<>();

        mMessageQueue.drainTo(messages);

        for (String message : messages) {
            mTextArea.appendText(message);
        }
    }

    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------

    public void add(String text) {
        add(text, true);
    }

    public void add(String text, boolean newLine) {
        try {
            mMessageQueue.put(newLine ? "\n" + text : text);
        } catch (InterruptedException e) {
            mTextArea.appendText("\nInterruptedException while writing: " + text);
        }
    }
}
