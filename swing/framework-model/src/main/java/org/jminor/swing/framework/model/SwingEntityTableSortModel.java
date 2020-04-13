/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.swing.common.model.table.AbstractTableSortModel;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A default sort model implementation based on Entity
 */
public class SwingEntityTableSortModel extends AbstractTableSortModel<Entity, Property> {

  private final EntityDefinition.Provider definitionProvider;

  /**
   * Instantiates a new DefaultEntityTableSortModel
   * @param definitionProvider the domain entity definition provider
   * @param entityId the entity ID
   */
  public SwingEntityTableSortModel(final EntityDefinition.Provider definitionProvider, final String entityId) {
    super(initializeColumns(definitionProvider.getDefinition(entityId).getVisibleProperties()));
    this.definitionProvider = definitionProvider;
  }

  @Override
  public final Class getColumnClass(final Property property) {
    return property.getTypeClass();
  }

  @Override
  protected Comparator initializeColumnComparator(final Property property) {
    if (property instanceof ForeignKeyProperty) {
      return definitionProvider.getDefinition(((ForeignKeyProperty) property).getForeignEntityId()).getComparator();
    }

    return super.initializeColumnComparator(property);
  }

  @Override
  protected final Comparable getComparable(final Entity row, final Property property) {
    return (Comparable) row.get(property);
  }

  private static List<TableColumn> initializeColumns(final List<Property> visibleProperties) {
    final List<TableColumn> columns = new ArrayList<>(visibleProperties.size());
    for (final Property property : visibleProperties) {
      final TableColumn column = new TableColumn(columns.size());
      column.setIdentifier(property);
      column.setHeaderValue(property.getCaption());
      if (property.getPreferredColumnWidth() > 0) {
        column.setPreferredWidth(property.getPreferredColumnWidth());
      }
      columns.add(column);
    }

    return columns;
  }
}
