/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.entity.ForeignKey;

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
   * @return true if the detail table model should automatically search by the inserted entity
   * when an insert is performed in a master model
   * @see ForeignKeyDetailModelLink#SEARCH_BY_INSERTED_ENTITY
   */
  boolean isSearchByInsertedEntity();

  /**
   * @param searchOnMasterInsert if true then the detail table model will automatically search by the inserted entity
   * when an insert is performed in a master model
   * @return this {@link ForeignKeyDetailModelLink} instance
   * @see ForeignKeyDetailModelLink#SEARCH_BY_INSERTED_ENTITY
   */
  ForeignKeyDetailModelLink<M, E, T> setSearchByInsertedEntity(boolean searchOnMasterInsert);

  /**
   * @return true if the detail table model should be automatically refreshed
   * when the foreign key condition is set according to the master model selection
   * @see ForeignKeyDetailModelLink#REFRESH_ON_SELECTION
   */
  boolean isRefreshOnSelection();

  /**
   * @param refreshOnSelection if true then the detail table model will be automatically refreshed
   * when the foreign key condition is set according to the master model selection
   * @return this {@link ForeignKeyDetailModelLink} instance
   * @see ForeignKeyDetailModelLink#REFRESH_ON_SELECTION
   */
  ForeignKeyDetailModelLink<M, E, T> setRefreshOnSelection(boolean refreshOnSelection);

  /**
   * Returns true if the detail model should set the foreign key to null when null or no value is selected in the master model.
   * @return true if a null selection should result in the foreign key being set to null
   * @see ForeignKeyDetailModelLink#CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION
   */
  boolean isClearForeignKeyOnEmptySelection();

  /**
   * Set to true if the detail model should set the foreign key to null when null or no value is selected in the master model.
   * @param clearForeignKeyOnEmptySelection true if initialization with a null value should set the foreign key to null
   * @return this {@link ForeignKeyDetailModelLink} instance
   * @see ForeignKeyDetailModelLink#CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION
   */
  ForeignKeyDetailModelLink<M, E, T> setClearForeignKeyOnEmptySelection(boolean clearForeignKeyOnEmptySelection);
}
