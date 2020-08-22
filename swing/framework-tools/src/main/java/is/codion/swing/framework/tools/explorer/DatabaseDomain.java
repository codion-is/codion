/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.swing.framework.tools.metadata.Column;
import is.codion.swing.framework.tools.metadata.ForeignKey;
import is.codion.swing.framework.tools.metadata.Table;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.Util.nullOrEmpty;

final class DatabaseDomain extends DefaultDomain {

  private final Map<Table, EntityType<Entity>> tableEntityTypes = new HashMap<>();

  DatabaseDomain(final DomainType domainType, final Collection<Table> tables) {
    super(domainType);
    tables.forEach(this::defineEntity);
  }

  private void define(final EntityType<?> entityType, final List<Property.Builder<?>> propertyBuilders) {
    if (!propertyBuilders.isEmpty()) {
      define(entityType, entityType.getName(), propertyBuilders.toArray(new Property.Builder[0]));
    }
  }

  private void defineEntity(final Table table) {
    if (!tableEntityTypes.containsKey(table)) {
      final EntityType<Entity> entityType = getDomainType().entityType(table.getSchema().getName() + "." + table.getTableName());
      tableEntityTypes.put(table, entityType);
      define(entityType, getPropertyBuilders(table, entityType, new ArrayList<>(table.getForeignKeys())));
    }
  }

  private List<Property.Builder<?>> getPropertyBuilders(final Table table, final EntityType<Entity> entityType,
                                                        final List<ForeignKey> foreignKeys) {
    final List<Property.Builder<?>> builders = new ArrayList<>();
    table.getColumns().forEach(column -> {
      builders.add(getColumnPropertyBuilder(column, entityType));
      if (column.isForeignKeyColumn()) {
        final ForeignKey foreignKey = foreignKeys.stream().filter(key ->
                key.getReferences().keySet().iterator().next().equals(column)).findFirst().orElse(null);
        if (foreignKey != null) {
          foreignKeys.remove(foreignKey);
          if (!foreignKey.getReferencedTable().equals(table)) {
            defineEntity(foreignKey.getReferencedTable());
          }
          builders.add(getForeignKeyPropertyBuilder(foreignKey, entityType));
        }
      }
    });

    return builders;
  }

  private Property.Builder<?> getForeignKeyPropertyBuilder(final ForeignKey foreignKey, final EntityType<?> entityType) {
    final Table referencedTable = foreignKey.getReferencedTable();
    //todo foreign keys to a table of the same name in different schemas, attribute name clash
    final Attribute<Entity> attribute = entityType.entityAttribute(referencedTable.getTableName() + "_FK");
    final String caption = getCaption(referencedTable.getTableName());

    final EntityType<?> referencedEntityType = tableEntityTypes.get(referencedTable);
    final ForeignKeyProperty.Builder builder = Properties.foreignKeyProperty(attribute, caption);
    foreignKey.getReferences().forEach((column, referencedColumn) ->
            builder.reference(getAttribute(entityType, column), getAttribute(referencedEntityType, referencedColumn)));

    return builder;
  }

  private static ColumnProperty.Builder<?> getColumnPropertyBuilder(final Column column, final EntityType<?> entityType) {
    final String caption = getCaption(column.getColumnName());
    final Attribute<?> attribute = getAttribute(entityType, column);
    final ColumnProperty.Builder<?> builder;
    if (attribute.isByteArray()) {
      builder = Properties.blobProperty((Attribute<byte[]>) attribute, caption).eagerlyLoaded(false);
    }
    else {
      builder = Properties.columnProperty(attribute, caption);
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
      builder.columnHasDefaultValue(true);
    }
    if (!nullOrEmpty(column.getComment())) {
      builder.description(column.getComment());
    }

    return builder;
  }

  private static <T> Attribute<T> getAttribute(final EntityType<?> entityType, final Column column) {
    return (Attribute<T>) entityType.attribute(column.getColumnName(), column.getColumnTypeClass());
  }

  private static String getCaption(final String name) {
    final String caption = name.toLowerCase().replaceAll("_", " ");

    return caption.substring(0, 1).toUpperCase() + caption.substring(1);
  }
}
