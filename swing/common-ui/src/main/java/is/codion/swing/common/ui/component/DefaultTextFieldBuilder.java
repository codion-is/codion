/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.textfield.SizedDocument;
import is.codion.swing.common.ui.textfield.TextComponents;

import javax.swing.Action;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.text.Format;
import java.util.Collection;
import java.util.function.Supplier;

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

  DefaultTextFieldBuilder(final Class<T> valueClass, final Value<T> linkedValue) {
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
  public final B columns(final int columns) {
    this.columns = columns;
    return (B) this;
  }

  @Override
  public final B action(final Action action) {
    this.action = requireNonNull(action);

    return transferFocusOnEnter(false);
  }

  @Override
  public final B actionListener(final ActionListener actionListener) {
    this.actionListener = actionListener;

    return transferFocusOnEnter(false);
  }

  @Override
  public final B selectAllOnFocusGained(final boolean selectAllOnFocusGained) {
    this.selectAllOnFocusGained = selectAllOnFocusGained;
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
  public final B horizontalAlignment(final int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return (B) this;
  }

  @Override
  protected C createTextComponent() {
    if (!(valueClass.equals(String.class) || valueClass.equals(Character.class))) {
      throw new IllegalArgumentException("TextFieldBuilder only supports String and Character");
    }
    final C textField = (C) new JTextField(new SizedDocument(), "", 1);
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

  @Override
  protected void setInitialValue(final C component, final T initialValue) {
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
