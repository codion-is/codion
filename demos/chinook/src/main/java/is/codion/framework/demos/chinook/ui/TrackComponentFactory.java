/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.DefaultEntityComponentFactory;
import is.codion.swing.framework.ui.EntitySearchField;

public final class TrackComponentFactory extends DefaultEntityComponentFactory<Entity, ForeignKey, EntitySearchField> {

  @Override
  public ComponentValue<Entity, EntitySearchField> createComponentValue(ForeignKey foreignKey,
                                                                        SwingEntityEditModel editModel,
                                                                        Entity initialValue) {
    ComponentValue<Entity, EntitySearchField> componentValue = super.createComponentValue(foreignKey, editModel, initialValue);
    EntitySearchField trackSearchField = componentValue.getComponent();
    trackSearchField.setSelectionProvider(new TrackSelectionProvider(trackSearchField.model()));

    return componentValue;
  }
}
