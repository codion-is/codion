/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.Action;
import javax.swing.JTextField;
import java.text.Format;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class DefaultTextFieldBuilder<T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> extends AbstractTextComponentBuilder<T, C, B>
        implements TextFieldBuilder<T, C, B> {

  private final Class<T> valueClass;

  private Action action;
  private boolean selectAllOnFocusGained;
  private Supplier<Collection<T>> valueSupplier;
  private Format format;

  DefaultTextFieldBuilder(final Class<T> valueClass) {
    requireNonNull(valueClass);
    if (!(valueClass.equals(String.class) || valueClass.equals(Character.class))) {
      throw new IllegalArgumentException("TextFieldBuilder only supports String and Character");
    }
    this.valueClass = valueClass;
  }

  @Override
  public final B action(final Action action) {
    this.action = requireNonNull(action);

    return transferFocusOnEnter(false);
  }

  @Override
  public final B selectAllOnFocusGained() {
    this.selectAllOnFocusGained = true;
    return (B) this;
  }

  @Override
  public final B lookupDialog(final Supplier<Collection<T>> valueSupplier) {
    this.valueSupplier = requireNonNull(valueSupplier);
    return (B) this;
  }

  @Override
  public final B format(final Format format) {
    this.format = format;
    return (B) this;
  }

  @Override
  protected final C buildComponent() {
    final C textField = createTextField();
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

  @Override
  protected ComponentValue<T, C> buildComponentValue(final C component) {
    requireNonNull(component);
    if (valueClass.equals(String.class)) {
      return (ComponentValue<T, C>) ComponentValues.textComponent(component, format, updateOn);
    }

    return (ComponentValue<T, C>) ComponentValues.characterTextField(component, updateOn);
  }

  /**
   * Creates the text field built by this builder.
   * @return a JTextField or subclass
   */
  protected C createTextField() {
    if (valueClass.equals(String.class)) {
      return (C) initializeStringField();
    }

    return (C) new JTextField(new SizedDocument(1), "", 1);
  }

  protected final Class<T> getValueClass() {
    return valueClass;
  }

  protected final Format getFormat() {
    return format;
  }

  private JTextField initializeStringField() {
    final SizedDocument sizedDocument = new SizedDocument();
    if (maximumLength > 0) {
      sizedDocument.setMaximumLength(maximumLength);
    }

    return new JTextField(sizedDocument, "", 0);
  }
}
