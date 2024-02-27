/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.generator;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.tools.metadata.MetaDataColumn;
import is.codion.swing.framework.model.tools.metadata.MetaDataForeignKeyConstraint;
import is.codion.swing.framework.model.tools.metadata.MetaDataSchema;
import is.codion.swing.framework.model.tools.metadata.MetaDataTable;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.attribute.ForeignKey.reference;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DatabaseDomain extends DefaultDomain {

  private static final int MAXIMUM_COLUMN_SIZE = 2_147_483_647;

  private final Map<MetaDataTable, EntityType> tableEntityTypes = new HashMap<>();

  DatabaseDomain(MetaDataSchema schema) {
    super(domainType(schema.name()));
    setStrictForeignKeys(false);
    schema.tables().values().forEach(this::defineEntity);
  }

  String tableType(EntityType entityType) {
    return tableEntityTypes.entrySet().stream()
            .filter(entry -> entry.getValue().equals(entityType))
            .findFirst()
            .map(Map.Entry::getKey)
            .map(MetaDataTable::tableType)
            .orElseThrow(IllegalArgumentException::new);
  }

  private void defineEntity(MetaDataTable table) {
    if (!tableEntityTypes.containsKey(table)) {
      EntityType entityType = type().entityType(table.schema().name() + "." + table.tableName());
      tableEntityTypes.put(table, entityType);
      table.foreignKeys().stream()
              .map(MetaDataForeignKeyConstraint::referencedTable)
              .filter(referencedTable -> !referencedTable.equals(table))
              .forEach(this::defineEntity);
      defineEntity(table, entityType);
    }
  }

  private void defineEntity(MetaDataTable table, EntityType entityType) {
    List<AttributeDefinition.Builder<?, ?>> attributeDefinitionBuilders = defineAttributes(table, entityType, new ArrayList<>(table.foreignKeys()));
    if (!attributeDefinitionBuilders.isEmpty()) {
      EntityDefinition.Builder entityDefinitionBuilder = entityType.define(attributeDefinitionBuilders.toArray(new AttributeDefinition.Builder[0]));
      entityDefinitionBuilder.caption(caption(table.tableName()));
      if (tableHasAutoIncrementPrimaryKeyColumn(table)) {
        entityDefinitionBuilder.keyGenerator(KeyGenerator.identity());
      }
      if (!nullOrEmpty(table.comment())) {
        entityDefinitionBuilder.description(table.comment());
      }
      entityDefinitionBuilder.readOnly("view".equalsIgnoreCase(table.tableType()));
      add(entityDefinitionBuilder);
    }
  }

  private List<AttributeDefinition.Builder<?, ?>> defineAttributes(MetaDataTable table, EntityType entityType,
                                                                   Collection<MetaDataForeignKeyConstraint> foreignKeyConstraints) {
    List<AttributeDefinition.Builder<?, ?>> builders = new ArrayList<>();
    table.columns().forEach(column -> {
      builders.add(columnDefinitionBuilder(column, entityType));
      if (column.foreignKeyColumn()) {
        foreignKeyConstraints.stream()
                //if this is the last column in the foreign key
                .filter(foreignKeyConstraint -> lastKeyColumn(foreignKeyConstraint, column))
                .findFirst()
                .ifPresent(foreignKeyConstraint -> {
                  //we add the foreign key just below it
                  foreignKeyConstraints.remove(foreignKeyConstraint);
                  builders.add(foreignKeyDefinitionBuilder(foreignKeyConstraint, entityType));
                });
      }
    });

    return builders;
  }

  private AttributeDefinition.Builder<?, ?> foreignKeyDefinitionBuilder(MetaDataForeignKeyConstraint foreignKeyConstraint, EntityType entityType) {
    MetaDataTable referencedTable = foreignKeyConstraint.referencedTable();
    EntityType referencedEntityType = tableEntityTypes.get(referencedTable);
    ForeignKey foreignKey = entityType.foreignKey(createForeignKeyName(foreignKeyConstraint) + "_FK",
            foreignKeyConstraint.references().entrySet().stream()
                    .map(entry -> reference(column(entityType, entry.getKey()), column(referencedEntityType, entry.getValue())))
                    .collect(toList()));

    return foreignKey.define().foreignKey().caption(caption(referencedTable.tableName().toLowerCase()));
  }

  private static ColumnDefinition.Builder<?, ?> columnDefinitionBuilder(MetaDataColumn metadataColumn, EntityType entityType) {
    String caption = caption(metadataColumn.columnName());
    Column<?> column = column(entityType, metadataColumn);
    ColumnDefinition.Builder<?, ?> builder;
    if (metadataColumn.primaryKeyColumn()) {
      builder = column.define().primaryKey(metadataColumn.primaryKeyIndex() - 1);
    }
    else {
      builder = column.define().column().caption(caption);
    }
    if (!metadataColumn.primaryKeyColumn() && metadataColumn.nullable() == DatabaseMetaData.columnNoNulls) {
      builder.nullable(false);
    }
    if (column.type().isString() && metadataColumn.columnSize() > 0 && metadataColumn.columnSize() < MAXIMUM_COLUMN_SIZE) {
      builder.maximumLength(metadataColumn.columnSize());
    }
    if (column.type().isDecimal() && metadataColumn.decimalDigits() >= 1) {
      builder.maximumFractionDigits(metadataColumn.decimalDigits());
    }
    if (!metadataColumn.primaryKeyColumn() && metadataColumn.defaultValue() != null) {
      builder.columnHasDefaultValue(true);
    }
    if (!nullOrEmpty(metadataColumn.comment())) {
      builder.description(metadataColumn.comment());
    }

    return builder;
  }

  private static <T> Column<T> column(EntityType entityType, MetaDataColumn column) {
    return (Column<T>) entityType.column(column.columnName(), column.columnClass());
  }

  private static String caption(String name) {
    String caption = name.toLowerCase().replace("_", " ");

    return caption.substring(0, 1).toUpperCase() + caption.substring(1);
  }

  private static boolean lastKeyColumn(MetaDataForeignKeyConstraint foreignKeyConstraint, MetaDataColumn column) {
    return foreignKeyConstraint.references().keySet().stream()
            .mapToInt(MetaDataColumn::position)
            .max()
            .orElse(-1) == column.position();
  }

  private static String createForeignKeyName(MetaDataForeignKeyConstraint foreignKeyConstraint) {
    return foreignKeyConstraint.references().keySet().stream()
            .map(MetaDataColumn::columnName)
            .map(String::toUpperCase)
            .collect(joining("_"));
  }

  private static boolean tableHasAutoIncrementPrimaryKeyColumn(MetaDataTable table) {
    return table.columns().stream()
            .filter(MetaDataColumn::primaryKeyColumn)
            .anyMatch(MetaDataColumn::autoIncrement);
  }
}
