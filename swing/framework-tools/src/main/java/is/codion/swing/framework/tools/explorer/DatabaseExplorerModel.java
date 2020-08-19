/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.Text;
import is.codion.common.Util;
import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.common.value.Values;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.model.table.AbstractTableSortModel;
import is.codion.swing.framework.tools.metadata.Column;
import is.codion.swing.framework.tools.metadata.ForeignKey;
import is.codion.swing.framework.tools.metadata.MetaDataModel;
import is.codion.swing.framework.tools.metadata.Schema;
import is.codion.swing.framework.tools.metadata.Table;

import javax.swing.table.TableColumn;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public final class DatabaseExplorerModel {

  private static final String PROPERTIES_COLUMN_PROPERTY = "        columnProperty(";

  private final MetaDataModel metadataModel;
  private final SchemaModel schemaModel;
  private final DomainModel domainModel;
  private final Map<Table, EntityType<Entity>> tableEntityTypes = new HashMap<>();

  private final Map<Schema, MetaDataDomain> domains = new HashMap<>();
  private final Connection connection;

  private final Value<String> domainCodeValue = Values.value();

  public DatabaseExplorerModel(final Database database, final User user) throws DatabaseException {
    this.connection = database.createConnection(user);
    this.schemaModel = new SchemaModel();
    this.domainModel = new DomainModel();
    this.metadataModel = new MetaDataModel(connection);
    bindEvents();
    refreshDomains();
  }

  public AbstractFilteredTableModel<Schema, Integer> getSchemaModel() {
    return schemaModel;
  }

  public AbstractFilteredTableModel<EntityDefinition, Integer> getDefinitionModel() {
    return domainModel;
  }

  public ValueObserver<String> getDomainCodeObserver() {
    return Values.valueObserver(domainCodeValue);
  }

  public void close() {
    Database.closeSilently(connection);
  }

  private MetaDataDomain createDomain(final Schema schema) {
    EntityDefinition.STRICT_FOREIGN_KEYS.set(false);
    final MetaDataDomain domain = new MetaDataDomain(DomainType.domainType(schema.getName()));
    schema.getTables().values().forEach(table -> defineEntity(domain, table));

    return domain;
  }

  private void defineEntity(final MetaDataDomain domain, final Table table) {
    if (!tableEntityTypes.containsKey(table)) {
      final EntityType<Entity> entityType = domain.getDomainType().entityType(table.getSchema().getName() + "." + table.getTableName());
      tableEntityTypes.put(table, entityType);
      final List<Property.Builder<?>> builders = new ArrayList<>();
      table.getColumns().values().forEach(column ->
              builders.add(getColumnPropertyBuilder(column, entityType)));
      table.getForeignKeys().forEach((referencedTable, foreignKey) -> {
        if (!foreignKey.getReferencedTable().equals(table)) {
          defineEntity(domain, foreignKey.getReferencedTable());
        }
      });
      table.getForeignKeys().forEach((referencedTable, foreignKey) ->
              builders.add(getForeignKeyPropertyBuilder(foreignKey, entityType)));
      domain.define(entityType, builders);
    }
  }

  private Property.Builder<?> getForeignKeyPropertyBuilder(final ForeignKey foreignKey, final EntityType<?> entityType) {
    final Column fkColumn = foreignKey.getReferences().keySet().iterator().next();
    final Attribute<Entity> attribute = entityType.entityAttribute(fkColumn.getColumnName() + "_fk");
    final String caption = getCaption(fkColumn);

    final EntityType<?> referencedEntityType = tableEntityTypes.get(foreignKey.getReferencedTable());
    final ForeignKeyProperty.Builder builder = Properties.foreignKeyProperty(attribute, caption);
    foreignKey.getReferences().forEach((column, referencedColumn) ->
            builder.reference(getAttribute(entityType, column), getAttribute(referencedEntityType, referencedColumn)));

    return builder;
  }

  private ColumnProperty.Builder<?> getColumnPropertyBuilder(final Column column, final EntityType<?> entityType) {
    final String caption = getCaption(column);
    final Attribute<?> attribute = getAttribute(entityType, column);
    final ColumnProperty.Builder<?> builder = Properties.columnProperty(attribute, caption);
    if (column.getKeySeq() != -1) {
      builder.primaryKeyIndex(column.getKeySeq() - 1);
    }
    if (column.getKeySeq() == -1 && column.getNullable() == DatabaseMetaData.columnNoNulls) {
      builder.nullable(false);
    }
    if (attribute.isString() && column.getColumnSize() != -1) {
      builder.maximumLength(column.getColumnSize());
    }
    if (attribute.isDecimal() && column.getDecimalDigits() >= 1) {
      builder.maximumFractionDigits(column.getDecimalDigits());
    }
    if (column.hasDefaultValue()) {
      builder.columnHasDefaultValue(true);
    }
    if (!nullOrEmpty(column.getComment())) {
      builder.description(column.getComment());
    }

    return builder;
  }

  private void refreshDomains() {
    tableEntityTypes.clear();
    domains.clear();
    metadataModel.getSchemas().forEach((schemaName, schema) -> domains.put(schema, createDomain(schema)));
    schemaModel.refresh();
  }

  private void bindEvents() {
    schemaModel.getSelectionModel().addSelectionChangedListener(domainModel::refresh);
    domainModel.getSelectionModel().addSelectionChangedListener(this::updateCodeValue);
  }

  private void updateCodeValue() {
    domainCodeValue.set(createDomainCode(domainModel.getSelectionModel().getSelectedItems()));
  }

  private String createDomainCode(final List<EntityDefinition> definitions) {
    final StringBuilder builder = new StringBuilder();
    definitions.forEach(definition -> {
      final String interfaceName = getInterfaceName(definition.getTableName(), true);
      builder.append("public interface ").append(interfaceName).append(" {").append(Util.LINE_SEPARATOR);
      builder.append("  ").append("EntityType<Entity> TYPE = ").append("DOMAIN.entityType(\"")
              .append(definition.getTableName().toLowerCase()).append("\");").append(Util.LINE_SEPARATOR);
      final List<ColumnProperty<?>> columnProperties = definition.getColumnProperties();
      columnProperties.forEach(property -> {
        final String typeClassName = property.getAttribute().getTypeClass().getSimpleName();
        builder.append("  ").append("Attribute<").append(typeClassName).append("> ")
                .append(property.getColumnName().toUpperCase()).append(" = TYPE.").append(getAttributeTypePrefix(typeClassName))
                .append("Attribute(\"").append(property.getColumnName().toLowerCase()).append("\");").append(Util.LINE_SEPARATOR);
      });
      final List<ForeignKeyProperty> foreignKeyProperties = definition.getForeignKeyProperties();
      foreignKeyProperties.forEach(property -> builder.append("  ").append("Attribute<Entity> ")
              .append(property.getAttribute().getName().toUpperCase()).append(" = TYPE.entityAttribute(\"")
              .append(property.getAttribute().getName().toLowerCase()).append("\");").append(Util.LINE_SEPARATOR));

      builder.append("}").append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);

      builder.append("void ").append(getInterfaceName(definition.getTableName(), false)).append("() {").append(Util.LINE_SEPARATOR);
      builder.append("  define(").append(interfaceName).append(".TYPE").append(",").append(Util.LINE_SEPARATOR);
      columnProperties.forEach(property -> builder.append("  ").append(getColumnPropertyDefinition(interfaceName, property, definition))
              .append(",").append(Util.LINE_SEPARATOR));
      foreignKeyProperties.forEach(property -> builder.append("  ").append(getForeignKeyPropertyDefinition(interfaceName, property))
              .append(",").append(Util.LINE_SEPARATOR));
      builder.replace(builder.length() - 2, builder.length(), "");
      builder.append(Util.LINE_SEPARATOR).append("  );").append(Util.LINE_SEPARATOR);

      builder.append("}").append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);
    });

    return builder.toString();
  }

  private static String getForeignKeyPropertyDefinition(final String interfaceName, final ForeignKeyProperty property) {
    final StringBuilder builder = new StringBuilder();
    final String foreignKeyAttribute = property.getAttribute().getName().toUpperCase();
    final String caption = property.getCaption();
    builder.append("        foreignKeyProperty(").append(interfaceName).append(".").append(foreignKeyAttribute).append(", \"").append(caption)
            .append("\")").append(Util.LINE_SEPARATOR);
    property.getReferences().forEach(reference ->
            builder.append("                .reference(").append(interfaceName).append(".")
                    .append(reference.getAttribute().getName().toUpperCase()).append(", ")
                    .append(getInterfaceName(reference.getReferencedAttribute().getEntityType().getName(), true)).append(".")
                    .append(reference.getReferencedAttribute().getName().toUpperCase()).append(")"));

    return builder.toString();
  }

  private static String getColumnPropertyDefinition(final String interfaceName, final ColumnProperty<?> property,
                                                    final EntityDefinition definition) {
    final StringBuilder builder = new StringBuilder();
    builder.append(PROPERTIES_COLUMN_PROPERTY).append(interfaceName + "." + property.getColumnName().toUpperCase());
    if (!definition.isForeignKeyAttribute(property.getAttribute()) && !property.isPrimaryKeyColumn()) {
      builder.append(", ").append("\"").append(property.getCaption()).append("\")");
    }
    else {
      builder.append(")");
    }

    if (property.isPrimaryKeyColumn()) {
      builder.append(Util.LINE_SEPARATOR).append("                .primaryKeyIndex(").append(property.getPrimaryKeyIndex()).append(")");
    }
    if (property.columnHasDefaultValue()) {
      builder.append(Util.LINE_SEPARATOR).append("                .columnHasDefaultValue(true)");
    }
    if (!property.isNullable() && !property.isPrimaryKeyColumn()) {
      builder.append(Util.LINE_SEPARATOR).append("                .nullable(false)");
    }
    if (String.class.equals(property.getAttribute().getTypeClass())) {
      builder.append(Util.LINE_SEPARATOR).append("                .maximumLength(").append(property.getMaximumLength()).append(")");
    }
    if (Double.class.equals(property.getAttribute().getTypeClass()) && property.getMaximumFractionDigits() >= 1) {
      builder.append(Util.LINE_SEPARATOR).append("                .maximumFractionDigits(").append(property.getMaximumFractionDigits()).append(")");
    }
    if (!nullOrEmpty(property.getDescription())) {
      builder.append(Util.LINE_SEPARATOR).append("                .description(").append(property.getDescription()).append(")");
    }

    return builder.toString();
  }

  private String getAttributeTypePrefix(final String typeClassName) {
    if (typeClassName.equals("byte[]")) {
      return "blob";
    }

    return typeClassName.substring(0, 1).toLowerCase() + typeClassName.substring(1);
  }

  private static String getInterfaceName(final String tableName, final boolean uppercase) {
    String name = tableName;
    if (name.contains(".")) {
      name = name.substring(name.lastIndexOf('.') + 1);
    }
    name = Text.underscoreToCamelCase(name);
    if (uppercase) {
      name = name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    return name;
  }

  private static <T> Attribute<T> getAttribute(final EntityType<?> entityType, final Column column) {
    return (Attribute<T>) entityType.attribute(column.getColumnName(), column.getColumnTypeClass());
  }

  private static final class MetaDataDomain extends DefaultDomain {

    private MetaDataDomain(final DomainType domainType) {
      super(domainType);
    }

    private void define(final EntityType<?> entityType, final List<Property.Builder<?>> propertyBuilders) {
      define(entityType, entityType.getName(), propertyBuilders.toArray(new Property.Builder[0]));
    }
  }

  private final class SchemaModel extends AbstractFilteredTableModel<Schema, Integer> {

    private SchemaModel() {
      super(new AbstractTableSortModel<Schema, Integer>(createSchemaColumns()) {
        @Override
        public Class<?> getColumnClass(final Integer columnIdentifier) {
          return Schema.class;
        }

        @Override
        protected Comparable<?> getComparable(final Schema row, final Integer columnIdentifier) {
          return row.getName();
        }
      }, singletonList(new DefaultColumnConditionModel<>(0, Schema.class, "%")));
    }

    @Override
    protected void doRefresh() {
      clear();
      addItems(new ArrayList<>(domains.keySet()));
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      return getItemAt(rowIndex);
    }
  }

  private final class DomainModel extends AbstractFilteredTableModel<EntityDefinition, Integer> {

    private DomainModel() {
      super(new AbstractTableSortModel<EntityDefinition, Integer>(createDefinitionColumns()) {
        @Override
        public Class<?> getColumnClass(final Integer columnIdentifier) {
          switch (columnIdentifier) {
            case 0: return Domain.class;
            case 1: return EntityDefinition.class;
            default:
              throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
          }
        }

        @Override
        protected Comparable<?> getComparable(final EntityDefinition row, final Integer columnIdentifier) {
          switch (columnIdentifier) {
            case 0: return row.getDomainName();
            case 1: return row.getEntityType().getName();
            default:
              throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
          }
        }
      }, asList(new DefaultColumnConditionModel<>(0, Domain.class, "%"),
              new DefaultColumnConditionModel<>(1, EntityDefinition.class, "%")));
    }

    @Override
    protected void doRefresh() {
      clear();
      schemaModel.getSelectionModel().getSelectedItems().forEach(schema ->
              addItemsAt(0, new ArrayList<>(domains.get(schema).getEntities().getDefinitions())));
    }

    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
      final EntityDefinition definition = getItemAt(rowIndex);
      switch (columnIndex) {
        case 0: return definition.getEntityType().getDomainName();
        case 1: return definition.getEntityType().getName();
        default:
          throw new IllegalArgumentException("Unknown column: " + columnIndex);
      }
    }
  }

  private static List<TableColumn> createSchemaColumns() {
    final TableColumn schemaColumn = new TableColumn(0);
    schemaColumn.setIdentifier(0);
    schemaColumn.setHeaderValue("Schema");

    return singletonList(schemaColumn);
  }

  private static List<TableColumn> createDefinitionColumns() {
    final TableColumn domainColumn = new TableColumn(0);
    domainColumn.setIdentifier(0);
    domainColumn.setHeaderValue("Domain");
    final TableColumn entityTypeColumn = new TableColumn(1);
    entityTypeColumn.setIdentifier(1);
    entityTypeColumn.setHeaderValue("Entity");

    return asList(domainColumn, entityTypeColumn);
  }

  protected static String getCaption(final Column column) {
    final String columnName = column.getColumnName().toLowerCase().replaceAll("_", " ");

    return columnName.substring(0, 1).toUpperCase() + columnName.substring(1);
  }
}
