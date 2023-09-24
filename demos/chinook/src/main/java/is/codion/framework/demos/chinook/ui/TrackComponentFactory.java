/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
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
    trackSearchField.setSelectionProvider(new TrackSelectionProvider(trackSearchField.model()));

    return componentValue;
  }
}
