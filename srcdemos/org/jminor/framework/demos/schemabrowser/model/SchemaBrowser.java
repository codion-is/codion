/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.model;

import org.jminor.common.db.Database;
import org.jminor.common.db.IdSource;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityProxy;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import java.util.Locale;
import java.util.ResourceBundle;

public class SchemaBrowser {

  private static final ResourceBundle bundle =
          ResourceBundle.getBundle("org.jminor.framework.demos.schemabrowser.model.SchemaBrowser",
                  new Locale(Database.get().getDatabaseType()));

  public static final String T_SCHEMA = bundle.getString("t_schema");
  public static final String SCHEMA_NAME = bundle.getString("schema_name");

  public static final String T_TABLE = bundle.getString("t_table");
  public static final String TABLE_SCHEMA = bundle.getString("table_schema");
  public static final String TABLE_SCHEMA_FK = bundle.getString("table_schema_ref");
  public static final String TABLE_NAME = bundle.getString("table_name");

  public static final String T_COLUMN = bundle.getString("t_column");
  public static final String COLUMN_SCHEMA = bundle.getString("column_schema");
  public static final String COLUMN_TABLE_NAME = bundle.getString("column_table_name");
  public static final String COLUMN_TABLE_FK = bundle.getString("column_table_ref");
  public static final String COLUMN_NAME = bundle.getString("column_name");
  public static final String COLUMN_DATA_TYPE = bundle.getString("column_data_type");

  public static final String T_CONSTRAINT = bundle.getString("t_constraint");
  public static final String CONSTRAINT_SCHEMA = bundle.getString("constraint_schema");
  public static final String CONSTRAINT_TABLE_NAME = bundle.getString("constraint_table_name");
  public static final String CONSTRAINT_TABLE_FK = bundle.getString("constraint_table_ref");
  public static final String CONSTRAINT_NAME = bundle.getString("constraint_name");
  public static final String CONSTRAINT_TYPE = bundle.getString("constraint_type");

  public static final String T_COLUMN_CONSTRAINT = bundle.getString("t_column_constraint");
  public static final String COLUMN_CONSTRAINT_SCHEMA = bundle.getString("column_constraint_schema");
  public static final String COLUMN_CONSTRAINT_CONSTRAINT_NAME = bundle.getString("column_constraint_constraint_name");
  public static final String COLUMN_CONSTRAINT_CONSTRAINT_FK = bundle.getString("column_constraint_constraint_ref");
  public static final String COLUMN_CONSTRAINT_TABLE_NAME = bundle.getString("column_constraint_table_name");
  public static final String COLUMN_CONSTRAINT_COLUMN_NAME = bundle.getString("column_constraint_column_name");
  public static final String COLUMN_CONSTRAINT_POSITION = bundle.getString("column_constraint_position");

  static {
    EntityRepository.initialize(T_SCHEMA,
            IdSource.NONE, null, SCHEMA_NAME, null, true,
            new Property.PrimaryKeyProperty(SCHEMA_NAME, Type.STRING, "Name"));

    EntityRepository.initialize(T_TABLE,
            IdSource.NONE, null, TABLE_SCHEMA + ", " + TABLE_NAME, null, true, true,
            new Property.ForeignKeyProperty(TABLE_SCHEMA_FK, "Schema", T_SCHEMA,
                    new Property.PrimaryKeyProperty(TABLE_SCHEMA, Type.STRING)),
            new Property.PrimaryKeyProperty(TABLE_NAME, Type.STRING, "Name", 1));

    EntityRepository.initialize(T_COLUMN,
            IdSource.NONE, null, COLUMN_SCHEMA + ", " + COLUMN_TABLE_NAME + ", " + COLUMN_NAME, null, true,
            new Property.ForeignKeyProperty(COLUMN_TABLE_FK, "Table", T_TABLE,
                    new Property.PrimaryKeyProperty(COLUMN_SCHEMA, Type.STRING, null, 0),
                    new Property.PrimaryKeyProperty(COLUMN_TABLE_NAME, Type.STRING, null, 1)),
            new Property.PrimaryKeyProperty(COLUMN_NAME, Type.STRING, "Column name", 2),
            new Property(COLUMN_DATA_TYPE, Type.STRING, "Data type"));

    EntityRepository.initialize(T_CONSTRAINT,
            IdSource.NONE, null, CONSTRAINT_SCHEMA + ", " + CONSTRAINT_TABLE_NAME + ", " + CONSTRAINT_NAME, null, true, true,
            new Property.ForeignKeyProperty(CONSTRAINT_TABLE_FK, "Table", T_TABLE,
                    new Property.PrimaryKeyProperty(CONSTRAINT_SCHEMA, Type.STRING, null, 0),
                    new Property.PrimaryKeyProperty(CONSTRAINT_TABLE_NAME, Type.STRING, null, 1)),
            new Property.PrimaryKeyProperty(CONSTRAINT_NAME, Type.STRING, "Constraint name", 2),
            new Property(CONSTRAINT_TYPE, Type.STRING, "Type"));

    EntityRepository.initialize(T_COLUMN_CONSTRAINT,
            IdSource.NONE, null, COLUMN_CONSTRAINT_SCHEMA + ", " + COLUMN_CONSTRAINT_TABLE_NAME + ", " + COLUMN_CONSTRAINT_CONSTRAINT_NAME,
            null, true,
            new Property.ForeignKeyProperty(COLUMN_CONSTRAINT_CONSTRAINT_FK, "Constraint", T_CONSTRAINT,
                    new Property.PrimaryKeyProperty(COLUMN_CONSTRAINT_SCHEMA, Type.STRING, null, 0),
                    new Property.PrimaryKeyProperty(COLUMN_CONSTRAINT_TABLE_NAME, Type.STRING, null, 1),
                    new Property.PrimaryKeyProperty(COLUMN_CONSTRAINT_CONSTRAINT_NAME, Type.STRING, null, 2)),
            new Property(COLUMN_CONSTRAINT_COLUMN_NAME, Type.STRING, "Column name"),
            new Property(COLUMN_CONSTRAINT_POSITION, Type.INT, "Position"));

    EntityProxy.setDefaultEntityProxy(new EntityProxy() {
      @Override
      public String toString(final Entity entity) {
        if (entity.is(T_COLUMN))
          return entity.getValueAsString(COLUMN_TABLE_FK) + "." + entity.getStringValue(COLUMN_NAME);
        else if (entity.is(T_CONSTRAINT))
          return entity.getValueAsString(CONSTRAINT_TABLE_FK) + "." + entity.getStringValue(CONSTRAINT_NAME);
        else if (entity.is(T_SCHEMA))
          return entity.getStringValue(SCHEMA_NAME);
        else if (entity.is(T_TABLE))
          return entity.getValueAsString(TABLE_SCHEMA_FK) + "." + entity.getStringValue(TABLE_NAME);

        return super.toString(entity);
      }
    });
  }
}
