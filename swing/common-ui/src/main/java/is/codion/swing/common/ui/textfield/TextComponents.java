/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.swing.common.ui.textfield.CaseDocumentFilter.DocumentCase;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for JTextComponents.
 */
public final class TextComponents {

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

  private TextComponents() {}

  /**
   * Sets the maximum length for the given document, supports {@link SizedDocument} and {@link AbstractDocument}
   * @param document the document
   * @param maximumLength the maximum string length
   */
  public static void maximumLength(Document document, int maximumLength) {
    requireNonNull(document);
    if (document instanceof SizedDocument) {
      ((SizedDocument) document).setMaximumLength(maximumLength);
    }
    else if (document instanceof AbstractDocument) {
      DocumentFilter documentFilter = ((AbstractDocument) document).getDocumentFilter();
      if (documentFilter == null) {
        CaseDocumentFilter caseDocumentFilter = CaseDocumentFilter.caseDocumentFilter();
        caseDocumentFilter.addValidator(new StringLengthValidator(maximumLength));
        ((AbstractDocument) document).setDocumentFilter(caseDocumentFilter);
      }
      else if (documentFilter instanceof CaseDocumentFilter) {
        CaseDocumentFilter caseDocumentFilter = (CaseDocumentFilter) documentFilter;
        Optional<StringLengthValidator> lengthValidator = caseDocumentFilter.getValidators().stream()
                .filter(StringLengthValidator.class::isInstance)
                .map(StringLengthValidator.class::cast)
                .findFirst();
        if (lengthValidator.isPresent()) {
          lengthValidator.get().setMaximumLength(maximumLength);
        }
        else {
          caseDocumentFilter.addValidator(new StringLengthValidator(maximumLength));
        }
      }
    }
  }

  /**
   * Makes the given document convert all lower case input to upper case,
   * supports {@link SizedDocument} and {@link AbstractDocument}
   * @param document the document
   */
  public static void upperCase(Document document) {
    documentCase(document, DocumentCase.UPPERCASE);
  }

  /**
   * Makes the given document convert all upper case input to lower case,
   * supports {@link SizedDocument} and {@link AbstractDocument}
   * @param document the document
   */
  public static void lowerCase(Document document) {
    documentCase(document, DocumentCase.LOWERCASE);
  }

  /**
   * Selects all text in the given component when it gains focus and clears
   * the selection when focus is lost
   * @param textComponent the text component
   * @param <T> the component type
   * @return the component
   */
  public static <T extends JTextComponent> T selectAllOnFocusGained(T textComponent) {
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
  public static <T extends JTextComponent> T selectNoneOnFocusGained(T textComponent) {
    requireNonNull(textComponent, TEXT_COMPONENT);
    for (FocusListener listener : textComponent.getFocusListeners()) {
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
  public static <T extends JTextComponent> T moveCaretToStartOnFocusGained(T textComponent) {
    requireNonNull(textComponent, TEXT_COMPONENT);
    textComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
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
  public static <T extends JTextComponent> T moveCaretToEndOnFocusGained(T textComponent) {
    requireNonNull(textComponent, TEXT_COMPONENT);
    textComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
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

  private static void documentCase(Document document, DocumentCase documentCase) {
    requireNonNull(document);
    if (document instanceof SizedDocument) {
      ((SizedDocument) document).getDocumentFilter().setDocumentCase(documentCase);
    }
    else if (document instanceof AbstractDocument) {
      DocumentFilter documentFilter = ((AbstractDocument) document).getDocumentFilter();
      if (documentFilter == null) {
        CaseDocumentFilter caseDocumentFilter = CaseDocumentFilter.caseDocumentFilter();
        caseDocumentFilter.setDocumentCase(documentCase);
        ((AbstractDocument) document).setDocumentFilter(caseDocumentFilter);
      }
      else if (documentFilter instanceof CaseDocumentFilter) {
        ((CaseDocumentFilter) documentFilter).setDocumentCase(documentCase);
      }
    }
  }

  private static final class SelectAllListener extends FocusAdapter {

    private final JTextComponent textComponent;

    private SelectAllListener(JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    @Override
    public void focusGained(FocusEvent e) {
      textComponent.selectAll();
    }

    @Override
    public void focusLost(FocusEvent e) {
      textComponent.select(0, 0);
    }
  }
}
