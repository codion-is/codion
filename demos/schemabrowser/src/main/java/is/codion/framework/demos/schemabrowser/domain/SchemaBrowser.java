/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.domain;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;

import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static java.sql.Types.INTEGER;
import static java.sql.Types.VARCHAR;
import static java.util.Arrays.asList;

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
      final String databaseFactoryClassName = DatabaseFactory.getInstance().getClass().getName();
      bundle = ResourceBundle.getBundle(SchemaBrowser.class.getName(),
              new Locale(databaseFactoryClassName.substring(databaseFactoryClassName.lastIndexOf('.') + 1)));
    }
    catch (final SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static final String T_SCHEMA = "schema";
  public static final Attribute<String> SCHEMA_NAME = attribute(bundle.getString("schema_name"), VARCHAR);

  void defineSchema() {
    define(T_SCHEMA, bundle.getString("t_schema"),
            primaryKeyProperty(SCHEMA_NAME, "Name"))
            .orderBy(orderBy().ascending(SCHEMA_NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(SCHEMA_NAME))
            .caption("Schemas");
  }

  public static final String T_TABLE = "table";
  public static final Attribute<String> TABLE_SCHEMA = attribute(bundle.getString("table_schema"), VARCHAR);
  public static final EntityAttribute TABLE_SCHEMA_FK = entityAttribute(bundle.getString("table_schema_ref"));
  public static final Attribute<String> TABLE_NAME = attribute(bundle.getString("table_name"), VARCHAR);

  void defineTable() {
    define(T_TABLE, bundle.getString("t_table"),
            foreignKeyProperty(TABLE_SCHEMA_FK, "Schema", T_SCHEMA,
                    primaryKeyProperty(TABLE_SCHEMA).primaryKeyIndex(0)),
            primaryKeyProperty(TABLE_NAME, "Name").primaryKeyIndex(1))
            .orderBy(orderBy().ascending(TABLE_SCHEMA, TABLE_NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(TABLE_SCHEMA_FK).addText(".").addValue(TABLE_NAME))
            .caption("Tables");
  }

  public static final String T_COLUMN = "column";
  public static final Attribute<String> COLUMN_SCHEMA = attribute(bundle.getString("column_schema"), VARCHAR);
  public static final Attribute<String> COLUMN_TABLE_NAME = attribute(bundle.getString("column_table_name"), VARCHAR);
  public static final EntityAttribute COLUMN_TABLE_FK = entityAttribute(bundle.getString("column_table_ref"));
  public static final Attribute<String> COLUMN_NAME = attribute(bundle.getString("column_name"), VARCHAR);
  public static final Attribute<String> COLUMN_DATA_TYPE = attribute(bundle.getString("column_data_type"), VARCHAR);

  void defineColumn() {
    define(T_COLUMN, bundle.getString("t_column"),
            foreignKeyProperty(COLUMN_TABLE_FK, "Table", T_TABLE,
                    asList(columnProperty(COLUMN_SCHEMA).primaryKeyIndex(0),
                            columnProperty(COLUMN_TABLE_NAME).primaryKeyIndex(1))),
            primaryKeyProperty(COLUMN_NAME, "Column name").primaryKeyIndex(2),
            columnProperty(COLUMN_DATA_TYPE, "Data type"))
            .orderBy(orderBy().ascending(COLUMN_SCHEMA, COLUMN_TABLE_NAME, COLUMN_NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(COLUMN_TABLE_FK).addText(".").addValue(COLUMN_NAME))
            .caption("Columns");
  }

  public static final String T_CONSTRAINT = "constraint";
  public static final Attribute<String> CONSTRAINT_SCHEMA = attribute(bundle.getString("constraint_schema"), VARCHAR);
  public static final Attribute<String> CONSTRAINT_TABLE_NAME = attribute(bundle.getString("constraint_table_name"), VARCHAR);
  public static final EntityAttribute CONSTRAINT_TABLE_FK = entityAttribute(bundle.getString("constraint_table_ref"));
  public static final Attribute<String> CONSTRAINT_NAME = attribute(bundle.getString("constraint_name"), VARCHAR);
  public static final Attribute<String> CONSTRAINT_TYPE = attribute(bundle.getString("constraint_type"), VARCHAR);

  void defineConstraint() {
    define(T_CONSTRAINT, bundle.getString("t_constraint"),
            foreignKeyProperty(CONSTRAINT_TABLE_FK, "Table", T_TABLE,
                    asList(columnProperty(CONSTRAINT_SCHEMA).primaryKeyIndex(0),
                            columnProperty(CONSTRAINT_TABLE_NAME).primaryKeyIndex(1))),
            primaryKeyProperty(CONSTRAINT_NAME, "Constraint name").primaryKeyIndex(2),
            columnProperty(CONSTRAINT_TYPE, "Type"))
            .orderBy(orderBy().ascending(CONSTRAINT_SCHEMA, CONSTRAINT_TABLE_NAME, CONSTRAINT_NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(CONSTRAINT_TABLE_FK).addText(".").addValue(CONSTRAINT_NAME))
            .caption("Constraints");
  }

  public static final String T_COLUMN_CONSTRAINT = "column_constraint";
  public static final Attribute<String> COLUMN_CONSTRAINT_SCHEMA = attribute(bundle.getString("column_constraint_schema"), VARCHAR);
  public static final Attribute<String> COLUMN_CONSTRAINT_CONSTRAINT_NAME = attribute(bundle.getString("column_constraint_constraint_name"), VARCHAR);
  public static final EntityAttribute COLUMN_CONSTRAINT_CONSTRAINT_FK = entityAttribute(bundle.getString("column_constraint_constraint_ref"));
  public static final Attribute<String> COLUMN_CONSTRAINT_TABLE_NAME = attribute(bundle.getString("column_constraint_table_name"), VARCHAR);
  public static final Attribute<String> COLUMN_CONSTRAINT_COLUMN_NAME = attribute(bundle.getString("column_constraint_column_name"), VARCHAR);
  public static final Attribute<Integer> COLUMN_CONSTRAINT_POSITION = attribute(bundle.getString("column_constraint_position"), INTEGER);

  void defineColumnConstraint() {
    define(T_COLUMN_CONSTRAINT, bundle.getString("t_column_constraint"),
            foreignKeyProperty(COLUMN_CONSTRAINT_CONSTRAINT_FK, "Constraint", T_CONSTRAINT,
                    asList(columnProperty(COLUMN_CONSTRAINT_SCHEMA).primaryKeyIndex(0),
                            columnProperty(COLUMN_CONSTRAINT_TABLE_NAME).primaryKeyIndex(1),
                            columnProperty(COLUMN_CONSTRAINT_CONSTRAINT_NAME).primaryKeyIndex(2))),
            columnProperty(COLUMN_CONSTRAINT_COLUMN_NAME, "Column name"),
            columnProperty(COLUMN_CONSTRAINT_POSITION, "Position"))
            .orderBy(orderBy().ascending(COLUMN_CONSTRAINT_SCHEMA, COLUMN_CONSTRAINT_TABLE_NAME, COLUMN_CONSTRAINT_CONSTRAINT_NAME))
            .readOnly(true)
            .caption("Column constraints");
  }
}
