/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.domain;

import org.jminor.common.db.database.DatabaseProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.StringProvider;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;
import java.util.ResourceBundle;

import static java.util.Arrays.asList;
import static org.jminor.framework.domain.entity.OrderBy.orderBy;
import static org.jminor.framework.domain.property.Properties.*;

public final class SchemaBrowser extends Domain {

  public SchemaBrowser() {
    defineSchema();
    defineTable();
    defineColumn();
    defineConstraint();
    defineColumnConstraint();
  }

  private static final ResourceBundle bundle;

  static {
    try {
      final String databaseClassName = DatabaseProvider.getInstance().getDatabaseClassName();
      bundle = ResourceBundle.getBundle(SchemaBrowser.class.getName(),
              new Locale(databaseClassName.substring(databaseClassName.lastIndexOf('.') + 1)));
    }
    catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static final String T_SCHEMA = "schema";
  public static final String SCHEMA_NAME = bundle.getString("schema_name");

  void defineSchema() {
    define(T_SCHEMA, bundle.getString("t_schema"),
            primaryKeyProperty(SCHEMA_NAME, Types.VARCHAR, "Name"))
            .orderBy(orderBy().ascending(SCHEMA_NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(SCHEMA_NAME))
            .caption("Schemas");
  }

  public static final String T_TABLE = "table";
  public static final String TABLE_SCHEMA = bundle.getString("table_schema");
  public static final String TABLE_SCHEMA_FK = bundle.getString("table_schema_ref");
  public static final String TABLE_NAME = bundle.getString("table_name");

  void defineTable() {
    define(T_TABLE, bundle.getString("t_table"),
            foreignKeyProperty(TABLE_SCHEMA_FK, "Schema", T_SCHEMA,
                    primaryKeyProperty(TABLE_SCHEMA, Types.VARCHAR).primaryKeyIndex(0)),
            primaryKeyProperty(TABLE_NAME, Types.VARCHAR, "Name").primaryKeyIndex(1))
            .orderBy(orderBy().ascending(TABLE_SCHEMA, TABLE_NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(TABLE_SCHEMA_FK).addText(".").addValue(TABLE_NAME))
            .caption("Tables");
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
                    asList(columnProperty(COLUMN_SCHEMA, Types.VARCHAR).primaryKeyIndex(0),
                            columnProperty(COLUMN_TABLE_NAME, Types.VARCHAR).primaryKeyIndex(1))),
            primaryKeyProperty(COLUMN_NAME, Types.VARCHAR, "Column name").primaryKeyIndex(2),
            columnProperty(COLUMN_DATA_TYPE, Types.VARCHAR, "Data type"))
            .orderBy(orderBy().ascending(COLUMN_SCHEMA, COLUMN_TABLE_NAME, COLUMN_NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(COLUMN_TABLE_FK).addText(".").addValue(COLUMN_NAME))
            .caption("Columns");
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
                    asList(columnProperty(CONSTRAINT_SCHEMA, Types.VARCHAR).primaryKeyIndex(0),
                            columnProperty(CONSTRAINT_TABLE_NAME, Types.VARCHAR).primaryKeyIndex(1))),
            primaryKeyProperty(CONSTRAINT_NAME, Types.VARCHAR, "Constraint name").primaryKeyIndex(2),
            columnProperty(CONSTRAINT_TYPE, Types.VARCHAR, "Type"))
            .orderBy(orderBy().ascending(CONSTRAINT_SCHEMA, CONSTRAINT_TABLE_NAME, CONSTRAINT_NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(CONSTRAINT_TABLE_FK).addText(".").addValue(CONSTRAINT_NAME))
            .caption("Constraints");
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
                    asList(columnProperty(COLUMN_CONSTRAINT_SCHEMA, Types.VARCHAR).primaryKeyIndex(0),
                            columnProperty(COLUMN_CONSTRAINT_TABLE_NAME, Types.VARCHAR).primaryKeyIndex(1),
                            columnProperty(COLUMN_CONSTRAINT_CONSTRAINT_NAME, Types.VARCHAR).primaryKeyIndex(2))),
            columnProperty(COLUMN_CONSTRAINT_COLUMN_NAME, Types.VARCHAR, "Column name"),
            columnProperty(COLUMN_CONSTRAINT_POSITION, Types.INTEGER, "Position"))
            .orderBy(orderBy().ascending(COLUMN_CONSTRAINT_SCHEMA, COLUMN_CONSTRAINT_TABLE_NAME, COLUMN_CONSTRAINT_CONSTRAINT_NAME))
            .readOnly(true)
            .caption("Column constraints");
  }
}
