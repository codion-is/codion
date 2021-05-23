/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.Action;
import javax.swing.JTextField;
import java.text.Format;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultTextFieldBuilder<T> extends AbstractTextComponentBuilder<T, JTextField, TextFieldBuilder<T>>
        implements TextFieldBuilder<T> {

  private final Supplier<JTextField> textFieldSupplier;
  private final Class<T> valueClass;

  private Action action;
  private boolean selectAllOnFocusGained;
  private Supplier<Collection<T>> valueSupplier;
  private Format format;

  DefaultTextFieldBuilder(final Value<T> value, final Class<T> valueClass, final Supplier<JTextField> textFieldSupplier) {
    super(value);
    this.textFieldSupplier = textFieldSupplier;
    this.valueClass = valueClass;
  }

  @Override
  public TextFieldBuilder<T> action(final Action action) {
    this.action = requireNonNull(action);

    return transferFocusOnEnter(false);
  }

  @Override
  public TextFieldBuilder<T> selectAllOnFocusGained() {
    this.selectAllOnFocusGained = true;
    return this;
  }

  @Override
  public TextFieldBuilder<T> lookupDialog(final Supplier<Collection<T>> valueSupplier) {
    this.valueSupplier = requireNonNull(valueSupplier);
    return this;
  }

  @Override
  public TextFieldBuilder<T> format(final Format format) {
    this.format = format;
    return this;
  }

  @Override
  protected JTextField buildComponent() {
    final JTextField textField = ComponentValues.textFieldValue(textFieldSupplier.get(), valueClass, value, updateOn, format);
    textField.setEditable(editable);
    textField.setColumns(columns);
    if (action != null) {
      textField.setAction(action);
    }
    if (selectAllOnFocusGained) {
      TextFields.selectAllOnFocusGained(textField);
    }
    if (upperCase) {
      TextFields.upperCase(textField);
    }
    if (lowerCase) {
      TextFields.lowerCase(textField);
    }
    if (valueSupplier != null) {
      Dialogs.addLookupDialog(textField, valueSupplier);
    }

    return textField;
  }

}
