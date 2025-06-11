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
package is.codion.framework.domain.db;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static is.codion.common.Text.nullOrEmpty;
import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.attribute.ForeignKey.reference;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * For instances use the available factory methods.
 * @see #schemaDomain(DatabaseMetaData, String)
 * @see #schemaDomain(DatabaseMetaData, String, SchemaSettings)
 */
public final class SchemaDomain extends DomainModel {

	private static final int MAXIMUM_COLUMN_SIZE = 2_147_483_647;

	private final Map<MetaDataTable, EntityType> tableEntityTypes = new HashMap<>();

	private final SchemaSettings settings;

	private SchemaDomain(DatabaseMetaData metaData, String schemaName, SchemaSettings settings) throws SQLException {
		super(domainType(schemaName));
		this.settings = settings;
		validateForeignKeys(false);
		new MetaDataModel(metaData, schemaName)
						.schema().tables().values().forEach(this::defineEntity);
	}

	/**
	 * Factory method for creating a new {@link SchemaDomain} instance.
	 * @param metaData the database metadata
	 * @param schemaName the schema name
	 * @return a new {@link SchemaDomain} instance
	 */
	public static SchemaDomain schemaDomain(DatabaseMetaData metaData, String schemaName) {
		return schemaDomain(metaData, schemaName, SchemaSettings.builder().build());
	}

	/**
	 * Factory method for creating a new {@link SchemaDomain} instance.
	 * @param metaData the database metadata
	 * @param schemaName the schema name
	 * @param settings the configuration
	 * @return a new {@link SchemaDomain} instance
	 */
	public static SchemaDomain schemaDomain(DatabaseMetaData metaData, String schemaName, SchemaSettings settings) {
		try {
			return new SchemaDomain(requireNonNull(metaData), requireNonNull(schemaName), requireNonNull(settings));
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param entityType the entity type
	 * @return the table type for the given entity type, table or view
	 */
	public String tableType(EntityType entityType) {
		return tableEntityTypes.entrySet().stream()
						.filter(entry -> entry.getValue().equals(entityType))
						.findFirst()
						.map(Map.Entry::getKey)
						.map(MetaDataTable::tableType)
						.orElseThrow(IllegalArgumentException::new);
	}

	private void defineEntity(MetaDataTable table) {
		if (!tableEntityTypes.containsKey(table)) {
			EntityType entityType = type().entityType(table.schema().none() ?
							table.tableName() : table.schema().name() + "." + table.tableName());
			tableEntityTypes.put(table, entityType);
			table.foreignKeys().stream()
							.map(MetaDataForeignKeyConstraint::referencedTable)
							.filter(referencedTable -> !referencedTable.equals(table))
							.forEach(this::defineEntity);
			defineEntity(table, entityType);
		}
	}

	private void defineEntity(MetaDataTable table, EntityType entityType) {
		List<AttributeDefinition.Builder<?, ?>> attributeDefinitionBuilders = defineAttributes(table, entityType, new ArrayList<>(table.foreignKeys()));
		if (!attributeDefinitionBuilders.isEmpty()) {
			EntityDefinition.Builder entityDefinitionBuilder = entityType.define(attributeDefinitionBuilders.toArray(new AttributeDefinition.Builder[0]));
			entityDefinitionBuilder.caption(caption(table.tableName()));
			if (tableHasAutoIncrementPrimaryKeyColumn(table)) {
				entityDefinitionBuilder.keyGenerator(KeyGenerator.identity());
			}
			if (!nullOrEmpty(table.comment())) {
				entityDefinitionBuilder.description(table.comment());
			}
			entityDefinitionBuilder.readOnly("view".equalsIgnoreCase(table.tableType()));
			add(entityDefinitionBuilder.build());
		}
	}

	private List<AttributeDefinition.Builder<?, ?>> defineAttributes(MetaDataTable table, EntityType entityType,
																																	 Collection<MetaDataForeignKeyConstraint> foreignKeyConstraints) {
		List<AttributeDefinition.Builder<?, ?>> builders = new ArrayList<>();
		table.columns().forEach(column -> {
			builders.add(columnDefinitionBuilder(column, entityType));
			if (column.foreignKeyColumn()) {
				foreignKeyConstraints.stream()
								//if this is the last column in the foreign key
								.filter(foreignKeyConstraint -> lastKeyColumn(foreignKeyConstraint, column))
								.findFirst()
								.ifPresent(foreignKeyConstraint -> {
									//we add the foreign key just below it
									foreignKeyConstraints.remove(foreignKeyConstraint);
									builders.add(foreignKeyDefinitionBuilder(foreignKeyConstraint, entityType));
								});
			}
		});

		return builders;
	}

	private AttributeDefinition.Builder<?, ?> foreignKeyDefinitionBuilder(MetaDataForeignKeyConstraint foreignKeyConstraint, EntityType entityType) {
		MetaDataTable referencedTable = foreignKeyConstraint.referencedTable();
		EntityType referencedEntityType = tableEntityTypes.get(referencedTable);
		ForeignKey foreignKey = entityType.foreignKey(createForeignKeyName(foreignKeyConstraint) + "_FK",
						foreignKeyConstraint.references().entrySet().stream()
										.map(entry -> reference(column(entityType, entry.getKey()), column(referencedEntityType, entry.getValue())))
										.collect(toList()));

		return foreignKey.define().foreignKey().caption(caption(referencedTable.tableName().toLowerCase()));
	}

	private ColumnDefinition.Builder<?, ?> columnDefinitionBuilder(MetaDataColumn metadataColumn, EntityType entityType) {
		String caption = caption(metadataColumn.columnName());
		Column<?> column = column(entityType, metadataColumn);
		ColumnDefinition.Builder<?, ?> builder;
		if (metadataColumn.primaryKeyColumn()) {
			builder = column.define().primaryKey(metadataColumn.primaryKeyIndex() - 1);
		}
		else if (isAuditColumn(column)) {
			builder = auditColumnDefinitionBuilder(column)
							.caption(caption);
		}
		else {
			builder = column.define().column().caption(caption);
		}
		if (!metadataColumn.primaryKeyColumn() && metadataColumn.nullable() == DatabaseMetaData.columnNoNulls) {
			builder.nullable(false);
		}
		if (column.type().isString() && metadataColumn.columnSize() > 0 && metadataColumn.columnSize() < MAXIMUM_COLUMN_SIZE) {
			builder.maximumLength(metadataColumn.columnSize());
		}
		if (column.type().isDecimal() && metadataColumn.decimalDigits() >= 1) {
			builder.maximumFractionDigits(metadataColumn.decimalDigits());
		}
		if (!metadataColumn.primaryKeyColumn() && metadataColumn.defaultValue() != null) {
			builder.hasDatabaseDefault(true);
		}
		if (!nullOrEmpty(metadataColumn.comment())) {
			builder.description(metadataColumn.comment());
		}

		return builder;
	}

	private boolean isAuditColumn(Column<?> column) {
		return isAuditInsertUserColumn(column)
						|| isAuditInsertTimeColumn(column)
						|| isAuditUpdateUserColumn(column)
						|| isAuditUpdateTimeColumn(column);
	}

	private ColumnDefinition.Builder<?, ?> auditColumnDefinitionBuilder(Column<?> column) {
		if (isAuditInsertUserColumn(column)) {
			return column.define().auditColumn().insertUser();
		}
		if (isAuditInsertTimeColumn(column)) {
			return column.define().auditColumn().insertTime();
		}
		if (isAuditUpdateUserColumn(column)) {
			return column.define().auditColumn().updateUser();
		}
		if (isAuditUpdateTimeColumn(column)) {
			return column.define().auditColumn().updateTime();
		}

		throw new IllegalArgumentException("Unknown audit column type: " + column);
	}

	private boolean isAuditUpdateTimeColumn(Column<?> column) {
		return column.name().equalsIgnoreCase(settings.auditUpdateTimeColumnName().orElse(null));
	}

	private boolean isAuditUpdateUserColumn(Column<?> column) {
		return column.name().equalsIgnoreCase(settings.auditUpdateUserColumnName().orElse(null));
	}

	private boolean isAuditInsertTimeColumn(Column<?> column) {
		return column.name().equalsIgnoreCase(settings.auditInsertTimeColumnName().orElse(null));
	}

	private boolean isAuditInsertUserColumn(Column<?> column) {
		return column.name().equalsIgnoreCase(settings.auditInsertUserColumnName().orElse(null));
	}

	private static <T> Column<T> column(EntityType entityType, MetaDataColumn column) {
		return (Column<T>) entityType.column(column.columnName(), column.columnClass());
	}

	private static String caption(String name) {
		String caption = name.toLowerCase().replace("_", " ");

		return caption.substring(0, 1).toUpperCase() + caption.substring(1);
	}

	private static boolean lastKeyColumn(MetaDataForeignKeyConstraint foreignKeyConstraint, MetaDataColumn column) {
		return foreignKeyConstraint.references().keySet().stream()
						.mapToInt(MetaDataColumn::position)
						.max()
						.orElse(-1) == column.position();
	}

	private String createForeignKeyName(MetaDataForeignKeyConstraint foreignKeyConstraint) {
		return foreignKeyConstraint.references().keySet().stream()
						.map(MetaDataColumn::columnName)
						.map(String::toUpperCase)
						.map(this::removePrimaryKeyColumnSuffix)
						.collect(joining("_"));
	}

	private String removePrimaryKeyColumnSuffix(String columnName) {
		return settings.primaryKeyColumnSuffix()
						.map(suffix -> removeSuffix(columnName, suffix))
						.orElse(columnName);
	}

	private static String removeSuffix(String columnName, String suffix) {
		return columnName.toLowerCase().endsWith(suffix.toLowerCase()) ?
						columnName.substring(0, columnName.length() - suffix.length()) : columnName;
	}

	private static boolean tableHasAutoIncrementPrimaryKeyColumn(MetaDataTable table) {
		return table.columns().stream()
						.filter(MetaDataColumn::primaryKeyColumn)
						.anyMatch(MetaDataColumn::autoIncrement);
	}

	/**
	 * Specifies the settings used when deriving a domain model from a database schema.
	 * @see #builder()
	 */
	public interface SchemaSettings {

		Optional<String> primaryKeyColumnSuffix();

		Optional<String> auditInsertUserColumnName();

		Optional<String> auditInsertTimeColumnName();

		Optional<String> auditUpdateUserColumnName();

		Optional<String> auditUpdateTimeColumnName();

		/**
		 * @return a new builder
		 */
		static Builder builder() {
			return new DefaultSchemaSettings.DefaultBuilder();
		}

		/**
		 * Builds a {@link SchemaSettings} instance.
		 */
		interface Builder {

			Builder primaryKeyColumnSuffix(String primaryKeyColumnSuffix);

			Builder auditInsertUserColumnName(String auditInsertUserColumnName);

			Builder auditInsertTimeColumnName(String auditInsertTimeColumnName);

			Builder auditUpdateUserColumnName(String auditUpdateUserColumnName);

			Builder auditUpdateTimeColumnName(String auditUpdateTimeColumnName);

			SchemaSettings build();
		}
	}

	private static final class DefaultSchemaSettings implements SchemaSettings {

		private final String primaryKeyColumnSuffix;
		private final String auditInsertUserColumnName;
		private final String auditInsertTimeColumnName;
		private final String auditUpdateUserColumnName;
		private final String auditUpdateTimeColumnName;

		private DefaultSchemaSettings(DefaultBuilder builder) {
			this.primaryKeyColumnSuffix = builder.primaryKeyColumnSuffix;
			this.auditInsertUserColumnName = builder.auditInsertUserColumnName;
			this.auditInsertTimeColumnName = builder.auditInsertTimeColumnName;
			this.auditUpdateUserColumnName = builder.auditUpdateUserColumnName;
			this.auditUpdateTimeColumnName = builder.auditUpdateTimeColumnName;
		}

		@Override
		public Optional<String> primaryKeyColumnSuffix() {
			return Optional.ofNullable(primaryKeyColumnSuffix);
		}

		@Override
		public Optional<String> auditInsertUserColumnName() {
			return Optional.ofNullable(auditInsertUserColumnName);
		}

		@Override
		public Optional<String> auditInsertTimeColumnName() {
			return Optional.ofNullable(auditInsertTimeColumnName);
		}

		@Override
		public Optional<String> auditUpdateUserColumnName() {
			return Optional.ofNullable(auditUpdateUserColumnName);
		}

		@Override
		public Optional<String> auditUpdateTimeColumnName() {
			return Optional.ofNullable(auditUpdateTimeColumnName);
		}

		private static final class DefaultBuilder implements Builder {

			private String primaryKeyColumnSuffix;
			private String auditInsertUserColumnName;
			private String auditInsertTimeColumnName;
			private String auditUpdateUserColumnName;
			private String auditUpdateTimeColumnName;

			@Override
			public Builder primaryKeyColumnSuffix(String primaryKeyColumnSuffix) {
				this.primaryKeyColumnSuffix = requireNonNull(primaryKeyColumnSuffix);
				return this;
			}

			@Override
			public Builder auditInsertUserColumnName(String auditInsertUserColumnName) {
				this.auditInsertUserColumnName = requireNonNull(auditInsertUserColumnName);
				return this;
			}

			@Override
			public Builder auditInsertTimeColumnName(String auditInsertTimeColumnName) {
				this.auditInsertTimeColumnName = requireNonNull(auditInsertTimeColumnName);
				return this;
			}

			@Override
			public Builder auditUpdateUserColumnName(String auditUpdateUserColumnName) {
				this.auditUpdateUserColumnName = requireNonNull(auditUpdateUserColumnName);
				return this;
			}

			@Override
			public Builder auditUpdateTimeColumnName(String auditUpdateTimeColumnName) {
				this.auditUpdateTimeColumnName = requireNonNull(auditUpdateTimeColumnName);
				return this;
			}

			@Override
			public SchemaSettings build() {
				return new DefaultSchemaSettings(this);
			}
		}
	}
}
