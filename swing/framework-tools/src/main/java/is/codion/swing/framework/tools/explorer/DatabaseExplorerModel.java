/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.common.user.User;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.TableColumn;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public final class DatabaseExplorerModel {

  private static final Logger LOG = LoggerFactory.getLogger(DatabaseExplorerModel.class);

  private final MetaDataModel metadataModel;
  private final SchemaModel schemaModel;
  private final DefinitionModel definitionModel;
  private final Map<Table, EntityType<Entity>> tableEntityTypes = new HashMap<>();

  private final Database database;
  private final Map<Schema, MetaDataDomain> domains = new HashMap<>();
  private final Connection connection;

  public DatabaseExplorerModel(final Database database, final User user) throws DatabaseException {
    this.database = database;
    this.connection = database.createConnection(user);
    this.schemaModel = new SchemaModel();
    this.definitionModel = new DefinitionModel();
    this.metadataModel = new MetaDataModel(connection);
    bindEvents();
    refreshDomains();
  }

  public AbstractFilteredTableModel<Schema, Integer> getSchemaModel() {
    return schemaModel;
  }

  public AbstractFilteredTableModel<EntityDefinition, Integer> getDefinitionModel() {
    return definitionModel;
  }

  private MetaDataDomain createDomain(final Schema schema) {
    final MetaDataDomain domain = new MetaDataDomain(DomainType.domainType(schema.getName()));
    EntityDefinition.STRICT_FOREIGN_KEYS.set(false);
    final Set<Table> definedTables = new HashSet<>();
    schema.getTables().values().forEach(table -> defineEntity(domain, table, definedTables));

    return domain;
  }

  private void defineEntity(final MetaDataDomain domain, final Table table, final Set<Table> definedTables) {
    if (!tableEntityTypes.containsKey(table)) {
      final EntityType<Entity> entityType = domain.getDomainType().entityType(table.getSchema().getName() + "." + table.getTableName());
      tableEntityTypes.put(table, entityType);
      final List<Property.Builder<?>> builders = new ArrayList<>();
      table.getColumns().values().forEach(column ->
              builders.add(getColumnPropertyBuilder(column, entityType)));
      table.getForeignKeys().forEach((referencedTable, foreignKey) -> {
        if (!foreignKey.getReferencedTable().equals(table)) {
          defineEntity(domain, foreignKey.getReferencedTable(), definedTables);
        }
      });
      table.getForeignKeys().forEach((referencedTable, foreignKey) ->
              builders.add(getForeignKeyPropertyBuilder(foreignKey, entityType)));
      domain.define(entityType, builders);
    }
  }

  private Property.Builder<?> getPropertyBuilder(final Column column, final EntityType<?> entityType) {
    return getColumnPropertyBuilder(column, entityType);
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
    builder.nullable(column.getNullable() == DatabaseMetaData.columnNoNulls);

    return builder;
  }

  private void refreshDomains() {
    tableEntityTypes.clear();
    domains.clear();
    metadataModel.getSchemas().forEach((schemaName, schema) -> domains.put(schema, createDomain(schema)));
    schemaModel.refresh();
  }

  private void bindEvents() {
    schemaModel.getSelectionModel().addSelectionChangedListener(definitionModel::refresh);
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

  private final class DefinitionModel extends AbstractFilteredTableModel<EntityDefinition, Integer> {

    private DefinitionModel() {
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
