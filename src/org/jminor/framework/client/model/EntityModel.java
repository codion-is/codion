/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;

import java.util.Collection;
import java.util.List;

public interface EntityModel extends Refreshable {

  /**
   * @return an Event fired when the model is about to be refreshed
   */
  Event eventRefreshStarted();

  /**
   * @return an Event fired when the model has been refreshed, N.B. this event
   * is fired even if the refresh results in an exception
   */
  Event eventRefreshDone();

  /**
   * @return an Event fired when detail models are linked or unlinked
   */
  Event eventLinkedDetailModelsChanged();

  /**
   * @return the ID of the entity this model represents
   */
  String getEntityID();

  /**
   * @return the database connection provider
   */
  EntityDbProvider getDbProvider();

  /**
   * @return the EntityEditModel instance used by this EntityModel
   */
  EntityEditModel getEditModel();

  /**
   * @return the EntityTableModel, null if none is specified
   */
  EntityTableModel getTableModel();

  /**
   * @return the master model, if any
   */
  EntityModel getMasterModel();

  /**
   * @return true if this EntityModel contains a TableModel
   */
  boolean containsTableModel();

  /**
   * @return an unmodifiable collection containing the detail models that are currently linked to this model
   */
  Collection<EntityModel> getLinkedDetailModels();

  /**
   * Sets the currently linked detail models. Linked models are updated and filtered according
   * to the entity/entities selected in this (the master) model
   * @param detailModels the detail models to link
   */
  void setLinkedDetailModels(final EntityModel... detailModels);

  /**
   * @param value true if a refresh in this model should trigger a refresh in its detail models
   */
  void setCascadeRefresh(final boolean value);

  /**
   * @return true if a refresh on this model should trigger a refresh in its detail models
   */
  boolean isCascadeRefresh();

  /**
   * @return true if the selecting a record in this model should filter the detail models
   */
  boolean isSelectionFiltersDetail();

  /**
   * @param value true if selecting a record in this model should filter the detail models
   * @see #masterSelectionChanged
   * @see #masterSelectionChanged
   * @see DefaultEntityTableModel#searchByForeignKeyValues(String, java.util.List)
   */
  void setSelectionFiltersDetail(final boolean value);

  /**
   * Updates this EntityModel according to the given master entities,
   * sets the appropriate property value and filters the EntityTableModel
   * @param masterEntityID the ID of the master entity
   * @param selectedMasterEntities the master entities
   */
  void masterSelectionChanged(final String masterEntityID, final List<Entity> selectedMasterEntities);

  void setMasterModel(final EntityModel entityModel);

  /**
   * Adds the given detail models to this model.
   * @param detailModels the detail models to add
   */
  void addDetailModels(final EntityModel... detailModels);

  /**
   * Adds the given detail model to this model
   * @param detailModel the detail model
   * @return the detail model just added
   */
  EntityModel addDetailModel(final EntityModel detailModel);

  /**
   * @param modelClass the detail model class
   * @return true if this model contains a detail model of the given class
   */
  boolean containsDetailModel(final Class<? extends EntityModel> modelClass);

  /**
   * @param entityID the entity ID
   * @return true if this model contains a detail model for the given entity ID
   */
  boolean containsDetailModel(final String entityID);

  /**
   * Returns the first detail model of the given type
   * @param modelClass the type of the required EntityModel
   * @return the detail model of type <code>entityModelClass</code>, null if none is found
   * @see org.jminor.framework.Configuration#AUTO_CREATE_ENTITY_MODELS
   */
  EntityModel getDetailModel(final Class<? extends EntityModel> modelClass);

  /**
   * Returns a detail model of the given type
   * @param entityID the entity ID of the required EntityModel
   * @return the detail model of type <code>entityModelClass</code>, null if none is found
   */
  EntityModel getDetailModel(final String entityID);

  /**
   * @return an unmodifiable collection containing the detail models this model contains
   */
  Collection<? extends EntityModel> getDetailModels();

  /**
   * Refreshes the detail models.
   */
  void refreshDetailModels();

  /**
   * Clears the detail models.
   */
  void clearDetailModels();
}
