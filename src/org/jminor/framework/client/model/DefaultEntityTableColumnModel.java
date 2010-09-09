/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.Collections;
import java.util.List;

/**
 * A default EntityTableColumnModel implementation.
 */
public class DefaultEntityTableColumnModel extends DefaultTableColumnModel implements EntityTableColumnModel {

  private final String entityID;
  private final List<Property> columnProperties;

  /**
   * Instantiates a new DefaultEntityTableColumnModel, using all the visible properties defined for the given entity.
   * @param entityID the entity ID
   */
  public DefaultEntityTableColumnModel(final String entityID) {
    this(entityID, Entities.getVisibleProperties(entityID));
  }

  /**
   * Instantiates a new DefaultEntityTableColumnModel.
   * @param entityID the entity ID
   * @param columnProperties the properties to base this column model on
   */
  public DefaultEntityTableColumnModel(final String entityID, final List<Property> columnProperties) {
    this.entityID = entityID;
    this.columnProperties = Collections.unmodifiableList(columnProperties);
    initializeColumns();
  }

  /** {@inheritDoc} */
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  public final List<Property> getColumnProperties() {
    return columnProperties;
  }

  private void initializeColumns() {
    int modelIndex = 0;
    for (final Property property : columnProperties) {
      final TableColumn column = new TableColumn(modelIndex++);
      column.setIdentifier(property);
      column.setHeaderValue(property.getCaption());
      if (property.getPreferredColumnWidth() > 0) {
        column.setPreferredWidth(property.getPreferredColumnWidth());
      }
      addColumn(column);
    }
  }
}
