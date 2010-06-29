/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.reports.ReportException;
import org.jminor.common.model.reports.ReportResult;
import org.jminor.common.model.reports.ReportWrapper;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;

import java.util.List;
import java.util.Map;

public interface EntityModel extends Refreshable {

  Event eventRefreshStarted();

  Event eventRefreshDone();

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
   * @return true if this EntityModel contains a TableModel
   */
  boolean containsTableModel();

  /**
   * Sets the currently linked detail model, that is, the one that should be
   * updated according to the selected item
   * @param entityModel the detail model to link
   */
  void setLinkedDetailModel(final EntityModel entityModel);

  /**
   * Returns an initialized ReportResult object from the given report wrapper
   * @param reportWrapper the report wrapper
   * @param reportParameters the report parameters
   * @return an initialized ReportResult object
   * @throws ReportException in case of a report exception
   */
  ReportResult fillReport(final ReportWrapper reportWrapper, final Map<String, Object> reportParameters) throws ReportException;

  /**
   * Returns an initialized ReportResult object from the given report wrapper and data wrapper
   * @param reportWrapper the report wrapper
   * @param dataSource the ReportDataWrapper used to provide the report data
   * @param reportParameters the report parameters
   * @return an initialized ReportResult object
   * @throws ReportException in case of a report exception
   */
  ReportResult fillReport(final ReportWrapper reportWrapper, final ReportDataWrapper dataSource, final Map<String, Object> reportParameters) throws ReportException;

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
   * Returns the first detail model of the given type
   * @param modelClass the type of the required EntityModel
   * @return the detail model of type <code>entityModelClass</code>, null if none is found
   */
  EntityModel getDetailModel(final Class<? extends EntityModel> modelClass);

  /**
   * @return the detail models this model contains
   */
  List<? extends EntityModel> getDetailModels();
}
