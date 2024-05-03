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

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.Columns;
import is.codion.swing.framework.model.tools.metadata.MetaDataModel;
import is.codion.swing.framework.model.tools.metadata.MetaDataSchema;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static is.codion.common.Separators.LINE_SEPARATOR;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * For instances use the factory method {@link #domainGeneratorModel(Database, User)}.
 */
public final class DomainGeneratorModel {

	private final MetaDataModel metaDataModel;
	private final FilteredTableModel<MetaDataSchema, SchemaColumns.Id> schemaTableModel;
	private final FilteredTableModel<DefinitionRow, DefinitionColumns.Id> definitionTableModel;
	private final Connection connection;
	private final Value<String> domainSourceValue = Value.value();

	private DomainGeneratorModel(Database database, User user) throws DatabaseException {
		this.connection = requireNonNull(database, "database").createConnection(user);
		try {
			this.metaDataModel = new MetaDataModel(connection.getMetaData());
			this.schemaTableModel = FilteredTableModel.builder(new SchemaColumns())
							.items(metaDataModel::schemas)
							.build();
			this.definitionTableModel = FilteredTableModel.builder(new DefinitionColumns())
							.items(new DefinitionItems())
							.build();
			this.schemaTableModel.refresh();
			bindEvents();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	public FilteredTableModel<MetaDataSchema, SchemaColumns.Id> schemaModel() {
		return schemaTableModel;
	}

	public FilteredTableModel<DefinitionRow, DefinitionColumns.Id> definitionModel() {
		return definitionTableModel;
	}

	public ValueObserver<String> domainSource() {
		return domainSourceValue.observer();
	}

	public void close() {
		try {
			connection.close();
		}
		catch (Exception ignored) {/*ignored*/}
	}

	public void populateSelected(Consumer<String> schemaNotifier) {
		schemaTableModel.selectionModel().getSelectedItems().forEach(schema ->
						metaDataModel.populateSchema(schema.name(), schemaNotifier));
		definitionTableModel.refresh();
	}

	private void bindEvents() {
		schemaTableModel.selectionModel().selectionEvent().addListener(definitionTableModel::refresh);
		definitionTableModel.selectionModel().selectionEvent().addListener(this::updateDomainSource);
	}

	/**
	 * Instantiates a new {@link DomainGeneratorModel} instance.
	 * @param database the database to connect to
	 * @param user the user to connect with
	 * @return a new {@link DomainGeneratorModel} instance
	 * @throws DatabaseException in case of an exception while connecting to the database
	 */
	public static DomainGeneratorModel domainGeneratorModel(Database database, User user) throws DatabaseException {
		return new DomainGeneratorModel(database, user);
	}

	private void updateDomainSource() {
		domainSourceValue.set(definitionTableModel.selectionModel().getSelectedItems().stream()
						.map(definitionRow -> DomainToString.toString(definitionRow.definition))
						.collect(Collectors.joining(LINE_SEPARATOR + LINE_SEPARATOR)));
	}

	private final class DefinitionItems implements Supplier<Collection<DefinitionRow>> {

		@Override
		public Collection<DefinitionRow> get() {
			return schemaTableModel.selectionModel().getSelectedItems().stream()
							.flatMap(schema -> createDefinitionRows(schema).stream())
							.collect(toList());
		}

		private Collection<DefinitionRow> createDefinitionRows(MetaDataSchema schema) {
			DatabaseDomain domain = new DatabaseDomain(schema);

			return domain.entities().definitions().stream()
							.map(definition -> new DefinitionRow(definition, domain.tableType(definition.entityType())))
							.collect(toList());
		}
	}

	public static final class SchemaColumns implements Columns<MetaDataSchema, SchemaColumns.Id> {

		public enum Id {
			CATALOG,
			SCHEMA,
			POPULATED
		}

		private static final List<Id> IDENTIFIERS = unmodifiableList(asList(Id.values()));

		@Override
		public List<Id> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(Id identifier) {
			if (identifier == Id.POPULATED) {
				return Boolean.class;
			}

			return String.class;
		}

		@Override
		public Object value(MetaDataSchema row, Id identifier) {
			switch (identifier) {
				case CATALOG:
					return row.catalog();
				case SCHEMA:
					return row.name();
				case POPULATED:
					return row.populated();
				default:
					throw new IllegalArgumentException("Unknown column: " + identifier);
			}
		}
	}

	public static final class DefinitionColumns implements Columns<DefinitionRow, DefinitionColumns.Id> {

		public enum Id {
			DOMAIN,
			ENTITY,
			TABLE_TYPE
		}

		private static final List<Id> IDENTIFIERS = unmodifiableList(asList(Id.values()));

		@Override
		public List<Id> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(Id identifier) {
			return String.class;
		}

		@Override
		public Object value(DefinitionRow row, Id identifier) {
			switch (identifier) {
				case DOMAIN:
					return row.definition.entityType().domainType().name();
				case ENTITY:
					return row.definition.entityType().name();
				case TABLE_TYPE:
					return row.tableType;
				default:
					throw new IllegalArgumentException("Unknown column: " + identifier);
			}
		}
	}
}
