package com.thirtyonetensoftware.renamemediatool;

import javafx.animation.AnimationTimer;
import javafx.scene.control.TextArea;

public class MessageConsumer extends AnimationTimer {

    // ------------------------------------------------------------------------
    // Class Variables
    // ------------------------------------------------------------------------

    private static final int CAPACITY = 75000;

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private final TextArea mTextArea;

    private StringBuffer buffer = new StringBuffer(CAPACITY);

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
        mTextArea.appendText(buffer.toString());
        buffer = new StringBuffer(CAPACITY);
    }

    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------

    public void add(String text) {
        add(text, true);
    }

    public void add(String text, boolean newLine) {
        buffer.append(newLine ? "\n" + text : text);
    }
}
