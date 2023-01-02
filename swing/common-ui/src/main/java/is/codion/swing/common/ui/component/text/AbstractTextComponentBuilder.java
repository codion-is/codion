/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.text.CaseDocumentFilter.DocumentCase;

import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

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
  private Color disabledTextColor;
  private boolean moveCaretToEndOnFocusGained;
  private boolean moveCaretToStartOnFocusGained;
  private Consumer<String> onTextChanged;
  private boolean dragEnabled = false;
  private Character focusAcceleratorKey;

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
  public final B disabledTextColor(Color disabledTextColor) {
    this.disabledTextColor = requireNonNull(disabledTextColor);
    return (B) this;
  }

  @Override
  public final B moveCaretToEndOnFocusGained(boolean moveCaretToEndOnFocusGained) {
    if (moveCaretToStartOnFocusGained) {
      throw new IllegalArgumentException("Caret is already set to move to start on focus gained");
    }
    this.moveCaretToEndOnFocusGained = moveCaretToEndOnFocusGained;
    return (B) this;
  }

  @Override
  public final B moveCaretToStartOnFocusGained(boolean moveCaretToStartOnFocusGained) {
    if (moveCaretToEndOnFocusGained) {
      throw new IllegalArgumentException("Caret is already set to move to end on focus gained");
    }
    this.moveCaretToStartOnFocusGained = moveCaretToStartOnFocusGained;
    return (B) this;
  }

  @Override
  public final B onTextChanged(Consumer<String> onTextChanged) {
    this.onTextChanged = requireNonNull(onTextChanged);
    return (B) this;
  }

  @Override
  public final B dragEnabled(boolean dragEnabled) {
    this.dragEnabled = dragEnabled;
    return (B) this;
  }

  @Override
  public final B focusAccelerator(char focusAcceleratorKey) {
    this.focusAcceleratorKey = focusAcceleratorKey;
    return (B) this;
  }

  @Override
  protected final C createComponent() {
    C textComponent = createTextComponent();
    textComponent.setEditable(editable);
    textComponent.setDragEnabled(dragEnabled);
    if (focusAcceleratorKey != null) {
      textComponent.setFocusAccelerator(focusAcceleratorKey);
    }
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
              .modifiers(InputEvent.CTRL_DOWN_MASK)
              .action(new DeleteNextWordAction()));
      keyEvent(KeyEvents.builder(KeyEvent.VK_BACK_SPACE)
              .modifiers(InputEvent.CTRL_DOWN_MASK)
              .action(new DeletePreviousWordAction()));
    }
    if (disabledTextColor != null) {
      textComponent.setDisabledTextColor(disabledTextColor);
    }
    if (moveCaretToStartOnFocusGained) {
      textComponent.addFocusListener(new MoveCaretToStartListener(textComponent));
    }
    if (moveCaretToEndOnFocusGained) {
      textComponent.addFocusListener(new MoveCaretToEndListener(textComponent));
    }
    if (onTextChanged != null) {
      textComponent.getDocument().addDocumentListener(new OnTextChangedListener(onTextChanged, textComponent));
    }

    return textComponent;
  }

  /**
   * Creates the text component built by this builder.
   * @return a JTextComponent or subclass
   */
  protected abstract C createTextComponent();
}
