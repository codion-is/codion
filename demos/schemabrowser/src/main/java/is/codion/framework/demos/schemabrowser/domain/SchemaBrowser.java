/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.domain;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.query.SelectQuery;

import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.OrderBy.ascending;

public final class SchemaBrowser extends DefaultDomain {

  public static final DomainType DOMAIN = domainType(SchemaBrowser.class);

  private static final ResourceBundle bundle;

  static {
    try {
      String databaseFactoryClassName = DatabaseFactory.instance().getClass().getName();
      bundle = ResourceBundle.getBundle(SchemaBrowser.class.getName(),
              new Locale(databaseFactoryClassName.substring(databaseFactoryClassName.lastIndexOf('.') + 1)));
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public SchemaBrowser() {
    super(DOMAIN);
    schema();
    table();
    tableColumn();
    constraint();
    constraintColumn();
  }

  public interface Schema {
    EntityType TYPE = DOMAIN.entityType("schema");

    Column<String> NAME = TYPE.stringColumn(bundle.getString("schema_name"));
  }

  void schema() {
    add(Schema.TYPE.define(Schema.NAME.primaryKey()
                    .caption("Name"))
            .tableName(bundle.getString("t_schema"))
            .orderBy(ascending(Schema.NAME))
            .readOnly(true)
            .stringFactory(Schema.NAME)
            .caption("Schemas"));
  }

  public interface Table {
    EntityType TYPE = DOMAIN.entityType("table");

    Column<String> NAME = TYPE.stringColumn(bundle.getString("table_name"));
    Column<String> SCHEMA = TYPE.stringColumn(bundle.getString("table_schema"));

    ForeignKey SCHEMA_FK = TYPE.foreignKey(bundle.getString("table_schema_ref"), SCHEMA, Schema.NAME);
  }

  void table() {
    EntityDefinition.Builder tableBuilder = Table.TYPE.define(
            Table.SCHEMA.column()
                    .primaryKeyIndex(0),
            Table.SCHEMA_FK.foreignKey()
                    .caption("Schema"),
            Table.NAME.column()
                    .caption("Name")
                    .primaryKeyIndex(1))
            .tableName(bundle.getString("t_table"))
            .orderBy(ascending(Table.SCHEMA, Table.NAME))
            .readOnly(true)
            .stringFactory(StringFactory.builder()
                    .value(Table.SCHEMA_FK)
                    .text(".")
                    .value(Table.NAME)
                    .build())
            .caption("Tables");
    String tableQueryFrom = bundle.getString("t_table_query_from");
    if (!tableQueryFrom.isEmpty()) {
      String tableQueryColumns = bundle.getString("t_table_query_columns");
      String tableQueryWhere = bundle.getString("t_table_query_where");
      tableBuilder.selectQuery(SelectQuery.builder()
              .columns(tableQueryColumns)
              .from(tableQueryFrom)
              .where(tableQueryWhere)
              .build());
    }
    add(tableBuilder);
  }

  public interface TableColumn {
    EntityType TYPE = DOMAIN.entityType("column");

    Column<String> SCHEMA = TYPE.stringColumn(bundle.getString("column_schema"));
    Column<String> TABLE_NAME = TYPE.stringColumn(bundle.getString("column_table_name"));
    Column<String> NAME = TYPE.stringColumn(bundle.getString("column_name"));
    Column<String> DATA_TYPE = TYPE.stringColumn(bundle.getString("column_data_type"));

    ForeignKey TABLE_FK = TYPE.foreignKey(bundle.getString("column_table_ref"),
            TableColumn.SCHEMA, Table.SCHEMA,
            TableColumn.TABLE_NAME, Table.NAME);
  }

  void tableColumn() {
    add(TableColumn.TYPE.define(
            TableColumn.SCHEMA.column()
                    .primaryKeyIndex(0),
            TableColumn.TABLE_NAME.column()
                    .primaryKeyIndex(1),
            TableColumn.TABLE_FK.foreignKey()
                    .caption("Table")
                    .fetchDepth(2),
            TableColumn.NAME.column()
                    .caption("Column name")
                    .primaryKeyIndex(2),
            TableColumn.DATA_TYPE.column()
                    .caption("Data type"))
            .tableName(bundle.getString("t_column"))
            .orderBy(ascending(TableColumn.SCHEMA, TableColumn.TABLE_NAME, TableColumn.NAME))
            .readOnly(true)
            .stringFactory(StringFactory.builder()
                    .value(TableColumn.TABLE_FK)
                    .text(".")
                    .value(TableColumn.NAME)
                    .build())
            .caption("Columns"));
  }

  public interface Constraint {
    EntityType TYPE = DOMAIN.entityType("constraint");

    Column<String> SCHEMA = TYPE.stringColumn(bundle.getString("constraint_schema"));
    Column<String> NAME = TYPE.stringColumn(bundle.getString("constraint_name"));
    Column<String> CONSTRAINT_TYPE = TYPE.stringColumn(bundle.getString("constraint_type"));
    Column<String> TABLE_NAME = TYPE.stringColumn(bundle.getString("constraint_table_name"));

    ForeignKey TABLE_FK = TYPE.foreignKey(bundle.getString("constraint_table_ref"),
            Constraint.SCHEMA, Table.SCHEMA,
            Constraint.TABLE_NAME, Table.NAME);
  }

  void constraint() {
    add(Constraint.TYPE.define(
            Constraint.SCHEMA.column()
                    .primaryKeyIndex(0),
            Constraint.TABLE_NAME.column()
                    .primaryKeyIndex(1),
            Constraint.TABLE_FK.foreignKey()
                    .caption("Table")
                    .fetchDepth(2),
            Constraint.NAME.column()
                    .caption("Constraint name")
                    .primaryKeyIndex(2),
            Constraint.CONSTRAINT_TYPE.column()
                    .caption("Type"))
            .tableName(bundle.getString("t_constraint"))
            .orderBy(ascending(Constraint.SCHEMA, Constraint.TABLE_NAME, Constraint.NAME))
            .readOnly(true)
            .stringFactory(StringFactory.builder()
                    .value(Constraint.TABLE_FK)
                    .text(".")
                    .value(Constraint.NAME)
                    .build())
            .caption("Constraints"));
  }

  public interface ConstraintColumn {
    EntityType TYPE = DOMAIN.entityType("column_constraint");

    Column<String> SCHEMA = TYPE.stringColumn(bundle.getString("column_constraint_schema"));
    Column<String> CONSTRAINT_NAME = TYPE.stringColumn(bundle.getString("column_constraint_constraint_name"));
    Column<String> TABLE_NAME = TYPE.stringColumn(bundle.getString("column_constraint_table_name"));
    Column<String> COLUMN_NAME = TYPE.stringColumn(bundle.getString("column_constraint_column_name"));
    Column<Integer> POSITION = TYPE.integerColumn(bundle.getString("column_constraint_position"));

    ForeignKey CONSTRAINT_FK = TYPE.foreignKey(bundle.getString("column_constraint_constraint_ref"),
            ConstraintColumn.SCHEMA, Constraint.SCHEMA,
            ConstraintColumn.TABLE_NAME, Constraint.TABLE_NAME,
            ConstraintColumn.CONSTRAINT_NAME, Constraint.NAME);
  }

  void constraintColumn() {
    add(ConstraintColumn.TYPE.define(
            ConstraintColumn.SCHEMA.column()
                    .primaryKeyIndex(0),
            ConstraintColumn.TABLE_NAME.column()
                    .primaryKeyIndex(1),
            ConstraintColumn.CONSTRAINT_NAME.column()
                    .primaryKeyIndex(2),
            ConstraintColumn.CONSTRAINT_FK.foreignKey()
                    .caption("Constraint")
                    .fetchDepth(3),
            ConstraintColumn.COLUMN_NAME.column()
                    .caption("Column name"),
            ConstraintColumn.POSITION.column()
                    .caption("Position"))
            .tableName(bundle.getString("t_column_constraint"))
            .orderBy(ascending(ConstraintColumn.SCHEMA, ConstraintColumn.TABLE_NAME, ConstraintColumn.CONSTRAINT_NAME))
            .readOnly(true)
            .caption("Constraint columns"));
  }
}
