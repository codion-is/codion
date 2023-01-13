/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.framework.tools.metadata.MetaDataModel;
import is.codion.swing.framework.tools.metadata.Schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.stream.Collectors;

import static is.codion.common.Separators.LINE_SEPARATOR;
import static java.util.Objects.requireNonNull;

/**
 * For instances use the factory method {@link #databaseExplorerModel(Database, User)}.
 */
public final class DatabaseExplorerModel {

  private final MetaDataModel metaDataModel;
  private final SchemaTableModel schemaTableModel;
  private final DefinitionTableModel definitionTableModel;
  private final Connection connection;
  private final Value<String> domainSourceValue = Value.value();

  static {
    EntityDefinition.STRICT_FOREIGN_KEYS.set(false);
  }

  private DatabaseExplorerModel(Database database, User user) throws DatabaseException {
    this.connection = requireNonNull(database, "database").createConnection(user);
    try {
      this.metaDataModel = new MetaDataModel(connection.getMetaData());
      this.schemaTableModel = new SchemaTableModel(metaDataModel.schemas());
      this.definitionTableModel = new DefinitionTableModel(schemaTableModel);
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
    Database.closeSilently(connection);
  }

  private void bindEvents() {
    schemaTableModel.selectionModel().addSelectionListener(definitionTableModel::refresh);
    definitionTableModel.selectionModel().addSelectionListener(this::updateCodeValue);
  }

  public void populateSelected(EventDataListener<String> schemaNotifier) {
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
}
