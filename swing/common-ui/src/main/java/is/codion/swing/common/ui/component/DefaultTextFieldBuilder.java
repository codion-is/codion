/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.Action;
import javax.swing.JTextField;
import java.text.Format;
import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class DefaultTextFieldBuilder<T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> extends AbstractTextComponentBuilder<T, C, B>
        implements TextFieldBuilder<T, C, B> {

  private final Class<T> valueClass;

  private Action action;
  private boolean selectAllOnFocusGained;
  private Supplier<Collection<T>> valueSupplier;
  protected Format format;
  private String dateTimePattern;

  DefaultTextFieldBuilder(final Class<T> valueClass) {
    this.valueClass = requireNonNull(valueClass);
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
  public final B dateTimePattern(final String dateTimePattern) {
    this.dateTimePattern = dateTimePattern;
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
    return textFieldValue(component);
  }

  /**
   * Creates the text field built by this builder.
   * @return a JTextField or subclass
   */
  protected C createTextField() {
    if (Temporal.class.isAssignableFrom(valueClass)) {
      return (C) initializeTemporalField();
    }
    if (valueClass.equals(String.class)) {
      return (C) initializeStringField();
    }
    if (valueClass.equals(Character.class)) {
      return (C) new JTextField(new SizedDocument(1), "", 1);
    }

    throw new IllegalArgumentException("Creating text fields for type: " + valueClass + " is not supported");
  }

  private <C extends JTextField, T> ComponentValue<T, C> textFieldValue(final C textField) {
    requireNonNull(textField);
    if (Temporal.class.isAssignableFrom(valueClass)) {
      return (ComponentValue<T, C>) ComponentValues.temporalField((TemporalField<Temporal>) textField, updateOn);
    }
    if (valueClass.equals(String.class)) {
      return (ComponentValue<T, C>) ComponentValues.textComponent(textField, format, updateOn);
    }
    if (valueClass.equals(Character.class)) {
      return (ComponentValue<T, C>) ComponentValues.characterTextField(textField, updateOn);
    }

    throw new IllegalArgumentException("Text fields not implemented for type: " + valueClass);
  }

  private TemporalField<Temporal> initializeTemporalField() {
    if (dateTimePattern == null) {
      throw new IllegalStateException("dateTimePattern must be specified for temporal fields");
    }
    return TemporalField.builder((Class<Temporal>) valueClass)
            .dateTimePattern(dateTimePattern)
            .build();
  }

  private JTextField initializeStringField() {
    final SizedDocument sizedDocument = new SizedDocument();
    if (maximumLength > 0) {
      sizedDocument.setMaximumLength(maximumLength);
    }

    return new JTextField(sizedDocument, "", 0);
  }
}
