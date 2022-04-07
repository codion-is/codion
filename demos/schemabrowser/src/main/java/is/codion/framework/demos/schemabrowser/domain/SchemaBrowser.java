/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.domain;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.query.SelectQuery;

import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;

public final class SchemaBrowser extends DefaultDomain {

  static final DomainType DOMAIN = domainType(SchemaBrowser.class);

  private static final ResourceBundle bundle;

  static {
    try {
      String databaseFactoryClassName = DatabaseFactory.databaseFactory().getClass().getName();
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
    column();
    constraint();
    constraintColumn();
  }

  public interface Schema {
    EntityType TYPE = DOMAIN.entityType("schema");

    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("schema_name"));
  }

  void schema() {
    define(Schema.TYPE, bundle.getString("t_schema"),
            primaryKeyProperty(Schema.NAME, "Name"))
            .orderBy(orderBy().ascending(Schema.NAME))
            .readOnly(true)
            .stringFactory(stringFactory(Schema.NAME))
            .caption("Schemas");
  }

  public interface Table {
    EntityType TYPE = DOMAIN.entityType("table");

    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("table_name"));
    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("table_schema"));

    ForeignKey SCHEMA_FK = TYPE.foreignKey(bundle.getString("table_schema_ref"), SCHEMA, Schema.NAME);
  }

  void table() {
    EntityDefinition.Builder tableBuilder = define(Table.TYPE, bundle.getString("t_table"),
            columnProperty(Table.SCHEMA)
                    .primaryKeyIndex(0),
            foreignKeyProperty(Table.SCHEMA_FK, "Schema"),
            columnProperty(Table.NAME, "Name")
                    .primaryKeyIndex(1))
            .orderBy(orderBy().ascending(Table.SCHEMA, Table.NAME))
            .readOnly(true)
            .stringFactory(stringFactory(Table.SCHEMA_FK).text(".").value(Table.NAME))
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
  }

  public interface Column {
    EntityType TYPE = DOMAIN.entityType("column");

    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("column_schema"));
    Attribute<String> TABLE_NAME = TYPE.stringAttribute(bundle.getString("column_table_name"));
    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("column_name"));
    Attribute<String> DATA_TYPE = TYPE.stringAttribute(bundle.getString("column_data_type"));

    ForeignKey TABLE_FK = TYPE.foreignKey(bundle.getString("column_table_ref"),
            Column.SCHEMA, Table.SCHEMA,
            Column.TABLE_NAME, Table.NAME);
  }

  void column() {
    define(Column.TYPE, bundle.getString("t_column"),
            columnProperty(Column.SCHEMA)
                    .primaryKeyIndex(0),
            columnProperty(Column.TABLE_NAME)
                    .primaryKeyIndex(1),
            foreignKeyProperty(Column.TABLE_FK, "Table")
                    .fetchDepth(2),
            primaryKeyProperty(Column.NAME, "Column name")
                    .primaryKeyIndex(2),
            columnProperty(Column.DATA_TYPE, "Data type"))
            .orderBy(orderBy().ascending(Column.SCHEMA, Column.TABLE_NAME, Column.NAME))
            .readOnly(true)
            .stringFactory(stringFactory(Column.TABLE_FK).text(".").value(Column.NAME))
            .caption("Columns");
  }

  public interface Constraint {
    EntityType TYPE = DOMAIN.entityType("constraint");

    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("constraint_schema"));
    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("constraint_name"));
    Attribute<String> CONSTRAINT_TYPE = TYPE.stringAttribute(bundle.getString("constraint_type"));
    Attribute<String> TABLE_NAME = TYPE.stringAttribute(bundle.getString("constraint_table_name"));

    ForeignKey TABLE_FK = TYPE.foreignKey(bundle.getString("constraint_table_ref"),
            Constraint.SCHEMA, Table.SCHEMA,
            Constraint.TABLE_NAME, Table.NAME);
  }

  void constraint() {
    define(Constraint.TYPE, bundle.getString("t_constraint"),
            columnProperty(Constraint.SCHEMA)
                    .primaryKeyIndex(0),
            columnProperty(Constraint.TABLE_NAME)
                    .primaryKeyIndex(1),
            foreignKeyProperty(Constraint.TABLE_FK, "Table")
                    .fetchDepth(2),
            primaryKeyProperty(Constraint.NAME, "Constraint name")
                    .primaryKeyIndex(2),
            columnProperty(Constraint.CONSTRAINT_TYPE, "Type"))
            .orderBy(orderBy().ascending(Constraint.SCHEMA, Constraint.TABLE_NAME, Constraint.NAME))
            .readOnly(true)
            .stringFactory(stringFactory(Constraint.TABLE_FK).text(".").value(Constraint.NAME))
            .caption("Constraints");
  }

  public interface ConstraintColumn {
    EntityType TYPE = DOMAIN.entityType("column_constraint");

    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("column_constraint_schema"));
    Attribute<String> CONSTRAINT_NAME = TYPE.stringAttribute(bundle.getString("column_constraint_constraint_name"));
    Attribute<String> TABLE_NAME = TYPE.stringAttribute(bundle.getString("column_constraint_table_name"));
    Attribute<String> COLUMN_NAME = TYPE.stringAttribute(bundle.getString("column_constraint_column_name"));
    Attribute<Integer> POSITION = TYPE.integerAttribute(bundle.getString("column_constraint_position"));

    ForeignKey CONSTRAINT_FK = TYPE.foreignKey(bundle.getString("column_constraint_constraint_ref"),
            ConstraintColumn.SCHEMA, Constraint.SCHEMA,
            ConstraintColumn.TABLE_NAME, Constraint.TABLE_NAME,
            ConstraintColumn.CONSTRAINT_NAME, Constraint.NAME);
  }

  void constraintColumn() {
    define(ConstraintColumn.TYPE, bundle.getString("t_column_constraint"),
            columnProperty(ConstraintColumn.SCHEMA)
                    .primaryKeyIndex(0),
            columnProperty(ConstraintColumn.TABLE_NAME)
                    .primaryKeyIndex(1),
            columnProperty(ConstraintColumn.CONSTRAINT_NAME)
                    .primaryKeyIndex(2),
            foreignKeyProperty(ConstraintColumn.CONSTRAINT_FK, "Constraint")
                    .fetchDepth(3),
            columnProperty(ConstraintColumn.COLUMN_NAME, "Column name"),
            columnProperty(ConstraintColumn.POSITION, "Position"))
            .orderBy(orderBy().ascending(ConstraintColumn.SCHEMA, ConstraintColumn.TABLE_NAME, ConstraintColumn.CONSTRAINT_NAME))
            .readOnly(true)
            .caption("Constraint columns");
  }
}
