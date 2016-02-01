package e.edit;

import java.awt.event.*;

public interface MinibufferUser {
    /**
     * Returns the prompt the minibuffer should display while your
     * code is in charge. This will only be invoked once, around the
     * time getInitialValue is invoked.
     */
    public String getPrompt();

    /**
     * Returns the proposed initial value of the minibuffer's text.
     */
    public String getInitialValue();

    /**
     * Tests whether the given value is valid. Return true if you
     * don't know that it's invalid, because the minibuffer may
     * render invalid values differently, or prevent the user from
     * accepting an invalid value.
     */
    public boolean isValid(String value);

    /**
     * Invoked when the user has modified the minibuffer text. Note
     * that this isn't necessarily invoked for every keypress: the
     * minibuffer may coalesce changes that occur in quick succession.
     * This is intended to be used for implementing "as-you-type"
     * operations.
     */
    public void valueChangedTo(String value);
    
    /**
     * Invoked if the user tries to invoke an action from the keyboard.
     * Use the result of e.getKeyCode() to determine the action.
     * Return true if the minibuffer should hide itself, false otherwise.
     */
    public boolean interpretSpecialKeystroke(KeyEvent e);

    /**
     * Invoked if the user accepts the value in the minibuffer.
     * Return true to comply, false if you don't want to allow this value to be accepted.
     */
    public boolean wasAccepted(String value);

    /**
     * Invoked if the user cancels the minibuffer. This is useful if
     * you need to restore any state (a canceled search, for example,
     * might want to return the selection to its original location,
     * and/or remove any highlighting it's added).
     */
    public void wasCanceled();
}
