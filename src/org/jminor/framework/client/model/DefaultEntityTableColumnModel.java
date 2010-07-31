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

public class DefaultEntityTableColumnModel extends DefaultTableColumnModel implements EntityTableColumnModel {

  private final String entityID;
  private final List<Property> columnProperties;

  public DefaultEntityTableColumnModel(final String entityID) {
    this(entityID, Entities.getVisibleProperties(entityID));
  }

  public DefaultEntityTableColumnModel(final String entityID, final List<Property> columnProperties) {
    this.entityID = entityID;
    this.columnProperties = Collections.unmodifiableList(columnProperties);
    initializeColumns();
  }

  public final String getEntityID() {
    return entityID;
  }

  public final List<Property> getColumnProperties() {
    return columnProperties;
  }

  private void initializeColumns() {
    int i = 0;
    for (final Property property : columnProperties) {
      final TableColumn column = new TableColumn(i++);
      column.setIdentifier(property);
      column.setHeaderValue(property.getCaption());
      if (property.getPreferredColumnWidth() > 0) {
        column.setPreferredWidth(property.getPreferredColumnWidth());
      }
      addColumn(column);
    }
  }
}
