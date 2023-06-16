/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.ComponentValue;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.Format;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.swing.common.ui.component.text.SizedDocument.sizedDocument;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.util.Objects.requireNonNull;

class DefaultTextFieldBuilder<T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> extends AbstractTextComponentBuilder<T, C, B>
        implements TextFieldBuilder<T, C, B> {

  private final Class<T> valueClass;

  private int columns = -1;
  private Action action;
  private ActionListener actionListener;
  private SelectionProvider<T> selectionProvider;
  private Format format;
  private int horizontalAlignment = SwingConstants.LEADING;
  private String hintText;

  DefaultTextFieldBuilder(Class<T> valueClass, Value<T> linkedValue) {
    super(linkedValue);
    this.valueClass = requireNonNull(valueClass);
    if (valueClass.equals(Character.class)) {
      maximumLength(1);
      columns(1);
    }
    else {
      columns(DEFAULT_TEXT_FIELD_COLUMNS.get());
    }
  }

  @Override
  public final B columns(int columns) {
    this.columns = columns;
    return (B) this;
  }

  @Override
  public final B action(Action action) {
    this.action = requireNonNull(action);

    return transferFocusOnEnter(false);
  }

  @Override
  public final B actionListener(ActionListener actionListener) {
    this.actionListener = actionListener;

    return transferFocusOnEnter(false);
  }

  @Override
  public final B selectionProvider(SelectionProvider<T> selectionProvider) {
    this.selectionProvider = requireNonNull(selectionProvider);
    return (B) this;
  }

  @Override
  public final B format(Format format) {
    this.format = format;
    return (B) this;
  }

  @Override
  public final B horizontalAlignment(int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return (B) this;
  }

  @Override
  public final B hintText(String hintText) {
    if (nullOrEmpty(hintText)) {
      throw new IllegalArgumentException("Hint text is null or empty");
    }
    this.hintText = hintText;
    return (B) this;
  }

  @Override
  protected final C createTextComponent() {
    C textField = createTextField();
    if (columns != -1) {
      textField.setColumns(columns);
    }
    textField.setHorizontalAlignment(horizontalAlignment);
    if (action != null) {
      textField.setAction(action);
    }
    if (actionListener != null) {
      textField.addActionListener(actionListener);
    }
    if (selectionProvider != null) {
      addSelectionProvider(textField, selectionProvider);
    }
    if (hintText != null && textField instanceof HintTextField) {
      ((HintTextField) textField).setHintText(hintText);
    }

    return textField;
  }

  /**
   * @return the {@link javax.swing.text.JTextField} built by this builder.
   */
  protected C createTextField() {
    return (C) new HintTextField(sizedDocument());
  }

  @Override
  protected ComponentValue<T, C> createComponentValue(C component) {
    requireNonNull(component);
    if (valueClass.equals(Character.class)) {
      return (ComponentValue<T, C>) new CharacterFieldValue(component, updateOn);
    }

    return new DefaultTextComponentValue<>(component, format, updateOn);
  }

  protected final Format format() {
    return format;
  }

  private void addSelectionProvider(C textField, SelectionProvider<T> selectionProvider) {
    KeyEvents.builder(VK_SPACE)
            .modifiers(CTRL_DOWN_MASK)
            .action(new SelectionAction<>(textField, selectionProvider))
            .enable(textField);
  }

  private static final class SelectionAction<T> extends AbstractAction {

    private final JTextField textField;
    private final SelectionProvider<T> selectionProvider;

    private SelectionAction(JTextField textField, SelectionProvider<T> selectionProvider) {
      super("DefaultTextFieldBuilder.SelectionAction");
      this.textField = textField;
      this.selectionProvider = selectionProvider;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      selectionProvider.select(textField)
              .ifPresent(value -> textField.setText(value.toString()));
    }
  }
}
