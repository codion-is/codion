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
  public List<FilteredTableColumn<Attribute<?>>> createColumns() {
    List<FilteredTableColumn<Attribute<?>>> columns = new ArrayList<>(entityDefinition.visibleProperties().size());
    for (Property<?> property : entityDefinition.visibleProperties()) {
      FilteredTableColumn.Builder<? extends Attribute<?>> columnBuilder =
              FilteredTableColumn.builder(columns.size(), property.attribute())
                      .headerValue(property.caption())
                      .columnClass(property.attribute().valueClass())
                      .comparator(attributeComparator(entityDefinition, property.attribute()));
      columns.add((FilteredTableColumn<Attribute<?>>) columnBuilder.build());
    }

    return columns;
  }

  protected static Comparator<?> attributeComparator(EntityDefinition definition, Attribute<?> attribute) {
    if (attribute instanceof ForeignKey) {
      return definition.referencedDefinition((ForeignKey) attribute).comparator();
    }

    return definition.property(attribute).comparator();
  }
}
