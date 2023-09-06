/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
    AtomicInteger index = new AtomicInteger();
    return entityDefinition.attributeDefinitions().stream()
            .filter(attributeDefinition -> !attributeDefinition.isHidden())
            .map(attributeDefinition -> createColumn(attributeDefinition, index.getAndIncrement()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
  }

  /**
   * Creates a column for the given attribute.
   * @param attributeDefinition the attribute definition
   * @param modelIndex the column model index
   * @return the column or an empty Optional in case no column should be created for the given attribute
   */
  protected Optional<FilteredTableColumn<Attribute<?>>> createColumn(AttributeDefinition<?> attributeDefinition, int modelIndex) {
    FilteredTableColumn.Builder<? extends Attribute<?>> columnBuilder =
            FilteredTableColumn.builder(attributeDefinition.attribute(), modelIndex)
                    .headerValue(attributeDefinition.caption())
                    .columnClass(attributeDefinition.attribute().valueClass())
                    .comparator(attributeComparator(attributeDefinition.attribute()));

    return Optional.of((FilteredTableColumn<Attribute<?>>) columnBuilder.build());
  }

  /**
   * Returns a comparator for the given attribute.
   * @param attribute the attribute
   * @return the comparator
   */
  protected final Comparator<?> attributeComparator(Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      return entityDefinition.referencedEntity((ForeignKey) attribute).comparator();
    }

    return entityDefinition.attributeDefinition(attribute).comparator();
  }
}
