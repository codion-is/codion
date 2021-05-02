/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.TextInputPanel;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.text.Format;

import static is.codion.common.Util.nullOrEmpty;

/**
 * Utility class for text based {@link ComponentValue} instances.
 */
public final class TextValues {

  private TextValues() {}

  /**
   * @param textComponent the component
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponentValue(final C textComponent) {
    return new AbstractTextComponentValue<String, C>(textComponent, null, UpdateOn.KEYSTROKE) {
      @Override
      protected String getComponentValue(final C component) {
        final String text = component.getText();

        return nullOrEmpty(text) ? null : text;
      }
      @Override
      protected void setComponentValue(final C component, final String value) {
        component.setText(value);
      }
    };
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponentValue(final C textComponent, final Format format) {
    return textComponentValue(textComponent, format, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param updateOn specifies when the underlying value should be updated
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textComponentValue(final C textComponent, final Format format,
                                                                                        final UpdateOn updateOn) {
    return new FormattedTextComponentValue<>(textComponent, format, updateOn);
  }

  /**
   * @param textField the component
   * @return a Value bound to the given component
   */
  public static ComponentValue<Character, JTextField> characterTextFieldValue(final JTextField textField) {
    return characterTextFieldValue(textField, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textField the component
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<Character, JTextField> characterTextFieldValue(final JTextField textField, final UpdateOn updateOn) {
    return new CharacterFieldValue(textField, updateOn);
  }

  /**
   * Instantiates a new String based ComponentValue.
   * @param inputDialogTitle the title to use for the lookup input dialog
   * @param initialValue the initial value
   * @param maximumLength the maximum input length, -1 for no limit
   * @return a String based ComponentValue
   */
  public static ComponentValue<String, TextInputPanel> textInputPanelValue(final String inputDialogTitle, final String initialValue,
                                                                           final int maximumLength) {
    return new TextInputPanelValue(inputDialogTitle, initialValue, maximumLength);
  }
}
