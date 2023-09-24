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

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.framework.model.tools.metadata.MetaDataModel;
import is.codion.swing.framework.model.tools.metadata.Schema;

import javax.swing.SortOrder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static is.codion.common.Separators.LINE_SEPARATOR;
import static is.codion.framework.domain.DomainType.domainType;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * For instances use the factory method {@link #databaseExplorerModel(Database, User)}.
 */
public final class DatabaseExplorerModel {

  private final MetaDataModel metaDataModel;
  private final FilteredTableModel<Schema, Integer> schemaTableModel;
  private final FilteredTableModel<DefinitionRow, Integer> definitionTableModel;
  private final Connection connection;
  private final Value<String> domainSourceValue = Value.value();

  static {
    EntityDefinition.STRICT_FOREIGN_KEYS.set(false);
  }

  private DatabaseExplorerModel(Database database, User user) throws DatabaseException {
    this.connection = requireNonNull(database, "database").createConnection(user);
    try {
      this.metaDataModel = new MetaDataModel(connection.getMetaData());
      this.schemaTableModel = FilteredTableModel.builder(new SchemaColumnFactory(), new SchemaColumnValueProvider())
              .itemSupplier(metaDataModel::schemas)
              .build();
      this.schemaTableModel.sortModel().setSortOrder(0, SortOrder.ASCENDING);
      this.definitionTableModel = FilteredTableModel.builder(new DefinitionColumnFactory(), new DefinitionColumnValueProvider())
              .itemSupplier(new DefinitionItemSupplier())
              .build();
      this.schemaTableModel.refresh();
      bindEvents();
    }
    catch (SQLException e) {
      throw new DatabaseException(e);
    }
  }

  public FilteredTableModel<Schema, Integer> schemaModel() {
    return schemaTableModel;
  }

  public FilteredTableModel<DefinitionRow, Integer> definitionModel() {
    return definitionTableModel;
  }

  public ValueObserver<String> domainSourceObserver() {
    return domainSourceValue.observer();
  }

  public void close() {
    try {
      connection.close();
    }
    catch (Exception ignored) {/*ignored*/}
  }

  private void bindEvents() {
    schemaTableModel.selectionModel().addSelectionListener(definitionTableModel::refresh);
    definitionTableModel.selectionModel().addSelectionListener(this::updateCodeValue);
  }

  public void populateSelected(Consumer<String> schemaNotifier) {
    schemaTableModel.selectionModel().getSelectedItems().forEach(schema ->
            metaDataModel.populateSchema(schema.name(), schemaNotifier));
    definitionTableModel.refresh();
  }

  /**
   * Instantiates a new {@link DatabaseExplorerModel} instance.
   * @param database the database to connect to
   * @param user the user to connect with
   * @return a new {@link DatabaseExplorerModel} instance
   * @throws DatabaseException in case of an exception while connecting to the database
   */
  public static DatabaseExplorerModel databaseExplorerModel(Database database, User user) throws DatabaseException {
    return new DatabaseExplorerModel(database, user);
  }

  private void updateCodeValue() {
    domainSourceValue.set(definitionTableModel.selectionModel().getSelectedItems().stream()
            .map(definitionRow -> DomainToString.toString(definitionRow.definition))
            .collect(Collectors.joining(LINE_SEPARATOR + LINE_SEPARATOR)));
  }

  private static final class SchemaColumnFactory implements FilteredTableModel.ColumnFactory<Integer> {
    @Override
    public List<FilteredTableColumn<Integer>> createColumns() {
      FilteredTableColumn<Integer> schemaColumn = FilteredTableColumn.builder(SchemaColumnValueProvider.SCHEMA)
              .headerValue("Schema")
              .columnClass(String.class)
              .build();
      FilteredTableColumn<Integer> populatedColumn = FilteredTableColumn.builder(SchemaColumnValueProvider.POPULATED)
              .headerValue("Populated")
              .columnClass(Boolean.class)
              .build();

      return asList(schemaColumn, populatedColumn);
    }
  }

  private static final class DefinitionColumnFactory implements FilteredTableModel.ColumnFactory<Integer> {
    @Override
    public List<FilteredTableColumn<Integer>> createColumns() {
      FilteredTableColumn<Integer> domainColumn = FilteredTableColumn.builder(DefinitionColumnValueProvider.DOMAIN)
              .headerValue("Domain")
              .columnClass(String.class)
              .build();
      FilteredTableColumn<Integer> entityTypeColumn = FilteredTableColumn.builder(DefinitionColumnValueProvider.ENTITY)
              .headerValue("Entity")
              .columnClass(String.class)
              .build();

      return asList(domainColumn, entityTypeColumn);
    }
  }

  private static Collection<DefinitionRow> createDomainDefinitions(Schema schema) {
    DatabaseDomain domain = new DatabaseDomain(domainType(schema.name()), schema.tables().values());

    return domain.entities().definitions().stream()
            .map(definition -> new DefinitionRow(domain, definition))
            .collect(toList());
  }

  private final class DefinitionItemSupplier implements Supplier<Collection<DefinitionRow>> {

    @Override
    public Collection<DefinitionRow> get() {
      Collection<DefinitionRow> items = new ArrayList<>();
      schemaTableModel.selectionModel().getSelectedItems().forEach(schema -> items.addAll(createDomainDefinitions(schema)));

      return items;
    }
  }

  private static final class SchemaColumnValueProvider implements FilteredTableModel.ColumnValueProvider<Schema, Integer> {

    private static final int SCHEMA = 0;
    private static final int POPULATED = 1;

    @Override
    public Object value(Schema row, Integer columnIdentifier) {
      switch (columnIdentifier) {
        case SCHEMA:
          return row.name();
        case POPULATED:
          return row.isPopulated();
        default:
          throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
      }
    }
  }

  private static final class DefinitionColumnValueProvider implements FilteredTableModel.ColumnValueProvider<DefinitionRow, Integer> {

    private static final int DOMAIN = 0;
    private static final int ENTITY = 1;

    @Override
    public Object value(DefinitionRow row, Integer columnIdentifier) {
      switch (columnIdentifier) {
        case DOMAIN:
          return row.domain.type().name();
        case ENTITY:
          return row.definition.entityType().name();
        default:
          throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
      }
    }
  }
}
