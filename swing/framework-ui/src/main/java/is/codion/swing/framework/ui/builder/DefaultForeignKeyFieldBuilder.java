/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JTextField;
import java.awt.Dimension;
import java.util.function.Consumer;

final class DefaultForeignKeyFieldBuilder extends AbstractComponentBuilder<Entity, JTextField> implements ForeignKeyFieldBuilder {

  private int columns;

  DefaultForeignKeyFieldBuilder(final ForeignKeyProperty attribute, final Value<Entity> value) {
    super(attribute, value);
  }

  @Override
  public ForeignKeyFieldBuilder preferredHeight(final int preferredHeight) {
    return (ForeignKeyFieldBuilder) super.preferredHeight(preferredHeight);
  }

  @Override
  public ForeignKeyFieldBuilder preferredWidth(final int preferredWidth) {
    return (ForeignKeyFieldBuilder) super.preferredWidth(preferredWidth);
  }

  @Override
  public ForeignKeyFieldBuilder preferredSize(final Dimension preferredSize) {
    return (ForeignKeyFieldBuilder) super.preferredSize(preferredSize);
  }

  @Override
  public ForeignKeyFieldBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
    return (ForeignKeyFieldBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
  }

  @Override
  public ForeignKeyFieldBuilder enabledState(final StateObserver enabledState) {
    throw new UnsupportedOperationException("Foreign key fields are read-only and disabled by default");
  }

  @Override
  public ForeignKeyFieldBuilder onBuild(final Consumer<JTextField> onBuild) {
    return (ForeignKeyFieldBuilder) super.onBuild(onBuild);
  }

  @Override
  public ForeignKeyFieldBuilder columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public JTextField build() {
    final JTextField textField = new JTextField(columns);
    setPreferredSize(textField);
    onBuild(textField);
    textField.setEditable(false);
    textField.setFocusable(false);
    textField.setToolTipText(property.getDescription());
    final Value<String> entityStringValue = Value.value();
    value.addDataListener(entity -> entityStringValue.set(entity == null ? "" : entity.toString()));
    ComponentValues.textComponent(textField).link(entityStringValue);
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter(textField);
    }

    return textField;
  }
}
