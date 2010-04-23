/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.domain;

import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.StringProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import java.util.Locale;
import java.util.ResourceBundle;

public class SchemaBrowser {

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
    EntityRepository.add(new EntityDefinition(T_SCHEMA, bundle.getString("t_schema"),
            new Property.PrimaryKeyProperty(SCHEMA_NAME, Type.STRING, "Name"))
            .setOrderByClause(SCHEMA_NAME)
            .setReadOnly(true));

    Entity.setProxy(T_SCHEMA, new Entity.Proxy() {
      @Override
      public String toString(final Entity entity) {
        return entity.getStringValue(SCHEMA_NAME);
      }
    });

    EntityRepository.add(new EntityDefinition(T_TABLE, bundle.getString("t_table"),
            new Property.ForeignKeyProperty(TABLE_SCHEMA_FK, "Schema", T_SCHEMA,
                    new Property.PrimaryKeyProperty(TABLE_SCHEMA, Type.STRING).setIndex(0)),
            new Property.PrimaryKeyProperty(TABLE_NAME, Type.STRING, "Name").setIndex(1))
            .setOrderByClause(TABLE_SCHEMA + ", " + TABLE_NAME)
            .setReadOnly(true)
            .setStringProvider(new StringProvider<String, Object>(TABLE_SCHEMA_FK).addText(".").addValue(TABLE_NAME)));

    EntityRepository.add(new EntityDefinition(T_COLUMN, bundle.getString("t_column"),
            new Property.ForeignKeyProperty(COLUMN_TABLE_FK, "Table", T_TABLE,
                    new Property.PrimaryKeyProperty(COLUMN_SCHEMA, Type.STRING).setIndex(0),
                    new Property.PrimaryKeyProperty(COLUMN_TABLE_NAME, Type.STRING).setIndex(1)),
            new Property.PrimaryKeyProperty(COLUMN_NAME, Type.STRING, "Column name").setIndex(2),
            new Property(COLUMN_DATA_TYPE, Type.STRING, "Data type"))
            .setOrderByClause(COLUMN_SCHEMA + ", " + COLUMN_TABLE_NAME + ", " + COLUMN_NAME)
            .setReadOnly(true)
            .setStringProvider(new StringProvider<String, Object>(COLUMN_TABLE_FK).addText(".").addValue(COLUMN_NAME)));

    EntityRepository.add(new EntityDefinition(T_CONSTRAINT, bundle.getString("t_constraint"),
            new Property.ForeignKeyProperty(CONSTRAINT_TABLE_FK, "Table", T_TABLE,
                    new Property.PrimaryKeyProperty(CONSTRAINT_SCHEMA, Type.STRING).setIndex(0),
                    new Property.PrimaryKeyProperty(CONSTRAINT_TABLE_NAME, Type.STRING).setIndex(1)),
            new Property.PrimaryKeyProperty(CONSTRAINT_NAME, Type.STRING, "Constraint name").setIndex(2),
            new Property(CONSTRAINT_TYPE, Type.STRING, "Type"))
            .setOrderByClause(CONSTRAINT_SCHEMA + ", " + CONSTRAINT_TABLE_NAME + ", " + CONSTRAINT_NAME)
            .setReadOnly(true).setLargeDataset(true)
            .setStringProvider(new StringProvider<String, Object>(CONSTRAINT_TABLE_FK).addText(".").addValue(CONSTRAINT_NAME)));

    EntityRepository.add(new EntityDefinition(T_COLUMN_CONSTRAINT, bundle.getString("t_column_constraint"),
            new Property.ForeignKeyProperty(COLUMN_CONSTRAINT_CONSTRAINT_FK, "Constraint", T_CONSTRAINT,
                    new Property.PrimaryKeyProperty(COLUMN_CONSTRAINT_SCHEMA, Type.STRING).setIndex(0),
                    new Property.PrimaryKeyProperty(COLUMN_CONSTRAINT_TABLE_NAME, Type.STRING).setIndex(1),
                    new Property.PrimaryKeyProperty(COLUMN_CONSTRAINT_CONSTRAINT_NAME, Type.STRING).setIndex(2)),
            new Property(COLUMN_CONSTRAINT_COLUMN_NAME, Type.STRING, "Column name"),
            new Property(COLUMN_CONSTRAINT_POSITION, Type.INT, "Position"))
            .setOrderByClause(COLUMN_CONSTRAINT_SCHEMA + ", " + COLUMN_CONSTRAINT_TABLE_NAME + ", " + COLUMN_CONSTRAINT_CONSTRAINT_NAME)
            .setReadOnly(true));
  }
}
