/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.common.model.table.AbstractTableSortModel;

import java.util.Comparator;

import static java.util.Objects.requireNonNull;

/**
 * A default sort model implementation based on Entity
 */
public class SwingEntityTableSortModel extends AbstractTableSortModel<Entity, Attribute<?>> {

  private final Entities entities;

  /**
   * Instantiates a new {@link SwingEntityTableSortModel}
   * @param entities the domain entities
   */
  public SwingEntityTableSortModel(final Entities entities) {
    this.entities = requireNonNull(entities, "entities");
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
  protected Comparable<?> getComparable(final Entity row, final Attribute<?> attribute) {
    return (Comparable<?>) row.get(attribute);
  }
}
