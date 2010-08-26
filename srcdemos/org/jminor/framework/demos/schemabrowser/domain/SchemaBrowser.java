/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.domain;

import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.valuemap.StringProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import java.sql.Types;
import java.util.Locale;
import java.util.ResourceBundle;

public class SchemaBrowser {

  private SchemaBrowser() {}
  public static void init() {}

  private static final ResourceBundle bundle =
          ResourceBundle.getBundle("org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser",
                  new Locale(DatabaseProvider.getDatabaseType()));

  public static final String T_SCHEMA = "schema";
  public static final String SCHEMA_NAME = bundle.getString("schema_name");

  public static final String T_TABLE = "table";
  public static final String TABLE_SCHEMA = bundle.getString("table_schema");
  public static final String TABLE_SCHEMA_FK = bundle.getString("table_schema_ref");
  public static final String TABLE_NAME = bundle.getString("table_name");

  public static final String T_COLUMN = "column";
  public static final String COLUMN_SCHEMA = bundle.getString("column_schema");
  public static final String COLUMN_TABLE_NAME = bundle.getString("column_table_name");
  public static final String COLUMN_TABLE_FK = bundle.getString("column_table_ref");
  public static final String COLUMN_NAME = bundle.getString("column_name");
  public static final String COLUMN_DATA_TYPE = bundle.getString("column_data_type");

  public static final String T_CONSTRAINT = "constraint";
  public static final String CONSTRAINT_SCHEMA = bundle.getString("constraint_schema");
  public static final String CONSTRAINT_TABLE_NAME = bundle.getString("constraint_table_name");
  public static final String CONSTRAINT_TABLE_FK = bundle.getString("constraint_table_ref");
  public static final String CONSTRAINT_NAME = bundle.getString("constraint_name");
  public static final String CONSTRAINT_TYPE = bundle.getString("constraint_type");

  public static final String T_COLUMN_CONSTRAINT = "column_constraint";
  public static final String COLUMN_CONSTRAINT_SCHEMA = bundle.getString("column_constraint_schema");
  public static final String COLUMN_CONSTRAINT_CONSTRAINT_NAME = bundle.getString("column_constraint_constraint_name");
  public static final String COLUMN_CONSTRAINT_CONSTRAINT_FK = bundle.getString("column_constraint_constraint_ref");
  public static final String COLUMN_CONSTRAINT_TABLE_NAME = bundle.getString("column_constraint_table_name");
  public static final String COLUMN_CONSTRAINT_COLUMN_NAME = bundle.getString("column_constraint_column_name");
  public static final String COLUMN_CONSTRAINT_POSITION = bundle.getString("column_constraint_position");

  static {
    Entities.define(T_SCHEMA, bundle.getString("t_schema"),
            Properties.primaryKeyProperty(SCHEMA_NAME, Types.VARCHAR, "Name"))
            .setOrderByClause(SCHEMA_NAME)
            .setReadOnly(true)
            .setStringProvider(new StringProvider<String>(SCHEMA_NAME))
            .setCaption("Schemas");

    Entities.define(T_TABLE, bundle.getString("t_table"),
            Properties.foreignKeyProperty(TABLE_SCHEMA_FK, "Schema", T_SCHEMA,
                    Properties.primaryKeyProperty(TABLE_SCHEMA, Types.VARCHAR).setIndex(0)),
            Properties.primaryKeyProperty(TABLE_NAME, Types.VARCHAR, "Name").setIndex(1))
            .setOrderByClause(TABLE_SCHEMA + ", " + TABLE_NAME)
            .setReadOnly(true)
            .setStringProvider(new StringProvider<String>(TABLE_SCHEMA_FK).addText(".").addValue(TABLE_NAME))
            .setCaption("Tables");

    Entities.define(T_COLUMN, bundle.getString("t_column"),
            Properties.foreignKeyProperty(COLUMN_TABLE_FK, "Table", T_TABLE,
                    new Property.ColumnProperty[] {
                            Properties.primaryKeyProperty(COLUMN_SCHEMA, Types.VARCHAR).setIndex(0),
                            Properties.primaryKeyProperty(COLUMN_TABLE_NAME, Types.VARCHAR).setIndex(1)},
                    new String[] {TABLE_SCHEMA, TABLE_NAME}),
            Properties.primaryKeyProperty(COLUMN_NAME, Types.VARCHAR, "Column name").setIndex(2),
            Properties.columnProperty(COLUMN_DATA_TYPE, Types.VARCHAR, "Data type"))
            .setOrderByClause(COLUMN_SCHEMA + ", " + COLUMN_TABLE_NAME + ", " + COLUMN_NAME)
            .setReadOnly(true)
            .setStringProvider(new StringProvider<String>(COLUMN_TABLE_FK).addText(".").addValue(COLUMN_NAME))
            .setCaption("Columns");

    Entities.define(T_CONSTRAINT, bundle.getString("t_constraint"),
            Properties.foreignKeyProperty(CONSTRAINT_TABLE_FK, "Table", T_TABLE,
                    new Property.ColumnProperty[] {
                            Properties.primaryKeyProperty(CONSTRAINT_SCHEMA, Types.VARCHAR).setIndex(0),
                            Properties.primaryKeyProperty(CONSTRAINT_TABLE_NAME, Types.VARCHAR).setIndex(1)},
                    new String[] {TABLE_SCHEMA, TABLE_NAME}),
            Properties.primaryKeyProperty(CONSTRAINT_NAME, Types.VARCHAR, "Constraint name").setIndex(2),
            Properties.columnProperty(CONSTRAINT_TYPE, Types.VARCHAR, "Type"))
            .setOrderByClause(CONSTRAINT_SCHEMA + ", " + CONSTRAINT_TABLE_NAME + ", " + CONSTRAINT_NAME)
            .setReadOnly(true).setLargeDataset(true)
            .setStringProvider(new StringProvider<String>(CONSTRAINT_TABLE_FK).addText(".").addValue(CONSTRAINT_NAME))
            .setCaption("Constraints");

    Entities.define(T_COLUMN_CONSTRAINT, bundle.getString("t_column_constraint"),
            Properties.foreignKeyProperty(COLUMN_CONSTRAINT_CONSTRAINT_FK, "Constraint", T_CONSTRAINT,
                    new Property.ColumnProperty[] {
                            Properties.primaryKeyProperty(COLUMN_CONSTRAINT_SCHEMA, Types.VARCHAR).setIndex(0),
                            Properties.primaryKeyProperty(COLUMN_CONSTRAINT_TABLE_NAME, Types.VARCHAR).setIndex(1),
                            Properties.primaryKeyProperty(COLUMN_CONSTRAINT_CONSTRAINT_NAME, Types.VARCHAR).setIndex(2)},
                    new String[] {CONSTRAINT_SCHEMA, CONSTRAINT_TABLE_NAME, CONSTRAINT_NAME}),
            Properties.columnProperty(COLUMN_CONSTRAINT_COLUMN_NAME, Types.VARCHAR, "Column name"),
            Properties.columnProperty(COLUMN_CONSTRAINT_POSITION, Types.INTEGER, "Position"))
            .setOrderByClause(COLUMN_CONSTRAINT_SCHEMA + ", " + COLUMN_CONSTRAINT_TABLE_NAME + ", " + COLUMN_CONSTRAINT_CONSTRAINT_NAME)
            .setReadOnly(true)
            .setCaption("Column constraints");
  }
}
