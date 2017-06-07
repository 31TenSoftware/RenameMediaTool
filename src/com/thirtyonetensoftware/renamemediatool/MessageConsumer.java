package com.thirtyonetensoftware.renamemediatool;

import javafx.animation.AnimationTimer;
import javafx.scene.control.TextArea;

public class MessageConsumer extends AnimationTimer {

    // ------------------------------------------------------------------------
    // Class Variables
    // ------------------------------------------------------------------------

    private static final int CAPACITY = 150000;

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
    public synchronized void handle(long now) {
        mTextArea.appendText(buffer.toString());
        buffer = new StringBuffer(CAPACITY);
    }

    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------

    public synchronized void add(String text) {
        buffer.append(text);
    }
}
