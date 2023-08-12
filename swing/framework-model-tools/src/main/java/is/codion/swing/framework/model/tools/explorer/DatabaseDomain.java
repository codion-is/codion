/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.explorer;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;
import is.codion.swing.framework.model.tools.metadata.ForeignKeyConstraint;
import is.codion.swing.framework.model.tools.metadata.MetadataColumn;
import is.codion.swing.framework.model.tools.metadata.Table;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.entity.ForeignKey.reference;
import static is.codion.framework.domain.property.Property.*;
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
      EntityType entityType = type().entityType(table.schema().name() + "." + table.tableName());
      tableEntityTypes.put(table, entityType);
      table.foreignKeys().stream()
              .map(ForeignKeyConstraint::referencedTable)
              .filter(referencedTable -> !referencedTable.equals(table))
              .forEach(this::defineEntity);
      define(table, entityType);
    }
  }

  private void define(Table table, EntityType entityType) {
    List<Builder<?, ?>> propertyBuilders = propertyBuilders(table, entityType, new ArrayList<>(table.foreignKeys()));
    if (!propertyBuilders.isEmpty()) {
      EntityDefinition.Builder definitionBuilder = definition(propertyBuilders.toArray(new Builder[0]));
      if (tableHasAutoIncrementPrimaryKeyColumn(table)) {
        definitionBuilder.keyGenerator(KeyGenerator.identity());
      }
      if (!nullOrEmpty(table.comment())) {
        definitionBuilder.description(table.comment());
      }
      add(definitionBuilder);
    }
  }

  private List<Property.Builder<?, ?>> propertyBuilders(Table table, EntityType entityType,
                                                        Collection<ForeignKeyConstraint> foreignKeyConstraints) {
    List<Property.Builder<?, ?>> builders = new ArrayList<>();
    table.columns().forEach(column -> {
      builders.add(columnPropertyBuilder(column, entityType));
      if (column.isForeignKeyColumn()) {
        foreignKeyConstraints.stream()
                //if this is the last column in the foreign key
                .filter(foreignKeyConstraint -> isLastKeyColumn(foreignKeyConstraint, column))
                .findFirst()
                .ifPresent(foreignKeyConstraint -> {
                  //we add the foreign key property just below it
                  foreignKeyConstraints.remove(foreignKeyConstraint);
                  builders.add(foreignKeyPropertyBuilder(foreignKeyConstraint, entityType));
                });
      }
    });

    return builders;
  }

  private Property.Builder<?, ?> foreignKeyPropertyBuilder(ForeignKeyConstraint foreignKeyConstraint, EntityType entityType) {
    Table referencedTable = foreignKeyConstraint.referencedTable();
    EntityType referencedEntityType = tableEntityTypes.get(referencedTable);
    ForeignKey foreignKey = entityType.foreignKey(createForeignKeyName(foreignKeyConstraint) + "_FK",
            foreignKeyConstraint.references().entrySet().stream()
                    .map(entry -> reference(column(entityType, entry.getKey()), column(referencedEntityType, entry.getValue())))
                    .collect(toList()));

    return foreignKeyProperty(foreignKey, caption(referencedTable.tableName()));
  }

  private static ColumnProperty.Builder<?, ?> columnPropertyBuilder(MetadataColumn metadataColumn, EntityType entityType) {
    String caption = caption(metadataColumn.columnName());
    Column<?> column = column(entityType, metadataColumn);
    ColumnProperty.Builder<?, ?> builder;
    if (column.isByteArray()) {
      builder = blobProperty((Column<byte[]>) column, caption);
    }
    else {
      builder = columnProperty(column, caption);
    }
    if (metadataColumn.isPrimaryKeyColumn()) {
      builder.primaryKeyIndex(metadataColumn.primaryKeyIndex() - 1);
    }
    if (!metadataColumn.isPrimaryKeyColumn() && metadataColumn.nullable() == DatabaseMetaData.columnNoNulls) {
      builder.nullable(false);
    }
    if (column.isString() && metadataColumn.columnSize() > 0) {
      builder.maximumLength(metadataColumn.columnSize());
    }
    if (column.isDecimal() && metadataColumn.decimalDigits() >= 1) {
      builder.maximumFractionDigits(metadataColumn.decimalDigits());
    }
    if (!metadataColumn.isPrimaryKeyColumn() && metadataColumn.defaultValue() != null) {
      builder.columnHasDefaultValue(true);
    }
    if (!nullOrEmpty(metadataColumn.comment())) {
      builder.description(metadataColumn.comment());
    }

    return builder;
  }

  private static <T> Column<T> column(EntityType entityType, MetadataColumn column) {
    return (Column<T>) entityType.column(column.columnName(), column.columnClass());
  }

  private static String caption(String name) {
    String caption = name.toLowerCase().replace("_", " ");

    return caption.substring(0, 1).toUpperCase() + caption.substring(1);
  }

  private static boolean isLastKeyColumn(ForeignKeyConstraint foreignKeyConstraint, MetadataColumn column) {
    return foreignKeyConstraint.references().keySet().stream()
            .mapToInt(MetadataColumn::position)
            .max()
            .orElse(-1) == column.position();
  }

  private static String createForeignKeyName(ForeignKeyConstraint foreignKeyConstraint) {
    return foreignKeyConstraint.references().keySet().stream()
            .map(MetadataColumn::columnName)
            .map(String::toUpperCase)
            .collect(joining("_"));
  }

  private static boolean tableHasAutoIncrementPrimaryKeyColumn(Table table) {
    return table.columns().stream()
            .filter(MetadataColumn::isPrimaryKeyColumn)
            .anyMatch(MetadataColumn::autoIncrement);
  }
}
