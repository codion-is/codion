/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Refreshable;
import org.jminor.framework.domain.Entity;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

/**
 * Specifies a class responsible for, among other things, coordinating a {@link EntityEditModel} and an {@link EntityTableModel}.
 */
public interface EntityModel extends Refreshable, EntityDataProvider {

  /**
   * @return the {@link EntityEditModel} instance used by this {@link EntityModel}
   */
  EntityEditModel getEditModel();

  /**
   * @return the {@link EntityTableModel}, null if none is specified
   */
  EntityTableModel getTableModel();

  /**
   * @return true if this {@link EntityModel} contains a {@link EntityTableModel}
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
   * Initializes this {@link EntityModel} according to the given foreign key entities,
   * sets the appropriate property value in the {@link EntityEditModel} and filters the {@link EntityTableModel}
   * @param foreignKeyEntityID the ID of the master entity
   * @param foreignKeyValues the master entities
   */
  void initialize(final String foreignKeyEntityID, final List<Entity> foreignKeyValues);

  /**
   * Sets the model serving as master model
   * @param entityModel the master entity model
   */
  void setMasterModel(final EntityModel entityModel);

  /**
   * @return the master model, if any
   */
  EntityModel getMasterModel();

  /**
   * Adds the given detail model to this model, using the first foreign key property found
   * with the entityID of this entity model, a side-effect if the detail model contains
   * a table model is that it is configured so that a query criteria is required for it to show
   * any data, via {@link EntityTableModel#setQueryCriteriaRequired(boolean)}
   * @param detailModels the detail models to add
   */
  void addDetailModels(final EntityModel... detailModels);

  /**
   * Adds the given detail model to this model, using the first foreign key property found
   * with the entityID of this entity model, a side-effect if the detail model contains
   * a table model is that it is configured so that a query criteria is required for it to show
   * any data, via {@link EntityTableModel#setQueryCriteriaRequired(boolean)}
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
   * @param detailModel the detail model
   * @return true if this model contains the given detail model
   */
  boolean containsDetailModel(final EntityModel detailModel);

  /**
   * Returns the first detail model of the given type
   * @param modelClass the type of the required {@link EntityModel}
   * @return the detail model of type <code>entityModelClass</code>, null if none is found
   */
  EntityModel getDetailModel(final Class<? extends EntityModel> modelClass);

  /**
   * Returns a detail model of the given type
   * @param entityID the entity ID of the required EntityModel
   * @return the detail model of type <code>entityModelClass</code>
   * @throws IllegalArgumentException in case no detail model for the given entityID is found
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

  /**
   * @param listener a listener to be notified before a refresh is performed
   */
  void addBeforeRefreshListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeBeforeRefreshListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time a refresh has been performed
   */
  void addAfterRefreshListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeAfterRefreshListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time the linked detail models change
   */
  void addLinkedDetailModelsListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeLinkedDetailModelsListener(final ActionListener listener);
}
