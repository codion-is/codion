package org.jminor.framework.client.model;

import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.Collections;
import java.util.List;

public class EntityTableColumnModel extends DefaultTableColumnModel {

  private final String entityID;
  private final List<Property> columnProperties;

  public EntityTableColumnModel(final String entityID) {
    this(entityID, EntityRepository.getVisibleProperties(entityID));
  }

  public EntityTableColumnModel(final String entityID, final List<Property> columnProperties) {
    this.entityID = entityID;
    this.columnProperties = Collections.unmodifiableList(columnProperties);
    initializeColumns();
  }

  public String getEntityID() {
    return entityID;
  }

  public List<Property> getColumnProperties() {
    return columnProperties;
  }

  protected void initializeColumns() {
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
