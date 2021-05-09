/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComponentValues;
import is.codion.swing.framework.ui.EntitySearchField;

import javax.swing.JComponent;

final class ChinookComponentValues extends EntityComponentValues {

  private final ForeignKey trackForeignKey;

  ChinookComponentValues(final ForeignKey trackForeignKey) {
    this.trackForeignKey = trackForeignKey;
  }

  @Override
  protected <T extends JComponent> ComponentValue<Entity, T> createForeignKeyComponentValue(final ForeignKey foreignKey,
                                                                                            final SwingEntityEditModel editModel,
                                                                                            final Entity initialValue) {
    final ComponentValue<Entity, JComponent> componentValue =
            super.createForeignKeyComponentValue(foreignKey, editModel, initialValue);
    if (foreignKey.equals(trackForeignKey)) {
      final EntitySearchField trackSearchField = (EntitySearchField) componentValue.getComponent();
      trackSearchField.setSelectionProvider(new TrackSelectionProvider(trackSearchField.getModel()));
    }

    return (ComponentValue<Entity, T>) componentValue;
  }
}
