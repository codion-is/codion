/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.model;

import org.jminor.common.db.Database;
import org.jminor.common.db.IdSource;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityProxy;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

public class SchemaBrowser {

  public static final String T_SCHEMA = Database.isOracle() ? "all_users" : "information_schema.schemata";

  public static final String SCHEMA_NAME = Database.isOracle() ? "username" : "schema_name";

  public static final String T_TABLE = Database.isOracle() ? "all_objects where upper(object_type) = upper('table')" : "information_schema.tables";

  public static final String TABLE_SCHEMA = Database.isOracle() ? "owner" : "table_schema";
  public static final String TABLE_SCHEMA_REF = "schema_ref";
  public static final String TABLE_NAME = Database.isOracle() ? "object_name" : "table_name";

  public static final String T_COLUMN = Database.isOracle() ? "all_tab_cols" : "information_schema.columns";

  public static final String COLUMN_SCHEMA = Database.isOracle() ? "owner" : "table_schema";
  public static final String COLUMN_TABLE_NAME = "table_name";
  public static final String COLUMN_TABLE_REF = "table_ref";
  public static final String COLUMN_NAME = "column_name";
  public static final String COLUMN_DATA_TYPE = "data_type";

  public static final String T_CONSTRAINT = Database.isOracle() ?
          "all_constraints" : "information_schema.table_constraints";

  public static final String CONSTRAINT_SCHEMA = Database.isOracle() ? "owner" : "table_schema";
  public static final String CONSTRAINT_TABLE_NAME = "table_name";
  public static final String CONSTRAINT_TABLE_REF = "table_ref";
  public static final String CONSTRAINT_NAME = "constraint_name";
  public static final String CONSTRAINT_TYPE = "constraint_type";

  public static final String T_COLUMN_CONSTRAINT = Database.isOracle() ? "all_cons_columns" : "information_schema.key_column_usage";

  public static final String COLUMN_CONSTRAINT_SCHEMA = Database.isOracle() ? "owner" : "constraint_schema";
  public static final String COLUMN_CONSTRAINT_CONSTRAINT_NAME = "constraint_name";
  public static final String COLUMN_CONSTRAINT_CONSTRAINT_REF = "constraint_ref";
  public static final String COLUMN_CONSTRAINT_TABLE_NAME = "table_name";
  public static final String COLUMN_CONSTRAINT_COLUMN_NAME = "column_name";
  public static final String COLUMN_CONSTRAINT_POSITION = Database.isOracle() ? "position" : "ordinal_position";

  static {
    EntityRepository.get().initialize(T_SCHEMA,
            IdSource.ID_NONE, null, SCHEMA_NAME, null, true,
            new Property.PrimaryKeyProperty(SCHEMA_NAME, Type.STRING, "Name"));

    EntityRepository.get().initialize(T_TABLE,
            IdSource.ID_NONE, null, TABLE_SCHEMA + ", " + TABLE_NAME, null, true,
            new Property.EntityProperty(TABLE_SCHEMA_REF, "Schema", T_SCHEMA,
                    new Property.PrimaryKeyProperty(TABLE_SCHEMA, Type.STRING)),
            new Property.PrimaryKeyProperty(TABLE_NAME, Type.STRING, "Name", 1));

    EntityRepository.get().initialize(T_COLUMN,
            IdSource.ID_NONE, null, COLUMN_SCHEMA + ", " + COLUMN_TABLE_NAME + ", " + COLUMN_NAME, null, true,
            new Property.EntityProperty(COLUMN_TABLE_REF, "Table", T_TABLE,
                    new Property.PrimaryKeyProperty(COLUMN_SCHEMA, Type.STRING, null, 0),
                    new Property.PrimaryKeyProperty(COLUMN_TABLE_NAME, Type.STRING, null, 1)).setLookup(false),
            new Property.PrimaryKeyProperty(COLUMN_NAME, Type.STRING, "Column name", 2),
            new Property(COLUMN_DATA_TYPE, Type.STRING, "Data type"));

    EntityRepository.get().initialize(T_CONSTRAINT,
            IdSource.ID_NONE, null, CONSTRAINT_SCHEMA + ", " + CONSTRAINT_TABLE_NAME + ", " + CONSTRAINT_NAME, null, true,
            new Property.EntityProperty(CONSTRAINT_TABLE_REF, "Table", T_TABLE,
                    new Property.PrimaryKeyProperty(CONSTRAINT_SCHEMA, Type.STRING, null, 0),
                    new Property.PrimaryKeyProperty(CONSTRAINT_TABLE_NAME, Type.STRING, null, 1)).setLookup(false),
            new Property.PrimaryKeyProperty(CONSTRAINT_NAME, Type.STRING, "Constraint name", 2),
            new Property(CONSTRAINT_TYPE, Type.STRING, "Type"));

    EntityRepository.get().initialize(T_COLUMN_CONSTRAINT,
            IdSource.ID_NONE, null, COLUMN_CONSTRAINT_SCHEMA + ", " + COLUMN_CONSTRAINT_TABLE_NAME + ", " + COLUMN_CONSTRAINT_CONSTRAINT_NAME,
            null, true,
            new Property.EntityProperty(COLUMN_CONSTRAINT_CONSTRAINT_REF, "Constraint", T_CONSTRAINT,
                    new Property.PrimaryKeyProperty(COLUMN_CONSTRAINT_SCHEMA, Type.STRING, null, 0),
                    new Property.PrimaryKeyProperty(COLUMN_CONSTRAINT_TABLE_NAME, Type.STRING, null, 1),
                    new Property.PrimaryKeyProperty(COLUMN_CONSTRAINT_CONSTRAINT_NAME, Type.STRING, null, 2)).setLookup(false),
            new Property(COLUMN_CONSTRAINT_COLUMN_NAME, Type.STRING, "Column name"),
            new Property(COLUMN_CONSTRAINT_POSITION, Type.INT, "Position"));

    EntityProxy.setDefaultEntityProxy(new EntityProxy() {
      public String toString(final Entity entity) {
        if (entity.getEntityID().equals(T_COLUMN))
          return entity.getValueAsString(COLUMN_TABLE_REF) + "." + entity.getStringValue(COLUMN_NAME);
        else if (entity.getEntityID().equals(T_CONSTRAINT))
          return entity.getValueAsString(CONSTRAINT_TABLE_REF) + "." + entity.getStringValue(CONSTRAINT_NAME);
        else if (entity.getEntityID().equals(T_SCHEMA))
          return entity.getStringValue(SCHEMA_NAME);
        else if (entity.getEntityID().equals(T_TABLE))
          return entity.getValueAsString(TABLE_SCHEMA_REF) + "." + entity.getStringValue(TABLE_NAME);

        return super.toString(entity);
      }
    });
  }
}
