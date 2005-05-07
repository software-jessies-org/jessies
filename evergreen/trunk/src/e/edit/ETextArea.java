package e.edit;

import e.ptextarea.*;

/**
 * A text-editing component.
 */
public class ETextArea extends PTextArea {
    public String reformatPastedText(String pastedText) {
        return pastedText.replace('\u00a0', ' ');
    }
}
