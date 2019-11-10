/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.domain;

import org.jminor.common.db.Database;
import org.jminor.framework.domain.Domain;

import java.sql.Types;
import java.util.Locale;
import java.util.ResourceBundle;

import static java.util.Arrays.asList;
import static org.jminor.framework.domain.Properties.*;

public final class SchemaBrowser extends Domain {

  public SchemaBrowser() {
    defineSchema();
    defineTable();
    defineColumn();
    defineConstraint();
    defineColumnConstraint();
  }

  private static final ResourceBundle bundle =
          ResourceBundle.getBundle("org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser",
                  new Locale(Database.getDatabaseType().toString().toLowerCase()));

  public static final String T_SCHEMA = "schema";
  public static final String SCHEMA_NAME = bundle.getString("schema_name");

  void defineSchema() {
    define(T_SCHEMA, bundle.getString("t_schema"),
            primaryKeyProperty(SCHEMA_NAME, Types.VARCHAR, "Name"))
            .setOrderBy(orderBy().ascending(SCHEMA_NAME))
            .setReadOnly(true)
            .setStringProvider(new StringProvider(SCHEMA_NAME))
            .setCaption("Schemas");
  }

  public static final String T_TABLE = "table";
  public static final String TABLE_SCHEMA = bundle.getString("table_schema");
  public static final String TABLE_SCHEMA_FK = bundle.getString("table_schema_ref");
  public static final String TABLE_NAME = bundle.getString("table_name");

  void defineTable() {
    define(T_TABLE, bundle.getString("t_table"),
            foreignKeyProperty(TABLE_SCHEMA_FK, "Schema", T_SCHEMA,
                    primaryKeyProperty(TABLE_SCHEMA, Types.VARCHAR).setPrimaryKeyIndex(0)),
            primaryKeyProperty(TABLE_NAME, Types.VARCHAR, "Name").setPrimaryKeyIndex(1))
            .setOrderBy(orderBy().ascending(TABLE_SCHEMA, TABLE_NAME))
            .setReadOnly(true)
            .setStringProvider(new StringProvider(TABLE_SCHEMA_FK).addText(".").addValue(TABLE_NAME))
            .setCaption("Tables");
  }

  public static final String T_COLUMN = "column";
  public static final String COLUMN_SCHEMA = bundle.getString("column_schema");
  public static final String COLUMN_TABLE_NAME = bundle.getString("column_table_name");
  public static final String COLUMN_TABLE_FK = bundle.getString("column_table_ref");
  public static final String COLUMN_NAME = bundle.getString("column_name");
  public static final String COLUMN_DATA_TYPE = bundle.getString("column_data_type");

  void defineColumn() {
    define(T_COLUMN, bundle.getString("t_column"),
            foreignKeyProperty(COLUMN_TABLE_FK, "Table", T_TABLE,
                    asList(columnProperty(COLUMN_SCHEMA, Types.VARCHAR).setPrimaryKeyIndex(0),
                            columnProperty(COLUMN_TABLE_NAME, Types.VARCHAR).setPrimaryKeyIndex(1))),
            primaryKeyProperty(COLUMN_NAME, Types.VARCHAR, "Column name").setPrimaryKeyIndex(2),
            columnProperty(COLUMN_DATA_TYPE, Types.VARCHAR, "Data type"))
            .setOrderBy(orderBy().ascending(COLUMN_SCHEMA, COLUMN_TABLE_NAME, COLUMN_NAME))
            .setReadOnly(true)
            .setStringProvider(new StringProvider(COLUMN_TABLE_FK).addText(".").addValue(COLUMN_NAME))
            .setCaption("Columns");
  }

  public static final String T_CONSTRAINT = "constraint";
  public static final String CONSTRAINT_SCHEMA = bundle.getString("constraint_schema");
  public static final String CONSTRAINT_TABLE_NAME = bundle.getString("constraint_table_name");
  public static final String CONSTRAINT_TABLE_FK = bundle.getString("constraint_table_ref");
  public static final String CONSTRAINT_NAME = bundle.getString("constraint_name");
  public static final String CONSTRAINT_TYPE = bundle.getString("constraint_type");

  void defineConstraint() {
    define(T_CONSTRAINT, bundle.getString("t_constraint"),
            foreignKeyProperty(CONSTRAINT_TABLE_FK, "Table", T_TABLE,
                    asList(columnProperty(CONSTRAINT_SCHEMA, Types.VARCHAR).setPrimaryKeyIndex(0),
                            columnProperty(CONSTRAINT_TABLE_NAME, Types.VARCHAR).setPrimaryKeyIndex(1))),
            primaryKeyProperty(CONSTRAINT_NAME, Types.VARCHAR, "Constraint name").setPrimaryKeyIndex(2),
            columnProperty(CONSTRAINT_TYPE, Types.VARCHAR, "Type"))
            .setOrderBy(orderBy().ascending(CONSTRAINT_SCHEMA, CONSTRAINT_TABLE_NAME, CONSTRAINT_NAME))
            .setReadOnly(true)
            .setStringProvider(new StringProvider(CONSTRAINT_TABLE_FK).addText(".").addValue(CONSTRAINT_NAME))
            .setCaption("Constraints");
  }

  public static final String T_COLUMN_CONSTRAINT = "column_constraint";
  public static final String COLUMN_CONSTRAINT_SCHEMA = bundle.getString("column_constraint_schema");
  public static final String COLUMN_CONSTRAINT_CONSTRAINT_NAME = bundle.getString("column_constraint_constraint_name");
  public static final String COLUMN_CONSTRAINT_CONSTRAINT_FK = bundle.getString("column_constraint_constraint_ref");
  public static final String COLUMN_CONSTRAINT_TABLE_NAME = bundle.getString("column_constraint_table_name");
  public static final String COLUMN_CONSTRAINT_COLUMN_NAME = bundle.getString("column_constraint_column_name");
  public static final String COLUMN_CONSTRAINT_POSITION = bundle.getString("column_constraint_position");

  void defineColumnConstraint() {
    define(T_COLUMN_CONSTRAINT, bundle.getString("t_column_constraint"),
            foreignKeyProperty(COLUMN_CONSTRAINT_CONSTRAINT_FK, "Constraint", T_CONSTRAINT,
                    asList(columnProperty(COLUMN_CONSTRAINT_SCHEMA, Types.VARCHAR).setPrimaryKeyIndex(0),
                            columnProperty(COLUMN_CONSTRAINT_TABLE_NAME, Types.VARCHAR).setPrimaryKeyIndex(1),
                            columnProperty(COLUMN_CONSTRAINT_CONSTRAINT_NAME, Types.VARCHAR).setPrimaryKeyIndex(2))),
            columnProperty(COLUMN_CONSTRAINT_COLUMN_NAME, Types.VARCHAR, "Column name"),
            columnProperty(COLUMN_CONSTRAINT_POSITION, Types.INTEGER, "Position"))
            .setOrderBy(orderBy().ascending(COLUMN_CONSTRAINT_SCHEMA, COLUMN_CONSTRAINT_TABLE_NAME, COLUMN_CONSTRAINT_CONSTRAINT_NAME))
            .setReadOnly(true)
            .setCaption("Column constraints");
  }
}
