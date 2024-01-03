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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.attribute.ForeignKey;

/**
 * Represents a link between a master and detail model based on a foreign key.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 */
public interface ForeignKeyDetailModelLink<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> extends DetailModelLink<M, E, T> {

  /**
   * Specifies whether a detail model should automatically search by the entity inserted by the master model.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> SEARCH_BY_INSERTED_ENTITY =
          Configuration.booleanValue("is.codion.framework.model.ForeignKeyDetailModelLink.searchByInsertedEntity", false);

  /**
   * Specifies whether a detail model should be automatically refreshed when the selection in the master model changes.
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> REFRESH_ON_SELECTION =
          Configuration.booleanValue("is.codion.framework.model.ForeignKeyDetailModelLink.refreshOnSelection", true);

  /**
   * Specifies whether a detail model sets the master foreign key to null when null or no value is selected in a master model<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION =
          Configuration.booleanValue("is.codion.framework.model.ForeignKeyDetailModelLink.clearForeignKeyOnEmptySelection", false);

  /**
   * @return the foreign key representing this detail model
   */
  ForeignKey foreignKey();

  /**
   * @return the State controlling whether the detail table model should automatically search by the inserted entity
   * when an insert is performed in a master model
   * @see ForeignKeyDetailModelLink#SEARCH_BY_INSERTED_ENTITY
   */
  State searchByInsertedEntity();

  /**
   * @return the State controlling whether the detail table model should be automatically refreshed
   * when the foreign key condition is set according to the master model selection
   * @see ForeignKeyDetailModelLink#REFRESH_ON_SELECTION
   */
  State refreshOnSelection();

  /**
   * Returns the State controlling whether the detail model should set the foreign key to null when null or no value is selected in the master model.
   * @return the State controlling whether a null selection should result in the foreign key being set to null
   * @see ForeignKeyDetailModelLink#CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION
   */
  State clearForeignKeyOnEmptySelection();
}
