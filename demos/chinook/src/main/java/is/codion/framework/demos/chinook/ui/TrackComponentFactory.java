/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.DefaultEntityComponentFactory;
import is.codion.swing.framework.ui.component.EntitySearchField;

public final class TrackComponentFactory extends DefaultEntityComponentFactory<Entity, ForeignKey, EntitySearchField> {

  @Override
  public ComponentValue<Entity, EntitySearchField> componentValue(ForeignKey foreignKey,
                                                                  SwingEntityEditModel editModel,
                                                                  Entity initialValue) {
    ComponentValue<Entity, EntitySearchField> componentValue = super.componentValue(foreignKey, editModel, initialValue);
    EntitySearchField trackSearchField = componentValue.component();
    trackSearchField.setSelectorFactory(new TrackSelectorFactory());

    return componentValue;
  }
}
