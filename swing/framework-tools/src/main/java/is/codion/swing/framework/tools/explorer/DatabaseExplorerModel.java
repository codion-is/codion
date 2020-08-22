/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.common.value.Values;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.framework.tools.metadata.MetaDataModel;
import is.codion.swing.framework.tools.metadata.Schema;

import java.sql.Connection;
import java.sql.SQLException;

import static java.util.Objects.requireNonNull;

public final class DatabaseExplorerModel {

  private final MetaDataModel metaDataModel;
  private final SchemaTableModel schemaTableModel;
  private final DefinitionTableModel definitionTableModel;
  private final Connection connection;
  private final Value<String> domainSourceValue = Values.value();

  static {
    EntityDefinition.STRICT_FOREIGN_KEYS.set(false);
  }

  public DatabaseExplorerModel(final Database database, final User user) throws DatabaseException {
    this.connection = requireNonNull(database, "database").createConnection(user);
    try {
      this.metaDataModel = new MetaDataModel(connection.getMetaData());
      this.schemaTableModel = new SchemaTableModel(metaDataModel.getSchemas());
      this.definitionTableModel = new DefinitionTableModel(schemaTableModel);
      this.schemaTableModel.refresh();
      bindEvents();
    }
    catch (final SQLException e) {
      throw new DatabaseException(e, e.getMessage());
    }
  }

  public AbstractFilteredTableModel<Schema, Integer> getSchemaModel() {
    return schemaTableModel;
  }

  public AbstractFilteredTableModel<DefinitionRow, Integer> getDefinitionModel() {
    return definitionTableModel;
  }

  public ValueObserver<String> getDomainSourceObserver() {
    return Values.valueObserver(domainSourceValue);
  }

  public void close() {
    Database.closeSilently(connection);
  }

  private void bindEvents() {
    schemaTableModel.getSelectionModel().addSelectionChangedListener(definitionTableModel::refresh);
    definitionTableModel.getSelectionModel().addSelectionChangedListener(this::updateCodeValue);
  }

  public void populateSelected(final EventDataListener<String> schemaNotifier) {
    schemaTableModel.getSelectionModel().getSelectedItems().forEach(schema ->
            metaDataModel.populateSchema(schema.getName(), schemaNotifier));
    metaDataModel.resolveForeignKeys();
    definitionTableModel.refresh();
  }

  private void updateCodeValue() {
    final StringBuilder builder = new StringBuilder();
    definitionTableModel.getSelectionModel().getSelectedItems().forEach(definitionRow ->
            builder.append(DomainToString.toString(definitionRow.definition)));
    domainSourceValue.set(builder.toString());
  }
}
