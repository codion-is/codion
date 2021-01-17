/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.table.AbstractTableSortModel;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A default sort model implementation based on Entity
 */
public class SwingEntityTableSortModel extends AbstractTableSortModel<Entity, Attribute<?>> {

  private final Entities entities;

  /**
   * Instantiates a new DefaultEntityTableSortModel
   * @param entities the domain model entities
   * @param entityType the entityType
   */
  public SwingEntityTableSortModel(final Entities entities, final EntityType<?> entityType) {
    super(initializeColumns(entities.getDefinition(entityType).getVisibleProperties()));
    this.entities = entities;
  }

  @Override
  public final Class<?> getColumnClass(final Attribute<?> attribute) {
    return attribute.getTypeClass();
  }

  @Override
  protected Comparator<?> initializeColumnComparator(final Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      return entities.getDefinition(((ForeignKey) attribute).getReferencedEntityType()).getComparator();
    }

    return super.initializeColumnComparator(attribute);
  }

  @Override
  protected final Comparable<?> getComparable(final Entity row, final Attribute<?> attribute) {
    return (Comparable<?>) row.get(attribute);
  }

  private static List<TableColumn> initializeColumns(final List<Property<?>> visibleProperties) {
    final List<TableColumn> columns = new ArrayList<>(visibleProperties.size());
    for (final Property<?> property : visibleProperties) {
      final TableColumn column = new TableColumn(columns.size());
      column.setIdentifier(property.getAttribute());
      column.setHeaderValue(property.getCaption());
      if (property.getPreferredColumnWidth() > 0) {
        column.setPreferredWidth(property.getPreferredColumnWidth());
      }
      columns.add(column);
    }

    return columns;
  }
}
