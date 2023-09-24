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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.explorer;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
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
import static is.codion.framework.domain.entity.attribute.ForeignKey.reference;
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
    List<AttributeDefinition.Builder<?, ?>> definitionBuilders = definitionBuilders(table, entityType, new ArrayList<>(table.foreignKeys()));
    if (!definitionBuilders.isEmpty()) {
      EntityDefinition.Builder definitionBuilder = entityType.define(definitionBuilders.toArray(new AttributeDefinition.Builder[0]));
      if (tableHasAutoIncrementPrimaryKeyColumn(table)) {
        definitionBuilder.keyGenerator(KeyGenerator.identity());
      }
      if (!nullOrEmpty(table.comment())) {
        definitionBuilder.description(table.comment());
      }
      add(definitionBuilder);
    }
  }

  private List<AttributeDefinition.Builder<?, ?>> definitionBuilders(Table table, EntityType entityType,
                                                                     Collection<ForeignKeyConstraint> foreignKeyConstraints) {
    List<AttributeDefinition.Builder<?, ?>> builders = new ArrayList<>();
    table.columns().forEach(column -> {
      builders.add(columnDefinitionBuilder(column, entityType));
      if (column.isForeignKeyColumn()) {
        foreignKeyConstraints.stream()
                //if this is the last column in the foreign key
                .filter(foreignKeyConstraint -> isLastKeyColumn(foreignKeyConstraint, column))
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

  private AttributeDefinition.Builder<?, ?> foreignKeyDefinitionBuilder(ForeignKeyConstraint foreignKeyConstraint, EntityType entityType) {
    Table referencedTable = foreignKeyConstraint.referencedTable();
    EntityType referencedEntityType = tableEntityTypes.get(referencedTable);
    ForeignKey foreignKey = entityType.foreignKey(createForeignKeyName(foreignKeyConstraint) + "_FK",
            foreignKeyConstraint.references().entrySet().stream()
                    .map(entry -> reference(column(entityType, entry.getKey()), column(referencedEntityType, entry.getValue())))
                    .collect(toList()));

    return foreignKey.define().foreignKey().caption(caption(referencedTable.tableName().toLowerCase()));
  }

  private static ColumnDefinition.Builder<?, ?> columnDefinitionBuilder(MetadataColumn metadataColumn, EntityType entityType) {
    String caption = caption(metadataColumn.columnName());
    Column<?> column = column(entityType, metadataColumn);
    ColumnDefinition.Builder<?, ?> builder;
    if (column.type().isByteArray()) {
      builder = column.define().blobColumn().caption(caption);
    }
    else {
      builder = column.define().column().caption(caption);
    }
    if (metadataColumn.isPrimaryKeyColumn()) {
      builder.primaryKeyIndex(metadataColumn.primaryKeyIndex() - 1);
    }
    if (!metadataColumn.isPrimaryKeyColumn() && metadataColumn.nullable() == DatabaseMetaData.columnNoNulls) {
      builder.nullable(false);
    }
    if (column.type().isString() && metadataColumn.columnSize() > 0) {
      builder.maximumLength(metadataColumn.columnSize());
    }
    if (column.type().isDecimal() && metadataColumn.decimalDigits() >= 1) {
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
