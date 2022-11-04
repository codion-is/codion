/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.framework.domain.entity.ForeignKey;

/**
 * Represents a link between a master and detail model based on a foreign key.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 */
public interface ForeignKeyEntityModelLink<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> extends EntityModelLink<M, E, T> {

  /**
   * Specifies whether a detail model should automatically search by the entity inserted by the master model.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> SEARCH_BY_INSERTED_ENTITY =
          Configuration.booleanValue("is.codion.framework.model.ForeignKeyEntityModelLink.searchByInsertedEntity", false);

  /**
   * Specifies whether a detail model should be automatically refreshed when the selection in the master model changes.
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> REFRESH_ON_SELECTION =
          Configuration.booleanValue("is.codion.framework.model.ForeignKeyEntityModelLink.refreshOnSelection", true);

  /**
   * Specifies whether a detail model sets the master foreign key to null when null or no value is selected in a master model<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION =
          Configuration.booleanValue("is.codion.framework.model.ForeignKeyEntityModelLink.clearForeignKeyOnEmptySelection", false);

  /**
   * @return the foreign key this model link is based on
   */
  ForeignKey foreignKey();

  /**
   * @return true if this models table model should automatically search by the inserted entity
   * when an insert is performed in a master model
   * @see ForeignKeyEntityModelLink#SEARCH_BY_INSERTED_ENTITY
   */
  boolean isSearchByInsertedEntity();

  /**
   * @param searchOnMasterInsert if true then this models table model will automatically search by the inserted entity
   * when an insert is performed in a master model
   * @return this {@link EntityModelLink} instance
   * @see ForeignKeyEntityModelLink#SEARCH_BY_INSERTED_ENTITY
   */
  ForeignKeyEntityModelLink<M, E, T> setSearchByInsertedEntity(boolean searchOnMasterInsert);

  /**
   * @return true if this model table model should be automatically refreshed
   * when the foreign key condition is set according to the master model selection
   * @see ForeignKeyEntityModelLink#REFRESH_ON_SELECTION
   */
  boolean isRefreshOnSelection();

  /**
   * @param refreshOnSelection if true then this models table model will be automatically refreshed
   * when the foreign key condition is set according to the master model selection
   * @return this {@link EntityModelLink} instance
   * @see ForeignKeyEntityModelLink#REFRESH_ON_SELECTION
   */
  ForeignKeyEntityModelLink<M, E, T> setRefreshOnSelection(boolean refreshOnSelection);

  /**
   * Returns true if this model link should set the foreign key to null when null or no value is selected in the master model.
   * @return true if a null selection should result in the foreign key being set to null
   * @see ForeignKeyEntityModelLink#CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION
   */
  boolean isClearForeignKeyOnEmptySelection();

  /**
   * Set to true if this model link should set the foreign key to null when null or no value is selected in the master model.
   * @param clearForeignKeyOnEmptySelection true if initialization with a null value should set the foreign key to null
   * @return this {@link EntityModelLink} instance
   * @see ForeignKeyEntityModelLink#CLEAR_FOREIGN_KEY_ON_EMPTY_SELECTION
   */
  ForeignKeyEntityModelLink<M, E, T> setClearForeignKeyOnEmptySelection(boolean clearForeignKeyOnEmptySelection);
}
