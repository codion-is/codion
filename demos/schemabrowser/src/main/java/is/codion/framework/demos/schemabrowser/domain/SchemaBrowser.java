/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.domain;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityIdentity;
import is.codion.framework.domain.entity.StringProvider;

import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.domain.entity.Entities.entityIdentity;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
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

  public static final EntityIdentity T_SCHEMA = entityIdentity("schema");
  public static final Attribute<String> SCHEMA_NAME = T_SCHEMA.stringAttribute(bundle.getString("schema_name"));

  void defineSchema() {
    define(T_SCHEMA, bundle.getString("t_schema"),
            primaryKeyProperty(SCHEMA_NAME, "Name"))
            .orderBy(orderBy().ascending(SCHEMA_NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(SCHEMA_NAME))
            .caption("Schemas");
  }

  public static final EntityIdentity T_TABLE = entityIdentity("table");
  public static final Attribute<String> TABLE_SCHEMA = T_TABLE.stringAttribute(bundle.getString("table_schema"));
  public static final Attribute<Entity> TABLE_SCHEMA_FK = T_TABLE.entityAttribute(bundle.getString("table_schema_ref"));
  public static final Attribute<String> TABLE_NAME = T_TABLE.stringAttribute(bundle.getString("table_name"));

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

  public static final EntityIdentity T_COLUMN = entityIdentity("column");
  public static final Attribute<String> COLUMN_SCHEMA = T_COLUMN.stringAttribute(bundle.getString("column_schema"));
  public static final Attribute<String> COLUMN_TABLE_NAME = T_COLUMN.stringAttribute(bundle.getString("column_table_name"));
  public static final Attribute<Entity> COLUMN_TABLE_FK = T_COLUMN.entityAttribute(bundle.getString("column_table_ref"));
  public static final Attribute<String> COLUMN_NAME = T_COLUMN.stringAttribute(bundle.getString("column_name"));
  public static final Attribute<String> COLUMN_DATA_TYPE = T_COLUMN.stringAttribute(bundle.getString("column_data_type"));

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

  public static final EntityIdentity T_CONSTRAINT = entityIdentity("constraint");
  public static final Attribute<String> CONSTRAINT_SCHEMA = T_CONSTRAINT.stringAttribute(bundle.getString("constraint_schema"));
  public static final Attribute<String> CONSTRAINT_TABLE_NAME = T_CONSTRAINT.stringAttribute(bundle.getString("constraint_table_name"));
  public static final Attribute<Entity> CONSTRAINT_TABLE_FK = T_CONSTRAINT.entityAttribute(bundle.getString("constraint_table_ref"));
  public static final Attribute<String> CONSTRAINT_NAME = T_CONSTRAINT.stringAttribute(bundle.getString("constraint_name"));
  public static final Attribute<String> CONSTRAINT_TYPE = T_CONSTRAINT.stringAttribute(bundle.getString("constraint_type"));

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

  public static final EntityIdentity T_COLUMN_CONSTRAINT = entityIdentity("column_constraint");
  public static final Attribute<String> COLUMN_CONSTRAINT_SCHEMA = T_COLUMN_CONSTRAINT.stringAttribute(bundle.getString("column_constraint_schema"));
  public static final Attribute<String> COLUMN_CONSTRAINT_CONSTRAINT_NAME = T_COLUMN_CONSTRAINT.stringAttribute(bundle.getString("column_constraint_constraint_name"));
  public static final Attribute<Entity> COLUMN_CONSTRAINT_CONSTRAINT_FK = T_COLUMN_CONSTRAINT.entityAttribute(bundle.getString("column_constraint_constraint_ref"));
  public static final Attribute<String> COLUMN_CONSTRAINT_TABLE_NAME = T_COLUMN_CONSTRAINT.stringAttribute(bundle.getString("column_constraint_table_name"));
  public static final Attribute<String> COLUMN_CONSTRAINT_COLUMN_NAME = T_COLUMN_CONSTRAINT.stringAttribute(bundle.getString("column_constraint_column_name"));
  public static final Attribute<Integer> COLUMN_CONSTRAINT_POSITION = T_COLUMN_CONSTRAINT.integerAttribute(bundle.getString("column_constraint_position"));

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
