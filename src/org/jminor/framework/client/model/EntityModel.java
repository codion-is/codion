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
 * Specifies a class responsible for, among other things, coordinating a EntityEditModel and an EntityTableModel.
 */
public interface EntityModel extends Refreshable, EntityDataProvider {

  /**
   * @return the EntityEditModel instance used by this EntityModel
   */
  EntityEditModel getEditModel();

  /**
   * @return the EntityTableModel, null if none is specified
   */
  EntityTableModel getTableModel();

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
   * Initializes this EntityModel according to the given master entities,
   * sets the appropriate property value and filters the EntityTableModel
   * @param masterEntityID the ID of the master entity
   * @param selectedMasterEntities the master entities
   */
  void initialize(final String masterEntityID, final List<Entity> selectedMasterEntities);

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
   * Returns the first detail model of the given type, this method does not
   * automatically create an entity model if none is available
   * @param modelClass the type of the required EntityModel
   * @return the detail model of type <code>entityModelClass</code>, null if none is found
   */
  EntityModel getDetailModel(final Class<? extends EntityModel> modelClass);

  /**
   * Returns a detail model of the given type, automatically creates a
   * default entity model if none is available and auto creation is turned on
   * @param entityID the entity ID of the required EntityModel
   * @return the detail model of type <code>entityModelClass</code>
   * @see org.jminor.framework.Configuration#AUTO_CREATE_ENTITY_MODELS
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
