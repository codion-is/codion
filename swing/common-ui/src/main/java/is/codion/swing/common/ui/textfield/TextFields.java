/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.swing.common.ui.textfield.CaseDocumentFilter.DocumentCase;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.text.MaskFormatter;
import javax.swing.text.PlainDocument;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.ParseException;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for TextFields.
 */
public final class TextFields {

  /**
   * A square dimension which sides are the same as the preferred height of a JTextField.
   * This comes in handy for example when adding "..." lookup buttons next to text fields.
   */
  public static final Dimension DIMENSION_TEXT_FIELD_SQUARE = new Dimension(getPreferredTextFieldHeight(), getPreferredTextFieldHeight());

  private static final String TEXT_COMPONENT = "textComponent";

  /**
   * A text field used by getPreferredTextFieldSize and getPreferredTextFieldHeight
   */
  private static JTextField textField;

  private TextFields() {}

  /**
   * Makes {@code textComponent} convert all lower case input to upper case
   * @param textComponent the text component
   * @param <T> the component type
   * @return the text component
   */
  public static <T extends JTextComponent> T upperCase(final T textComponent) {
    requireNonNull(textComponent, TEXT_COMPONENT);
    if (textComponent.getDocument() instanceof SizedDocument) {
      ((SizedDocument) textComponent.getDocument()).getDocumentFilter().setDocumentCase(DocumentCase.UPPERCASE);
    }
    else {
      ((PlainDocument) textComponent.getDocument()).setDocumentFilter(new CaseDocumentFilter(DocumentCase.UPPERCASE));
    }

    return textComponent;
  }

  /**
   * Makes {@code textComponent} convert all upper case input to lower case
   * @param textComponent the text component
   * @param <T> the component type
   * @return the text component
   */
  public static <T extends JTextComponent> T lowerCase(final T textComponent) {
    requireNonNull(textComponent, TEXT_COMPONENT);
    if (textComponent.getDocument() instanceof SizedDocument) {
      ((SizedDocument) textComponent.getDocument()).getDocumentFilter().setDocumentCase(DocumentCase.LOWERCASE);
    }
    else {
      ((PlainDocument) textComponent.getDocument()).setDocumentFilter(new CaseDocumentFilter(DocumentCase.LOWERCASE));
    }

    return textComponent;
  }

  /**
   * Enables the hint text for the given field
   * @param textField the text field
   * @param hintText the hint text
   * @return the {@link Hint} instance
   */
  public static Hint hint(final JTextField textField, final String hintText) {
    return new DefaultTextFieldHint(textField, hintText);
  }

  /**
   * Selects all text in the given component when it gains focus and clears
   * the selection when focus is lost
   * @param textComponent the text component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JTextComponent> T selectAllOnFocusGained(final T textComponent) {
    requireNonNull(textComponent, TEXT_COMPONENT);
    textComponent.addFocusListener(new SelectAllListener(textComponent));

    return textComponent;
  }

  /**
   * Reverts the functionality added via {@link #selectAllOnFocusGained(JTextComponent)}.
   * @param textComponent the text component
   * @param <T> the component type
   * @return the text component
   * @see #selectAllOnFocusGained(JTextComponent)
   */
  public static <T extends JTextComponent> T selectNoneOnFocusGained(final T textComponent) {
    requireNonNull(textComponent, TEXT_COMPONENT);
    for (final FocusListener listener : textComponent.getFocusListeners()) {
      if (listener instanceof SelectAllListener) {
        textComponent.removeFocusListener(listener);
      }
    }

    return textComponent;
  }

  /**
   * Sets the caret position to 0 in the given text component when it gains focus
   * @param textComponent the text component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JTextComponent> T moveCaretToStartOnFocusGained(final T textComponent) {
    requireNonNull(textComponent, TEXT_COMPONENT);
    textComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        textComponent.setCaretPosition(0);
      }
    });

    return textComponent;
  }

  /**
   * Sets the caret position to the right of the last character in the given text component when it gains focus
   * @param textComponent the text component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JTextComponent> T moveCaretToEndOnFocusGained(final T textComponent) {
    requireNonNull(textComponent, TEXT_COMPONENT);
    textComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        textComponent.setCaretPosition(textComponent.getText().length());
      }
    });

    return textComponent;
  }

  /**
   * @return the preferred size of a JTextField
   */
  public static synchronized Dimension getPreferredTextFieldSize() {
    if (textField == null) {
      textField = new JTextField();
    }

    return textField.getPreferredSize();
  }

  /**
   * @return the preferred height of a JTextField
   */
  public static synchronized int getPreferredTextFieldHeight() {
    return getPreferredTextFieldSize().height;
  }

  /**
   * Somewhat of a hacky way to keep the current field selection and caret position when
   * the field gains focus, in case the content length has not changed
   * http://stackoverflow.com/a/2202073/317760
   * @param mask the format mask
   * @param valueContainsLiterals true if the value should contain literals
   * @return a new MaskFormatter
   * @throws ParseException in case of an exception while parsing the mask
   */
  public static MaskFormatter fieldFormatter(final String mask, final boolean valueContainsLiterals) throws ParseException {
    return new FieldFormatter(mask, valueContainsLiterals);
  }

  /**
   * A hint text for text fields, that is, text that is shown
   * when the field contains no data, is empty and unfocused.
   * @see TextFields#hint(JTextField, String)
   */
  public interface Hint {

    /**
     * @return the search hint string
     */
    String getHintText();

    /**
     * @return true if the field does not have focus and is displayint the hint text
     */
    boolean isHintVisible();

    /**
     * Updates the hint state for the component, showing the hint text if the component
     * contains no text and is not focused. This is done automatically on focus gained/lost events,
     * but sometimes it may be necessary to update manually.
     */
    void updateHint();
  }

  private static final class SelectAllListener extends FocusAdapter {

    private final JTextComponent textComponent;

    private SelectAllListener(final JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    @Override
    public void focusGained(final FocusEvent e) {
      textComponent.selectAll();
    }

    @Override
    public void focusLost(final FocusEvent e) {
      textComponent.select(0, 0);
    }
  }
}
