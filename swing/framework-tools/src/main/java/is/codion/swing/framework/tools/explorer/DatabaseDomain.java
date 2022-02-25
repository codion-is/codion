/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;
import is.codion.swing.framework.tools.metadata.Column;
import is.codion.swing.framework.tools.metadata.ForeignKeyConstraint;
import is.codion.swing.framework.tools.metadata.Table;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.framework.domain.entity.ForeignKey.reference;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DatabaseDomain extends DefaultDomain {

  private final Map<Table, EntityType> tableEntityTypes = new HashMap<>();

  DatabaseDomain(DomainType domainType, Collection<Table> tables) {
    super(domainType);
    tables.forEach(this::defineEntity);
  }

  private void defineEntity(Table table) {
    if (!tableEntityTypes.containsKey(table)) {
      EntityType entityType = getDomainType().entityType(table.getSchema().getName() + "." + table.getTableName());
      tableEntityTypes.put(table, entityType);
      table.getForeignKeys().stream()
              .map(ForeignKeyConstraint::getReferencedTable)
              .filter(referencedTable -> !referencedTable.equals(table))
              .forEach(this::defineEntity);
      define(entityType, getPropertyBuilders(table, entityType, new ArrayList<>(table.getForeignKeys())));
    }
  }

  private void define(EntityType entityType, List<Property.Builder<?, ?>> propertyBuilders) {
    if (!propertyBuilders.isEmpty()) {
      define(entityType, entityType.getName(), propertyBuilders.toArray(new Property.Builder[0]));
    }
  }

  private List<Property.Builder<?, ?>> getPropertyBuilders(Table table, EntityType entityType,
                                                           List<ForeignKeyConstraint> foreignKeyConstraints) {
    List<Property.Builder<?, ?>> builders = new ArrayList<>();
    table.getColumns().forEach(column -> {
      builders.add(getColumnPropertyBuilder(column, entityType));
      if (column.isForeignKeyColumn()) {
        foreignKeyConstraints.stream()
                .filter(key -> isLastKeyColumn(key, column))
                .findFirst()
                .ifPresent(foreignKeyConstraint -> {
                  foreignKeyConstraints.remove(foreignKeyConstraint);
                  builders.add(getForeignKeyPropertyBuilder(foreignKeyConstraint, entityType));
                });
      }
    });

    return builders;
  }

  private Property.Builder<?, ?> getForeignKeyPropertyBuilder(ForeignKeyConstraint foreignKeyConstraint, EntityType entityType) {
    Table referencedTable = foreignKeyConstraint.getReferencedTable();
    EntityType referencedEntityType = tableEntityTypes.get(referencedTable);
    ForeignKey foreignKey = entityType.foreignKey(createForeignKeyName(foreignKeyConstraint) + "_FK",
            foreignKeyConstraint.getReferences().entrySet().stream()
                    .map(entry -> reference(getAttribute(entityType, entry.getKey()), getAttribute(referencedEntityType, entry.getValue())))
                    .collect(toList()));

    return foreignKeyProperty(foreignKey, getCaption(referencedTable.getTableName()));
  }

  private static ColumnProperty.Builder<?, ?> getColumnPropertyBuilder(Column column, EntityType entityType) {
    String caption = getCaption(column.getColumnName());
    Attribute<?> attribute = getAttribute(entityType, column);
    ColumnProperty.Builder<?, ?> builder;
    if (attribute.isByteArray()) {
      builder = blobProperty((Attribute<byte[]>) attribute, caption);
    }
    else {
      builder = columnProperty(attribute, caption);
    }
    if (column.isPrimaryKeyColumn()) {
      builder.primaryKeyIndex(column.getPrimaryKeyIndex() - 1);
    }
    if (!column.isPrimaryKeyColumn() && column.getNullable() == DatabaseMetaData.columnNoNulls) {
      builder.nullable(false);
    }
    if (attribute.isString() && column.getColumnSize() > 0) {
      builder.maximumLength(column.getColumnSize());
    }
    if (attribute.isDecimal() && column.getDecimalDigits() >= 1) {
      builder.maximumFractionDigits(column.getDecimalDigits());
    }
    if (!column.isPrimaryKeyColumn() && column.hasDefaultValue()) {
      builder.columnHasDefaultValue();
    }
    if (!nullOrEmpty(column.getComment())) {
      builder.description(column.getComment());
    }

    return builder;
  }

  private static <T> Attribute<T> getAttribute(EntityType entityType, Column column) {
    return (Attribute<T>) entityType.attribute(column.getColumnName(), column.getColumnTypeClass());
  }

  private static String getCaption(String name) {
    String caption = name.toLowerCase().replace("_", " ");

    return caption.substring(0, 1).toUpperCase() + caption.substring(1);
  }

  private static boolean isLastKeyColumn(ForeignKeyConstraint foreignKeyConstraint, Column column) {
    OptionalInt lastColumnPosition = foreignKeyConstraint.getReferences().keySet().stream()
            .mapToInt(Column::getPosition)
            .max();

    return lastColumnPosition.isPresent() && column.getPosition() == lastColumnPosition.getAsInt();
  }

  private static String createForeignKeyName(ForeignKeyConstraint foreignKeyConstraint) {
    return foreignKeyConstraint.getReferences().keySet().stream()
            .map(Column::getColumnName)
            .map(String::toUpperCase)
            .collect(joining("_"));
  }
}
