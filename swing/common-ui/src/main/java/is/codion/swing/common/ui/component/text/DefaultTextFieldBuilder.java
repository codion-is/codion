/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.dialog.SelectionDialogBuilder.Selector;
import is.codion.swing.common.ui.key.KeyEvents;

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
  private Selector<T> selector;
  private Format format;
  private int horizontalAlignment = SwingConstants.LEADING;
  private String hintText;

  DefaultTextFieldBuilder(Class<T> valueClass, Value<T> linkedValue) {
    super(linkedValue);
    this.valueClass = requireNonNull(valueClass);
    if (valueClass.equals(Character.class)) {
      maximumLength(1);
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
  public final B selector(Selector<T> selector) {
    this.selector = requireNonNull(selector);
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
    if (selector != null) {
      setSelector(textField, selector);
    }
    if (hintText != null && textField instanceof HintTextField) {
      ((HintTextField) textField).hintText().set(hintText);
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

  private void setSelector(C textField, Selector<T> selector) {
    KeyEvents.builder(VK_SPACE)
            .modifiers(CTRL_DOWN_MASK)
            .action(new SelectionAction<>(textField, selector))
            .enable(textField);
  }

  private static final class SelectionAction<T> extends AbstractAction {

    private final JTextField textField;
    private final Selector<T> selector;

    private SelectionAction(JTextField textField, Selector<T> selector) {
      super("DefaultTextFieldBuilder.SelectionAction");
      this.textField = textField;
      this.selector = selector;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      selector.select(textField)
              .ifPresent(value -> textField.setText(value.toString()));
    }
  }
}
