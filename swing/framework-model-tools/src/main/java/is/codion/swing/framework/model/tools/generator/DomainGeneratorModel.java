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
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;
import is.codion.swing.framework.model.tools.metadata.MetaDataModel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static is.codion.swing.framework.model.tools.generator.DomainToString.apiSearchString;
import static is.codion.swing.framework.model.tools.generator.DomainToString.implSearchString;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * For instances use the factory method {@link #domainGeneratorModel(Database, User)}.
 */
public final class DomainGeneratorModel {

	private static final Pattern PACKAGE_PATTERN =
					Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*$");

	private final MetaDataModel metaDataModel;
	private final FilterTableModel<SchemaRow, SchemaColumns.Id> schemaTableModel;
	private final FilterTableModel<DefinitionRow, DefinitionColumns.Id> definitionTableModel;
	private final Connection connection;
	private final Value<String> domainPackageValue = Value.nonNull("").build();
	private final Value<String> domainImplValue = Value.<String>nullable()
					.notify(Value.Notify.WHEN_SET)
					.build();
	private final Value<String> domainApiValue = Value.<String>nullable()
					.notify(Value.Notify.WHEN_SET)
					.build();
	private final Value<String> domainCombinedValue = Value.<String>nullable()
					.notify(Value.Notify.WHEN_SET)
					.build();
	private final Value<String> apiSearchValue = Value.value();
	private final Value<String> implSearchValue = Value.value();

	private DomainGeneratorModel(Database database, User user) throws DatabaseException {
		this.connection = requireNonNull(database, "database").createConnection(user);
		try {
			this.metaDataModel = new MetaDataModel(connection.getMetaData());
			this.schemaTableModel = FilterTableModel.builder(new SchemaColumns())
							.items(new SchemaItems())
							.build();
			this.definitionTableModel = FilterTableModel.builder(new DefinitionColumns())
							.items(new DefinitionItems())
							.build();
			this.schemaTableModel.refresh();
			bindEvents();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	public FilterTableModel<SchemaRow, SchemaColumns.Id> schemaModel() {
		return schemaTableModel;
	}

	public FilterTableModel<DefinitionRow, DefinitionColumns.Id> definitionModel() {
		return definitionTableModel;
	}

	public ValueObserver<String> domainImpl() {
		return domainImplValue.observer();
	}

	public ValueObserver<String> domainApi() {
		return domainApiValue.observer();
	}

	public ValueObserver<String> domainCombined() {
		return domainCombinedValue.observer();
	}

	public Value<String> domainPackage() {
		return domainPackageValue;
	}

	public Value<String> apiSearchValue() {
		return apiSearchValue;
	}

	public Value<String> implSearchValue() {
		return implSearchValue;
	}

	public void close() {
		try {
			connection.close();
		}
		catch (Exception ignored) {/*ignored*/}
	}

	public void populateSelected(Consumer<String> schemaNotifier) {
		schemaTableModel.selectionModel().getSelectedItems().forEach(schema -> {
			metaDataModel.populateSchema(schema.name(), schemaNotifier);
			schema.setDomain(new DatabaseDomain(schema.metadata));
			int index = schemaTableModel.indexOf(schema);
			schemaTableModel.fireTableRowsUpdated(index, index);
		});
		definitionTableModel.refresh();
		updateDomainSource();
	}

	private void bindEvents() {
		schemaTableModel.selectionModel().selectionEvent().addListener(definitionTableModel::refresh);
		schemaTableModel.selectionModel().selectionEvent().addListener(this::updateDomainSource);
		domainPackageValue.addListener(this::updateDomainSource);
		definitionModel().selectionModel().selectedItemEvent().addConsumer(this::search);
	}

	private void search(DefinitionRow definitionRow) {
		apiSearchValue.set(definitionRow == null ? null : apiSearchString(definitionRow.definition));
		implSearchValue.set(definitionRow == null ? null : implSearchString(definitionRow.definition));
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
		String packageName = domainPackageValue.get();
		if (!PACKAGE_PATTERN.matcher(packageName).matches()) {
			packageName = "";
		}
		domainApiValue.set(DomainToString.apiString(selectedDomains(), packageName));
		domainImplValue.set(DomainToString.implementationString(selectedDomains(), packageName));
		domainCombinedValue.set(DomainToString.combinedString(selectedDomains(), packageName));
	}

	private List<DatabaseDomain> selectedDomains() {
		return schemaTableModel.selectionModel().getSelectedItems().stream()
						.map(SchemaRow::domain)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.collect(toList());
	}

	private final class SchemaItems implements Supplier<Collection<SchemaRow>> {

		@Override
		public Collection<SchemaRow> get() {
			return metaDataModel.schemas()
							.stream()
							.map(metaDataSchema ->
											new SchemaRow(metaDataSchema, metaDataSchema.catalog(), metaDataSchema.name(), false))
							.collect(Collectors.toList());
		}
	}

	private final class DefinitionItems implements Supplier<Collection<DefinitionRow>> {

		@Override
		public Collection<DefinitionRow> get() {
			return schemaTableModel.selectionModel().getSelectedItems().stream()
							.map(SchemaRow::domain)
							.filter(Optional::isPresent)
							.map(Optional::get)
							.flatMap(domain -> domain.entities().definitions().stream()
											.map(definition -> new DefinitionRow(definition, domain.tableType(definition.entityType()))))
							.collect(toList());
		}
	}

	public static final class SchemaColumns implements Columns<SchemaRow, SchemaColumns.Id> {

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
		public Object value(SchemaRow row, Id identifier) {
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
