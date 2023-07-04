/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Provides table columns based on an entity definition.
 */
public class SwingEntityColumnFactory implements ColumnFactory<Attribute<?>> {

  private final EntityDefinition entityDefinition;

  /**
   * Instantiates a new SwingEntityColumnFactory
   * @param entityDefinition the entity definition
   */
  public SwingEntityColumnFactory(EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
  }

  @Override
  public final List<FilteredTableColumn<Attribute<?>>> createColumns() {
    List<FilteredTableColumn<Attribute<?>>> columns = new ArrayList<>(entityDefinition.visibleProperties().size());
    for (Property<?> property : entityDefinition.visibleProperties()) {
      createColumn(property, columns.size()).ifPresent(columns::add);
    }

    return columns;
  }

  /**
   * Creates a column for the given property.
   * @param property the property
   * @param modelIndex the column model index
   * @return the column or an empty Optional in case no column should be created for the given property
   */
  protected Optional<FilteredTableColumn<Attribute<?>>> createColumn(Property<?> property, int modelIndex) {
    FilteredTableColumn.Builder<? extends Attribute<?>> columnBuilder =
            FilteredTableColumn.builder(property.attribute(), modelIndex)
                    .headerValue(property.caption())
                    .columnClass(property.attribute().valueClass())
                    .comparator(attributeComparator(property.attribute()));

    return Optional.of((FilteredTableColumn<Attribute<?>>) columnBuilder.build());
  }

  /**
   * Returns a comparator for the given attribute.
   * @param attribute the attribute
   * @return the comparator
   */
  protected final Comparator<?> attributeComparator(Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      return entityDefinition.referencedDefinition((ForeignKey) attribute).comparator();
    }

    return entityDefinition.property(attribute).comparator();
  }
}
