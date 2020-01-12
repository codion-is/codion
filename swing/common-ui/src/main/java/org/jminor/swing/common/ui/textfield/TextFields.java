package org.jminor.swing.common.ui.textfield;

import org.jminor.common.DateFormats;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.MaskFormatter;
import javax.swing.text.PlainDocument;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Locale;

/**
 * A utility class for TextFields.
 */
public final class TextFields {

  /**
   * A square dimension which sides are the same as the preferred height of a JTextField.
   * This comes in handy for example when adding "..." lookup buttons next to text fields.
   */
  public static final Dimension DIMENSION_TEXT_FIELD_SQUARE = new Dimension(getPreferredTextFieldHeight(), getPreferredTextFieldHeight());

  /**
   * A text field used by getPreferredTextFieldSize and getPreferredTextFieldHeight
   */
  private static JTextField textField;

  private TextFields() {}

  /**
   * Creates a formatted text field using the given format
   * @param dateFormat the format
   * @param initialValue the initial value
   * @return the text field
   */
  public static JFormattedTextField createFormattedTemporalField(final String dateFormat, final Temporal initialValue) {
    final JFormattedTextField textField = createFormattedField(DateFormats.getDateMask(dateFormat));
    if (initialValue != null) {
      textField.setText(DateTimeFormatter.ofPattern(dateFormat).format(initialValue));
    }

    return textField;
  }

  /**
   * Creates a JFormattedTextField with the given mask, using '_' as a placeholder character, disallowing invalid values,
   * with JFormattedTextField.COMMIT as focus lost behaviour. By default the value contains the literal characters.
   * @param mask the format mask
   * @return a JFormattedTextField
   */
  public static JFormattedTextField createFormattedField(final String mask) {
    return createFormattedField(mask, true);
  }

  /**
   * Creates a JFormattedTextField with the given mask, using '_' as a placeholder character, disallowing invalid values,
   * with JFormattedTextField.COMMIT as focus lost behaviour.
   * @param mask the format mask
   * @param valueContainsLiteralCharacter if true, the value will also contain the literal characters in mask
   * @return a JFormattedTextField
   */
  public static JFormattedTextField createFormattedField(final String mask, final boolean valueContainsLiteralCharacter) {
    return createFormattedField(mask, valueContainsLiteralCharacter, false);
  }

  /**
   * Creates a JFormattedTextField with the given mask, using '_' as a placeholder character, disallowing invalid values,
   * with JFormattedTextField.COMMIT as focus lost behaviour.
   * @param mask the format mask
   * @param valueContainsLiteralCharacter if true, the value will also contain the literal characters in mask
   * @param charsAsUpper if true then the field will automatically convert characters to upper case
   * @return a JFormattedTextField
   */
  public static JFormattedTextField createFormattedField(final String mask, final boolean valueContainsLiteralCharacter,
                                                         final boolean charsAsUpper) {
    try {
      final JFormattedTextField formattedTextField =
              new JFormattedTextField(new FieldFormatter(mask, charsAsUpper, valueContainsLiteralCharacter));
      formattedTextField.setFocusLostBehavior(JFormattedTextField.COMMIT);

      return formattedTextField;
    }
    catch (final ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Makes {@code textComponent} convert all lower case input to upper case
   * @param textComponent the text component
   * @param <T> the component type
   * @return the text component
   */
  public static <T extends JTextComponent> T makeUpperCase(final T textComponent) {
    if (textComponent.getDocument() instanceof SizedDocument) {
      ((SizedDocument) textComponent.getDocument()).setDocumentCase(SizedDocument.DocumentCase.UPPERCASE);
    }
    else {
      ((PlainDocument) textComponent.getDocument()).setDocumentFilter(new CaseDocumentFilter(SizedDocument.DocumentCase.UPPERCASE));
    }

    return textComponent;
  }

  /**
   * Makes {@code textComponent} convert all upper case input to lower case
   * @param textComponent the text component
   * @param <T> the component type
   * @return the text component
   */
  public static <T extends JTextComponent> T makeLowerCase(final T textComponent) {
    if (textComponent.getDocument() instanceof SizedDocument) {
      ((SizedDocument) textComponent.getDocument()).setDocumentCase(SizedDocument.DocumentCase.LOWERCASE);
    }
    else {
      ((PlainDocument) textComponent.getDocument()).setDocumentFilter(new CaseDocumentFilter(SizedDocument.DocumentCase.LOWERCASE));
    }

    return textComponent;
  }

  /**
   * Selects all text in the given component when it gains focus and clears
   * the selection when focus is lost
   * @param textComponent the text component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JTextComponent> T selectAllOnFocusGained(final T textComponent) {
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
    textComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        SwingUtilities.invokeLater(() -> textComponent.setCaretPosition(0));
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
    textComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        SwingUtilities.invokeLater(() -> textComponent.setCaretPosition(textComponent.getText().length()));
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
   * Somewhat of a hack to keep the current field selection and caret position when
   * the field gains focus, in case the content length has not changed
   * http://stackoverflow.com/a/2202073/317760
   */
  private static final class FieldFormatter extends MaskFormatter {

    private final boolean toUpperCase;

    private FieldFormatter(final String mask, final boolean toUpperCase, final boolean valueContainsLiteralCharacter) throws ParseException {
      super(mask);
      this.toUpperCase = toUpperCase;
      setPlaceholderCharacter('_');
      setAllowsInvalid(false);
      setValueContainsLiteralCharacters(valueContainsLiteralCharacter);
    }

    @Override
    public Object stringToValue(final String string) throws ParseException {
      String value = string;
      if (toUpperCase) {
        value = value.toUpperCase(Locale.getDefault());
      }

      return super.stringToValue(value);
    }

    @Override
    public void install(final JFormattedTextField field) {
      final int previousLength = field.getDocument().getLength();
      final int currentCaretPosition = field.getCaretPosition();
      final int currentSelectionStart = field.getSelectionStart();
      final int currentSelectionEnd = field.getSelectionEnd();
      super.install(field);
      if (previousLength == field.getDocument().getLength()) {
        if (currentSelectionEnd - currentSelectionStart > 0) {
          field.setCaretPosition(currentSelectionStart);
          field.moveCaretPosition(currentSelectionEnd);
        }
        else {
          field.setCaretPosition(currentCaretPosition);
        }
      }
    }
  }

  private static final class CaseDocumentFilter extends DocumentFilter {

    private final SizedDocument.DocumentCase documentCase;

    private CaseDocumentFilter(final SizedDocument.DocumentCase documentCase) {
      this.documentCase = documentCase;
    }

    @Override
    public void insertString(final FilterBypass bypass, final int offset, final String string,
                             final AttributeSet attributeSet) throws BadLocationException {
      super.insertString(bypass, offset, fixCase(string), attributeSet);
    }

    @Override
    public void replace(final FilterBypass bypass, final int offset, final int length, final String string,
                        final AttributeSet attributeSet) throws BadLocationException {
      super.replace(bypass, offset, length, fixCase(string), attributeSet);
    }

    private String fixCase(final String string) {
      if (string == null) {
        return string;
      }
      switch (documentCase) {
        case UPPERCASE: return string.toUpperCase(Locale.getDefault());
        case LOWERCASE: return string.toLowerCase(Locale.getDefault());
        default: return string;
      }
    }
  }

  private static final class SelectAllListener extends FocusAdapter {

    private final JTextComponent textComponent;

    private SelectAllListener(final JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    @Override
    public void focusGained(final FocusEvent e) {
      SwingUtilities.invokeLater(textComponent::selectAll);
    }

    @Override
    public void focusLost(final FocusEvent e) {
      SwingUtilities.invokeLater(() -> textComponent.select(0, 0));
    }
  }
}
