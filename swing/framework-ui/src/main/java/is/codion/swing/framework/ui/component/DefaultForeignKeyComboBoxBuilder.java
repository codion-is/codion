/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.DefaultComboBoxBuilder;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.ui.EntityComboBox;

final class DefaultForeignKeyComboBoxBuilder extends DefaultComboBoxBuilder<Entity, EntityComboBox, ForeignKeyComboBoxBuilder>
        implements ForeignKeyComboBoxBuilder {

  DefaultForeignKeyComboBoxBuilder(final SwingEntityComboBoxModel comboBoxModel) {
    super(comboBoxModel, null);
  }

  @Override
  protected EntityComboBox createComboBox() {
    return new EntityComboBox((SwingEntityComboBoxModel) comboBoxModel);
  }
}
