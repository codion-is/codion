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
package is.codion.tools.generator.model;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;
import is.codion.tools.generator.domain.DatabaseDomain;
import is.codion.tools.generator.domain.DomainSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static is.codion.common.Configuration.stringValue;
import static is.codion.common.Text.nullOrEmpty;
import static is.codion.common.value.Value.Notify.WHEN_SET;
import static is.codion.tools.generator.domain.DatabaseDomain.databaseDomain;
import static is.codion.tools.generator.domain.DomainSource.domainSource;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * For instances use the factory method {@link #domainGeneratorModel(Database, User)}.
 */
public final class DomainGeneratorModel {

	/**
	 * The default package.
	 */
	public static final PropertyValue<String> DEFAULT_DOMAIN_PACKAGE =
					stringValue("codion.domain.generator.defaultDomainPackage", "");

	/**
	 * The default source directory.
	 */
	public static final PropertyValue<String> DEFAULT_SOURCE_DIRECTORY =
					stringValue("codion.domain.generator.defaultSourceDirectory", System.getProperty("user.dir"));

	private static final Pattern PACKAGE_PATTERN =
					Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*$");

	private final FilterTableModel<SchemaRow, SchemaColumns.Id> schemaTableModel =
					FilterTableModel.builder(new SchemaColumns())
									.items(new SchemaItems())
									.build();
	private final State populatedSchemaSelected = State.state();
	private final FilterTableModel<EntityRow, EntityColumns.Id> entityTableModel =
					FilterTableModel.builder(new EntityColumns())
									.items(new EntityItems())
									.build();
	private final Connection connection;
	private final State domainPackageSpecified = State.state();
	private final Value<String> domainPackageValue = Value.builder()
					.nonNull(DEFAULT_DOMAIN_PACKAGE.get())
					.listener(this::domainPackageChanged)
					.build();
	private final State sourceDirectorySpecified = State.state();
	private final Value<String> sourceDirectoryValue = Value.builder()
					.nonNull(DEFAULT_SOURCE_DIRECTORY.get())
					.listener(this::sourceDirectoryChanged)
					.build();
	private final Value<String> domainImplValue = Value.builder()
					.<String>nullable()
					.notify(WHEN_SET)
					.build();
	private final Value<String> domainApiValue = Value.builder()
					.<String>nullable()
					.notify(WHEN_SET)
					.build();
	private final Value<String> domainCombinedValue = Value.builder()
					.<String>nullable()
					.notify(WHEN_SET)
					.build();
	private final Value<String> apiSearchValue = Value.value();
	private final Value<String> implSearchValue = Value.value();

	private DomainGeneratorModel(Database database, User user) throws DatabaseException {
		this.connection = requireNonNull(database, "database").createConnection(user);
		sourceDirectoryChanged();
		domainPackageChanged();
		schemaTableModel.refresh();
		bindEvents();
	}

	public FilterTableModel<SchemaRow, SchemaColumns.Id> schemaModel() {
		return schemaTableModel;
	}

	public FilterTableModel<EntityRow, EntityColumns.Id> entityModel() {
		return entityTableModel;
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

	public Value<String> sourceDirectory() {
		return sourceDirectoryValue;
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
		schemaTableModel.selectionModel().selectedItem().ifPresent(schema -> {
			schemaNotifier.accept(schema.name());
			schema.setDomain(databaseDomain(connection, schema.name()));
			int index = schemaTableModel.indexOf(schema);
			schemaTableModel.fireTableRowsUpdated(index, index);
		});
		schemaSelectionChanged();
	}

	public void saveApiImpl() throws IOException {
		if (schemaTableModel.selectionModel().selectionNotEmpty().get()) {
			DatabaseDomain domain = selectedDomain();
			if (domain != null) {
				domainSource(domain, domainPackageValue.optional()
								.filter(DomainGeneratorModel::validPackageName)
								.orElse(""))
								.writeApiImpl(savePath(new File(sourceDirectoryValue.get())));
			}
		}
	}

	public void saveCombined() throws IOException {
		if (schemaTableModel.selectionModel().selectionNotEmpty().get()) {
			DatabaseDomain domain = selectedDomain();
			if (domain != null) {
				domainSource(domain, domainPackageValue.optional()
								.filter(DomainGeneratorModel::validPackageName)
								.orElse(""))
								.writeCombined(savePath(new File(sourceDirectoryValue.get())));
			}
		}
	}

	public StateObserver saveEnabled() {
		return State.and(domainPackageSpecified, sourceDirectorySpecified, populatedSchemaSelected);
	}

	private void bindEvents() {
		schemaTableModel.selectionModel().selectionEvent().addListener(this::schemaSelectionChanged);
		entityModel().selectionModel().selectedItemEvent().addConsumer(this::search);
	}

	private void schemaSelectionChanged() {
		entityTableModel.refresh();
		updateDomainSource();
		populatedSchemaSelected.set(schemaTableModel.selectionModel().selectedItem()
						.map(SchemaRow::populated)
						.orElse(false));
	}

	private void search(EntityRow entityRow) {
		apiSearchValue.set(entityRow == null ? null : DomainSource.apiSearchString(entityRow.definition));
		implSearchValue.set(entityRow == null ? null : DomainSource.implSearchString(entityRow.definition));
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

	private void sourceDirectoryChanged() {
		sourceDirectorySpecified.set(nonNull(sourceDirectoryValue.get()));
	}

	private void domainPackageChanged() {
		domainPackageSpecified.set(!nullOrEmpty(domainPackageValue.get()));
		updateDomainSource();
	}

	private void updateDomainSource() {
		DatabaseDomain selectedDomain = selectedDomain();
		if (selectedDomain != null) {
			DomainSource domainSource = domainSource(selectedDomain, domainPackageValue.optional()
							.filter(DomainGeneratorModel::validPackageName)
							.orElse(""));
			domainApiValue.set(domainSource.api());
			domainImplValue.set(domainSource.implementation());
			domainCombinedValue.set(domainSource.combined());
		}
		else {
			domainApiValue.set(null);
			domainImplValue.set(null);
			domainCombinedValue.set(null);
		}
	}

	private DatabaseDomain selectedDomain() {
		return schemaTableModel.selectionModel().selectedItem()
						.map(SchemaRow::domain)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.orElse(null);
	}

	private Path savePath(File directory) {
		Path path = directory.toPath();
		String domainPackage = domainPackageValue.get();
		if (domainPackage != null) {
			for (String pkg : domainPackage.split("\\.")) {
				path = path.resolve(pkg);
			}
		}

		return path;
	}

	private static boolean validPackageName(String packageName) {
		return PACKAGE_PATTERN.matcher(packageName).matches();
	}

	private final class SchemaItems implements Supplier<Collection<SchemaRow>> {

		@Override
		public Collection<SchemaRow> get() {
			Collection<SchemaRow> schemaRows = new ArrayList<>();
			try (ResultSet resultSet = connection.getMetaData().getSchemas()) {
				while (resultSet.next()) {
					String tableSchem = resultSet.getString("TABLE_SCHEM");
					if (tableSchem != null) {
						schemaRows.add(new SchemaRow(resultSet.getString("TABLE_CATALOG"), tableSchem));
					}
				}

				return schemaRows;
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private final class EntityItems implements Supplier<Collection<EntityRow>> {

		@Override
		public Collection<EntityRow> get() {
			return schemaTableModel.selectionModel().getSelectedItems().stream()
							.map(SchemaRow::domain)
							.filter(Optional::isPresent)
							.map(Optional::get)
							.flatMap(domain -> domain.entities().definitions().stream()
											.map(definition -> new EntityRow(definition, domain.tableType(definition.entityType()))))
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

	public static final class EntityColumns implements Columns<EntityRow, EntityColumns.Id> {

		public enum Id {
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
		public Object value(EntityRow row, Id identifier) {
			switch (identifier) {
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
