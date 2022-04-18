/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.text.CaseDocumentFilter.DocumentCase;

import javax.swing.AbstractAction;
import javax.swing.JPasswordField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.Utilities;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static java.util.Objects.requireNonNull;

abstract class AbstractTextComponentBuilder<T, C extends JTextComponent, B extends TextComponentBuilder<T, C, B>>
        extends AbstractComponentBuilder<T, C, B> implements TextComponentBuilder<T, C, B> {

  protected UpdateOn updateOn = UpdateOn.KEYSTROKE;

  private boolean editable = true;
  private boolean upperCase;
  private boolean lowerCase;
  private int maximumLength = -1;
  private Insets margin;
  private boolean controlDeleteWord = true;

  protected AbstractTextComponentBuilder(Value<T> linkedValue) {
    super(linkedValue);
  }

  @Override
  public final B editable(boolean editable) {
    this.editable = editable;
    return (B) this;
  }

  @Override
  public final B updateOn(UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return (B) this;
  }

  @Override
  public final B upperCase(boolean upperCase) {
    if (upperCase && lowerCase) {
      throw new IllegalArgumentException("Field is already lowercase");
    }
    this.upperCase = upperCase;
    return (B) this;
  }

  @Override
  public final B lowerCase(boolean lowerCase) {
    if (lowerCase && upperCase) {
      throw new IllegalArgumentException("Field is already uppercase");
    }
    this.lowerCase = lowerCase;
    return (B) this;
  }

  @Override
  public final B maximumLength(int maximumLength) {
    this.maximumLength = maximumLength;
    return (B) this;
  }

  @Override
  public final B margin(Insets margin) {
    this.margin = margin;
    return (B) this;
  }

  @Override
  public final B controlDeleteWord(boolean controlDeleteWord) {
    this.controlDeleteWord = controlDeleteWord;
    return (B) this;
  }

  @Override
  protected final C createComponent() {
    C textComponent = createTextComponent();
    textComponent.setEditable(editable);
    if (margin != null) {
      textComponent.setMargin(margin);
    }
    if (upperCase) {
      new TextFieldDocumentCase(textComponent.getDocument(), DocumentCase.UPPERCASE);
    }
    if (lowerCase) {
      new TextFieldDocumentCase(textComponent.getDocument(), DocumentCase.LOWERCASE);
    }
    if (maximumLength > 0) {
      new MaximumTextFieldLength(textComponent.getDocument(), maximumLength);
    }
    if (controlDeleteWord) {
      keyEvent(KeyEvents.builder(KeyEvent.VK_DELETE)
              .modifiers(KeyEvent.CTRL_DOWN_MASK)
              .action(new DeleteNextWordAction()));
      keyEvent(KeyEvents.builder(KeyEvent.VK_BACK_SPACE)
              .modifiers(KeyEvent.CTRL_DOWN_MASK)
              .action(new DeletePreviousWordAction()));
    }

    return textComponent;
  }

  /**
   * Creates the text component built by this builder.
   * @return a JTextComponent or subclass
   */
  protected abstract C createTextComponent();

  private static final class DeletePreviousWordAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      JTextComponent textComponent = (JTextComponent) actionEvent.getSource();
      Document document = textComponent.getDocument();
      int caretPosition = textComponent.getCaretPosition();
      try {
        int removeFromPosition = textComponent instanceof JPasswordField ?
                0 ://special handling for passwords, just remove everything before cursor
                Utilities.getWordStart(textComponent, caretPosition);
        document.remove(removeFromPosition, caretPosition - removeFromPosition);
      }
      catch (BadLocationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final class DeleteNextWordAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
      JTextComponent textComponent = (JTextComponent) actionEvent.getSource();
      Document document = textComponent.getDocument();
      int caretPosition = textComponent.getCaretPosition();
      try {
        int removeToPosition = textComponent instanceof JPasswordField ?
                document.getLength() ://special handling for passwords, just remove everything after cursor
                Utilities.getWordEnd(textComponent, caretPosition) - caretPosition;
        document.remove(caretPosition, removeToPosition);
      }
      catch (BadLocationException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
