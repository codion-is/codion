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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.generator.model;

import is.codion.common.db.database.Database;
import is.codion.common.observer.Observable;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.domain.db.SchemaDomain;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.tools.generator.domain.DomainSource;

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
import static is.codion.common.value.Value.Notify.SET;
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
					FilterTableModel.builder()
									.columns(new SchemaColumns())
									.supplier(new SchemaItems())
									.build();
	private final State populatedSchemaSelected = State.state();
	private final FilterTableModel<EntityRow, EntityColumns.Id> entityTableModel =
					FilterTableModel.builder()
									.columns(new EntityColumns())
									.supplier(new EntityItems())
									.build();
	private final Database database;
	private final User user;
	private final State includeDto = State.builder()
					.listener(this::updateDomainSource)
					.build();
	private final State domainPackageSpecified = State.state();
	private final Value<String> domainPackageValue = Value.builder()
					.nonNull(DEFAULT_DOMAIN_PACKAGE.getOrThrow())
					.listener(this::domainPackageChanged)
					.build();
	private final State sourceDirectorySpecified = State.state();
	private final Value<String> sourceDirectoryValue = Value.builder()
					.nonNull(DEFAULT_SOURCE_DIRECTORY.getOrThrow())
					.listener(this::sourceDirectoryChanged)
					.build();
	private final Value<String> domainImplValue = Value.builder()
					.<String>nullable()
					.notify(SET)
					.build();
	private final Value<String> domainApiValue = Value.builder()
					.<String>nullable()
					.notify(SET)
					.build();
	private final Value<String> domainCombinedValue = Value.builder()
					.<String>nullable()
					.notify(SET)
					.build();
	private final Value<String> apiSearchValue = Value.nullable();
	private final Value<String> implSearchValue = Value.nullable();

	private DomainGeneratorModel(Database database, User user) {
		this.database = requireNonNull(database);
		this.user = user;
		sourceDirectoryChanged();
		domainPackageChanged();
		schemaTableModel.sort().ascending(SchemaColumns.Id.SCHEMA);
		entityTableModel.sort().ascending(EntityColumns.Id.ENTITY);
		schemaTableModel.items().refresh();
		bindEvents();
	}

	public FilterTableModel<SchemaRow, SchemaColumns.Id> schemaModel() {
		return schemaTableModel;
	}

	public FilterTableModel<EntityRow, EntityColumns.Id> entityModel() {
		return entityTableModel;
	}

	public Observable<String> domainImpl() {
		return domainImplValue.observable();
	}

	public Observable<String> domainApi() {
		return domainApiValue.observable();
	}

	public Observable<String> domainCombined() {
		return domainCombinedValue.observable();
	}

	public State includeDto() {
		return includeDto;
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

	public void populateSelected(Consumer<String> schemaNotifier) {
		schemaTableModel.selection().item().optional().ifPresent(schema -> {
			schemaNotifier.accept(schema.name());
			schema.setDomain(schemaDomain(schema));
			int index = schemaTableModel.items().visible().indexOf(schema);
			schemaTableModel.fireTableRowsUpdated(index, index);
		});
		schemaSelectionChanged();
	}

	public void saveApiImpl() throws IOException {
		if (schemaTableModel.selection().empty().not().is()) {
			SchemaDomain domain = selectedDomain();
			if (domain != null) {
				domainSource(domain)
								.writeApiImpl(domainPackageValue.optional()
												.filter(DomainGeneratorModel::validPackageName)
												.orElse(""), includeDto.is(), savePath(Path.of(sourceDirectoryValue.getOrThrow())));
			}
		}
	}

	public void saveCombined() throws IOException {
		if (schemaTableModel.selection().empty().not().is()) {
			SchemaDomain domain = selectedDomain();
			if (domain != null) {
				domainSource(domain)
								.writeCombined(domainPackageValue.optional()
												.filter(DomainGeneratorModel::validPackageName)
												.orElse(""), includeDto.is(), savePath(Path.of(sourceDirectoryValue.getOrThrow())));
			}
		}
	}

	public ObservableState saveEnabled() {
		return State.and(domainPackageSpecified, sourceDirectorySpecified, populatedSchemaSelected);
	}

	private void bindEvents() {
		schemaTableModel.selection().indexes().addListener(this::schemaSelectionChanged);
		entityModel().selection().item().addConsumer(this::search);
	}

	private SchemaDomain schemaDomain(SchemaRow schema) {
		try (Connection connection = createConnection()) {
			return SchemaDomain.schemaDomain(connection.getMetaData(), schema.name());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Connection createConnection() {
		return user == null ? database.createConnection() : database.createConnection(user);
	}

	private void schemaSelectionChanged() {
		entityTableModel.items().refresh();
		updateDomainSource();
		populatedSchemaSelected.set(schemaTableModel.selection().item().optional()
						.map(SchemaRow::populated)
						.orElse(false));
	}

	private void search(EntityRow entityRow) {
		apiSearchValue.set(entityRow == null ? null : DomainSource.apiSearchString(entityRow.definition));
		implSearchValue.set(entityRow == null ? null : DomainSource.implSearchString(entityRow.definition));
	}

	/**
	 * Instantiates a new {@link DomainGeneratorModel} instance, assuming the underlying database does not require a user to connect.
	 * @param database the database to connect to
	 * @return a new {@link DomainGeneratorModel} instance
	 */
	public static DomainGeneratorModel domainGeneratorModel(Database database) {
		return new DomainGeneratorModel(database, null);
	}

	/**
	 * Instantiates a new {@link DomainGeneratorModel} instance.
	 * @param database the database to connect to
	 * @param user the user to connect with
	 * @return a new {@link DomainGeneratorModel} instance
	 */
	public static DomainGeneratorModel domainGeneratorModel(Database database, User user) {
		return new DomainGeneratorModel(database, requireNonNull(user));
	}

	private void sourceDirectoryChanged() {
		sourceDirectorySpecified.set(nonNull(sourceDirectoryValue.get()));
	}

	private void domainPackageChanged() {
		domainPackageSpecified.set(!nullOrEmpty(domainPackageValue.get()));
		updateDomainSource();
	}

	private void updateDomainSource() {
		SchemaDomain selectedDomain = selectedDomain();
		if (selectedDomain != null) {
			DomainSource domainSource = domainSource(selectedDomain);
			String domainPackage = domainPackageValue.optional()
							.filter(DomainGeneratorModel::validPackageName)
							.orElse("");
			domainApiValue.set(domainSource.api(domainPackage, includeDto.is()));
			domainImplValue.set(domainSource.implementation(domainPackage));
			domainCombinedValue.set(domainSource.combined(domainPackage, includeDto.is()));
		}
		else {
			domainApiValue.clear();
			domainImplValue.clear();
			domainCombinedValue.clear();
		}
	}

	private SchemaDomain selectedDomain() {
		return schemaTableModel.selection().item().optional()
						.flatMap(SchemaRow::domain)
						.orElse(null);
	}

	private Path savePath(Path path) {
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
			try (Connection connection = createConnection();
					 ResultSet resultSet = connection.getMetaData().getSchemas()) {
				return schemaRows(resultSet);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Collection<SchemaRow> schemaRows(ResultSet resultSet) throws SQLException {
			Collection<SchemaRow> schemaRows = new ArrayList<>();
			while (resultSet.next()) {
				String tableSchem = resultSet.getString("TABLE_SCHEM");
				if (tableSchem != null) {
					schemaRows.add(new SchemaRow(resultSet.getString("TABLE_CATALOG"), tableSchem));
				}
			}
			if (schemaRows.isEmpty()) {
				schemaRows.add(new SchemaRow());
			}

			return schemaRows;
		}
	}

	private final class EntityItems implements Supplier<Collection<EntityRow>> {

		@Override
		public Collection<EntityRow> get() {
			return schemaTableModel.selection().items().get().stream()
							.map(SchemaRow::domain)
							.flatMap(Optional::stream)
							.flatMap(domain -> domain.entities().definitions().stream()
											.map(definition -> new EntityRow(definition, domain.tableType(definition.type()))))
							.collect(toList());
		}
	}

	public static final class SchemaColumns implements TableColumns<SchemaRow, SchemaColumns.Id> {

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

	public static final class EntityColumns implements TableColumns<EntityRow, EntityColumns.Id> {

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
					return row.definition.type().name();
				case TABLE_TYPE:
					return row.tableType;
				default:
					throw new IllegalArgumentException("Unknown column: " + identifier);
			}
		}
	}
}
