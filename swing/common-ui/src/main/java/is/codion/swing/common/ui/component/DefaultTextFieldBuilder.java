/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TextComponents;
import is.codion.swing.common.ui.textfield.TextFieldHint;

import javax.swing.Action;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.text.Format;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultTextFieldBuilder<T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> extends AbstractTextComponentBuilder<T, C, B>
        implements TextFieldBuilder<T, C, B> {

  private final Class<T> valueClass;

  private int columns;
  private Action action;
  private ActionListener actionListener;
  private boolean selectAllOnFocusGained;
  private Supplier<Collection<T>> valueSupplier;
  private Format format;
  private int horizontalAlignment = SwingConstants.LEADING;
  private String hintText;
  private Consumer<String> onTextChanged;

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
  public final B selectAllOnFocusGained(boolean selectAllOnFocusGained) {
    this.selectAllOnFocusGained = selectAllOnFocusGained;
    return (B) this;
  }

  @Override
  public final B lookupDialog(Supplier<Collection<T>> valueSupplier) {
    this.valueSupplier = requireNonNull(valueSupplier);
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
  public final B onTextChanged(Consumer<String> onTextChanged) {
    this.onTextChanged = requireNonNull(onTextChanged);
    return (B) this;
  }

  @Override
  protected final C createTextComponent() {
    C textField = createTextField();
    textField.setColumns(columns);
    textField.setHorizontalAlignment(horizontalAlignment);
    if (action != null) {
      textField.setAction(action);
    }
    if (actionListener != null) {
      textField.addActionListener(actionListener);
    }
    if (selectAllOnFocusGained) {
      TextComponents.selectAllOnFocusGained(textField);
    }
    if (valueSupplier != null) {
      Dialogs.addLookupDialog(textField, valueSupplier);
    }
    if (hintText != null) {
      TextFieldHint.create(textField, hintText);
    }
    if (onTextChanged != null) {
      textField.getDocument().addDocumentListener((DocumentAdapter) documentEvent ->
              onTextChanged.accept(textField.getText()));
    }

    return textField;
  }

  /**
   * @return the {@link javax.swing.text.JTextComponent} built by this builder.
   */
  protected C createTextField() {
    if (!(valueClass.equals(String.class) || valueClass.equals(Character.class))) {
      throw new IllegalArgumentException("DefaultTextFieldBuilder only supports String and Character");
    }

    return (C) new JTextField(new SizedDocument(), "", 1);
  }

  @Override
  protected ComponentValue<T, C> buildComponentValue(C component) {
    requireNonNull(component);
    if (valueClass.equals(String.class)) {
      return new FormattedTextComponentValue<>(component, format, updateOn);
    }

    return (ComponentValue<T, C>) new CharacterFieldValue(component, updateOn);
  }

  @Override
  protected void setInitialValue(C component, T initialValue) {
    if (initialValue instanceof String) {
      component.setText((String) initialValue);
    }
    else if (initialValue instanceof Character) {
      component.setText(String.valueOf(initialValue));
    }
  }

  protected final Class<T> getValueClass() {
    return valueClass;
  }

  protected final Format getFormat() {
    return format;
  }
}
