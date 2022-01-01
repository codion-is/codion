/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.ComponentValues;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.EntityComboBox;

import javax.swing.JComponent;

import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;

final class DefaultForeignKeyComboBoxBuilder extends AbstractComponentBuilder<Entity, EntityComboBox, ForeignKeyComboBoxBuilder>
        implements ForeignKeyComboBoxBuilder {

  private final SwingEntityComboBoxModel comboBoxModel;
  private int popupWidth;
  private boolean refreshOnSetVisible;

  DefaultForeignKeyComboBoxBuilder(final SwingEntityComboBoxModel comboBoxModel) {
    this.comboBoxModel = comboBoxModel;
    preferredHeight(getPreferredTextFieldHeight());
  }

  @Override
  public ForeignKeyComboBoxBuilder popupWidth(final int popupWidth) {
    this.popupWidth = popupWidth;
    return this;
  }

  @Override
  public ForeignKeyComboBoxBuilder refreshOnSetVisible(final boolean refreshOnSetVisible) {
    this.refreshOnSetVisible = refreshOnSetVisible;
    return this;
  }

  @Override
  protected EntityComboBox buildComponent() {
    final EntityComboBox comboBox = Completion.enable(new EntityComboBox(comboBoxModel));
    if (popupWidth > 0) {
      comboBox.setPopupWidth(popupWidth);
    }
    if (refreshOnSetVisible) {
      comboBox.refreshOnSetVisible();
    }

    return comboBox;
  }

  @Override
  protected ComponentValue<Entity, EntityComboBox> buildComponentValue(final EntityComboBox component) {
    return ComponentValues.comboBox(component);
  }

  @Override
  protected void setInitialValue(final EntityComboBox component, final Entity initialValue) {
    component.setSelectedItem(initialValue);
  }

  @Override
  protected void setTransferFocusOnEnter(final EntityComboBox component) {
    component.setTransferFocusOnEnter(true);
    TransferFocusOnEnter.enable((JComponent) component.getEditor().getEditorComponent());
  }
}
