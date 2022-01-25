/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.DefaultComboBoxBuilder;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.EntityComboBox;

import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;

final class DefaultForeignKeyComboBoxBuilder extends DefaultComboBoxBuilder<Entity, EntityComboBox, ForeignKeyComboBoxBuilder>
        implements ForeignKeyComboBoxBuilder {

  private boolean refreshOnSetVisible;

  DefaultForeignKeyComboBoxBuilder(final SwingEntityComboBoxModel comboBoxModel) {
    super(comboBoxModel, null);
    preferredHeight(getPreferredTextFieldHeight());
  }

  @Override
  public ForeignKeyComboBoxBuilder refreshOnSetVisible(final boolean refreshOnSetVisible) {
    this.refreshOnSetVisible = refreshOnSetVisible;
    return this;
  }

  @Override
  protected EntityComboBox createComboBox() {
    final EntityComboBox comboBox = new EntityComboBox((SwingEntityComboBoxModel) comboBoxModel);
    if (refreshOnSetVisible) {
      comboBox.refreshOnSetVisible();
    }

    return comboBox;
  }
}
