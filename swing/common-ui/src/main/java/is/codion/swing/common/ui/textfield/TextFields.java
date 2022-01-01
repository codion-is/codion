/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.UpdateOn;
import is.codion.swing.common.ui.textfield.CaseDocumentFilter.DocumentCase;

import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.Format;

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
      ((PlainDocument) textComponent.getDocument()).setDocumentFilter(CaseDocumentFilter.caseDocumentFilter(DocumentCase.UPPERCASE));
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
      ((PlainDocument) textComponent.getDocument()).setDocumentFilter(CaseDocumentFilter.caseDocumentFilter(DocumentCase.LOWERCASE));
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
   * @param textComponent the text component
   * @param format the format
   * @param updateOn the updateOn policy
   * @param <C> the component type
   * @return a text component value
   */
  public static <C extends JTextComponent> ComponentValue<String, C> formattedTextComponentValue(final C textComponent,
                                                                                                 final Format format,
                                                                                                 final UpdateOn updateOn) {
    return new FormattedTextComponentValue<>(textComponent, format, updateOn);
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
