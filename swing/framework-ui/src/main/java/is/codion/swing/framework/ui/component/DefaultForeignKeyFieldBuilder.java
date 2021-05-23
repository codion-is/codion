/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JTextField;

final class DefaultForeignKeyFieldBuilder extends AbstractComponentBuilder<Entity, JTextField, ForeignKeyFieldBuilder>
        implements ForeignKeyFieldBuilder {

  private int columns;

  DefaultForeignKeyFieldBuilder(final Value<Entity> value) {
    super(value);
    preferredHeight(TextFields.getPreferredTextFieldHeight());
  }

  @Override
  public ForeignKeyFieldBuilder columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  protected JTextField buildComponent() {
    final JTextField textField = new JTextField(columns);
    textField.setEditable(false);
    textField.setFocusable(false);
    final Value<String> entityStringValue = Value.value();
    value.addDataListener(entity -> entityStringValue.set(entity == null ? "" : entity.toString()));
    ComponentValues.textComponent(textField).link(entityStringValue);

    return textField;
  }
}
