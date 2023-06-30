/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A {@link FilteredTableModel.ColumnFactory} implementation for {@link EntityDefinition}s.
 */
public class SwingEntityColumnFactory implements FilteredTableModel.ColumnFactory<Attribute<?>> {

  private final EntityDefinition entityDefinition;

  /**
   * Instantiates a new {@link SwingEntityColumnFactory}.
   * @param entityDefinition the entity definition
   */
  public SwingEntityColumnFactory(EntityDefinition entityDefinition) {
    this.entityDefinition = requireNonNull(entityDefinition);
  }

  @Override
  public final List<FilteredTableColumn<Attribute<?>>> createColumns() {
    List<FilteredTableColumn<Attribute<?>>> columns = new ArrayList<>(entityDefinition.visibleProperties().size());
    for (Property<?> property : entityDefinition.visibleProperties()) {
      createColumn(columns.size(), property).ifPresent(columns::add);
    }

    return columns;
  }

  /**
   * Creates a {@link FilteredTableColumn} for the given {@link Property}, with the given model index.
   * @param modelIndex the model index
   * @param property the property
   * @return a {@link FilteredTableColumn} for the given {@link Property} or an empty Optional if the property should not be displayed
   */
  protected Optional<FilteredTableColumn<Attribute<?>>> createColumn(int modelIndex, Property<?> property) {
    FilteredTableColumn.Builder<? extends Attribute<?>> columnBuilder =
              FilteredTableColumn.builder(modelIndex, property.attribute())
                      .headerValue(property.caption())
                      .columnClass(property.attribute().valueClass())
                      .comparator(attributeComparator(property.attribute()));

    return Optional.of((FilteredTableColumn<Attribute<?>>) columnBuilder.build());
  }

  /**
   * @param attribute the attribute
   * @return a Comparator for the given attribute
   */
  protected final Comparator<?> attributeComparator(Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      return entityDefinition.referencedDefinition((ForeignKey) attribute).comparator();
    }

    return entityDefinition.property(attribute).comparator();
  }
}
