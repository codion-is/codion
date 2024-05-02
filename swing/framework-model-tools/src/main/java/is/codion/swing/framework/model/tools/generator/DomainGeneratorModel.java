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
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValues;
import is.codion.swing.framework.model.tools.metadata.MetaDataModel;
import is.codion.swing.framework.model.tools.metadata.MetaDataSchema;

import javax.swing.SortOrder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static is.codion.common.Separators.LINE_SEPARATOR;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * For instances use the factory method {@link #domainGeneratorModel(Database, User)}.
 */
public final class DomainGeneratorModel {

	private final MetaDataModel metaDataModel;
	private final FilteredTableModel<MetaDataSchema, Integer> schemaTableModel;
	private final FilteredTableModel<DefinitionRow, Integer> definitionTableModel;
	private final Connection connection;
	private final Value<String> domainSourceValue = Value.value();

	private DomainGeneratorModel(Database database, User user) throws DatabaseException {
		this.connection = requireNonNull(database, "database").createConnection(user);
		try {
			this.metaDataModel = new MetaDataModel(connection.getMetaData());
			this.schemaTableModel = FilteredTableModel.builder(new SchemaColumnFactory(), new SchemaColumnValues())
							.items(metaDataModel::schemas)
							.build();
			this.schemaTableModel.sortModel().setSortOrder(SchemaColumnValues.SCHEMA, SortOrder.ASCENDING);
			this.definitionTableModel = FilteredTableModel.builder(new DefinitionColumnFactory(), new DefinitionColumnValues())
							.items(new DefinitionItems())
							.build();
			this.schemaTableModel.refresh();
			bindEvents();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	public FilteredTableModel<MetaDataSchema, Integer> schemaModel() {
		return schemaTableModel;
	}

	public FilteredTableModel<DefinitionRow, Integer> definitionModel() {
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

	private static final class SchemaColumnFactory implements FilteredTableModel.ColumnFactory<Integer> {

		@Override
		public List<FilteredTableColumn<Integer>> createColumns() {
			FilteredTableColumn<Integer> catalogColumn = FilteredTableColumn.builder(SchemaColumnValues.CATALOG)
							.headerValue("Catalog")
							.columnClass(String.class)
							.build();
			FilteredTableColumn<Integer> schemaColumn = FilteredTableColumn.builder(SchemaColumnValues.SCHEMA)
							.headerValue("Schema")
							.columnClass(String.class)
							.build();
			FilteredTableColumn<Integer> populatedColumn = FilteredTableColumn.builder(SchemaColumnValues.POPULATED)
							.headerValue("Populated")
							.columnClass(Boolean.class)
							.build();

			return asList(catalogColumn, schemaColumn, populatedColumn);
		}
	}

	private static final class DefinitionColumnFactory implements FilteredTableModel.ColumnFactory<Integer> {

		@Override
		public List<FilteredTableColumn<Integer>> createColumns() {
			FilteredTableColumn<Integer> domainColumn = FilteredTableColumn.builder(DefinitionColumnValues.DOMAIN)
							.headerValue("Domain")
							.columnClass(String.class)
							.build();
			FilteredTableColumn<Integer> entityTypeColumn = FilteredTableColumn.builder(DefinitionColumnValues.ENTITY)
							.headerValue("Entity")
							.columnClass(String.class)
							.build();
			FilteredTableColumn<Integer> typeColumn = FilteredTableColumn.builder(DefinitionColumnValues.TABLE_TYPE)
							.headerValue("Type")
							.columnClass(String.class)
							.preferredWidth(120)
							.build();

			return asList(domainColumn, entityTypeColumn, typeColumn);
		}
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

	private static final class SchemaColumnValues implements ColumnValues<MetaDataSchema, Integer> {

		private static final int CATALOG = 0;
		private static final int SCHEMA = 1;
		private static final int POPULATED = 2;

		@Override
		public Object value(MetaDataSchema row, Integer columnIdentifier) {
			switch (columnIdentifier) {
				case CATALOG:
					return row.catalog();
				case SCHEMA:
					return row.name();
				case POPULATED:
					return row.populated();
				default:
					throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
			}
		}
	}

	private static final class DefinitionColumnValues implements ColumnValues<DefinitionRow, Integer> {

		private static final int DOMAIN = 0;
		private static final int ENTITY = 1;
		private static final int TABLE_TYPE = 2;

		@Override
		public Object value(DefinitionRow row, Integer columnIdentifier) {
			switch (columnIdentifier) {
				case DOMAIN:
					return row.definition.entityType().domainType().name();
				case ENTITY:
					return row.definition.entityType().name();
				case TABLE_TYPE:
					return row.tableType;
				default:
					throw new IllegalArgumentException("Unknown column: " + columnIdentifier);
			}
		}
	}
}
