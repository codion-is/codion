/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.AbstractComponentValue;
import is.codion.swing.common.ui.value.ComponentValue;

import javax.swing.JTextField;

final class DefaultForeignKeyFieldBuilder extends AbstractComponentBuilder<Entity, JTextField, ForeignKeyFieldBuilder>
        implements ForeignKeyFieldBuilder {

  private int columns;

  DefaultForeignKeyFieldBuilder() {
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

    return textField;
  }

  @Override
  protected ComponentValue<Entity, JTextField> buildComponentValue(final JTextField textField) {
    return new AbstractComponentValue<Entity, JTextField>(textField) {
      @Override
      protected Entity getComponentValue(final JTextField component) {
        return null;
      }

      @Override
      protected void setComponentValue(final JTextField component, final Entity entity) {
        component.setText(entity == null ? "" : entity.toString());
      }
    };
  }
}
