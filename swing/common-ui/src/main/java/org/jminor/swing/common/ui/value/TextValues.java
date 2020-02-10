/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.value.Value;
import org.jminor.swing.common.ui.textfield.TextInputPanel;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.text.Format;

import static org.jminor.common.Util.nullOrEmpty;

/**
 * Utility class for text based {@link ComponentValue} instances.
 */
public final class TextValues {

  private TextValues() {}

  /**
   * @return a Value bound to a JTextField
   */
  public static ComponentValue<String, JTextField> textValue() {
    return textValue(new JTextField());
  }

  /**
   * @param textComponent the component
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textValue(final C textComponent) {
    return new AbstractTextComponentValue<String, C>(textComponent, true, true) {
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
  public static <C extends JTextComponent> ComponentValue<String, C> textValue(final C textComponent, final Format format) {
    return textValue(textComponent, format, true);
  }

  /**
   * @param textComponent the component
   * @param format the format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param <C> the text component type
   * @return a Value bound to the given component
   */
  public static <C extends JTextComponent> ComponentValue<String, C> textValue(final C textComponent, final Format format,
                                                                               final boolean updateOnKeystroke) {
    return new FormattedTextComponentValue<>(textComponent, format, updateOnKeystroke);
  }

  /**
   * Instantiates a new String based ComponentValue.
   * @param inputDialogTitle the title to use for the lookup input dialog
   * @param initialValue the initial value
   * @param maxLength the maximum input length, -1 for no limit
   * @return a String based ComponentValue
   */
  public static ComponentValue<String, TextInputPanel> textValue(final String inputDialogTitle, final String initialValue,
                                                                 final int maxLength) {
    return new TextInputPanelValue(inputDialogTitle, initialValue, maxLength);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param value the value to link with the component
   */
  public static void textValueLink(final JTextComponent textComponent, final Value<String> value) {
    textValueLink(textComponent, value, null, true);
  }

  /**
   * @param textComponent the text component to link with the value
   * @param value the value to link with the component
   * @param format the format to use when displaying the linked value, null if no formatting should be performed
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   */
  public static void textValueLink(final JTextComponent textComponent, final Value<String> value, final Format format,
                                   final boolean updateOnKeystroke) {
    value.link(textValue(textComponent, format, updateOnKeystroke));
  }
}
