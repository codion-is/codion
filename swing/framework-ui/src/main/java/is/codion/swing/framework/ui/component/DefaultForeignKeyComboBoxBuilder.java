/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.EntityComboBox;

import javax.swing.JComponent;

final class DefaultForeignKeyComboBoxBuilder extends AbstractComponentBuilder<Entity, EntityComboBox, ForeignKeyComboBoxBuilder>
        implements ForeignKeyComboBoxBuilder {

  private final SwingEntityComboBoxModel comboBoxModel;
  private int popupWidth;

  DefaultForeignKeyComboBoxBuilder(final Value<Entity> value, final SwingEntityComboBoxModel comboBoxModel) {
    super(value);
    this.comboBoxModel = comboBoxModel;
    preferredHeight(TextFields.getPreferredTextFieldHeight());
  }

  @Override
  public ForeignKeyComboBoxBuilder popupWidth(final int popupWidth) {
    this.popupWidth = popupWidth;
    return this;
  }

  @Override
  protected EntityComboBox buildComponent() {
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    ComponentValues.comboBox(comboBox).link(value);
    Completion.addComboBoxCompletion(comboBox);
    if (popupWidth > 0) {
      comboBox.setPopupWidth(popupWidth);
    }

    return comboBox;
  }

  @Override
  protected void setTransferFocusOnEnter(final EntityComboBox component) {
    component.setTransferFocusOnEnter(true);
    Components.transferFocusOnEnter((JComponent) component.getEditor().getEditorComponent());
  }
}
