/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.domain;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringProvider;

import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

public final class SchemaBrowser extends DefaultDomain {

  static final DomainType DOMAIN = domainType(SchemaBrowser.class);

  public SchemaBrowser() {
    super(DOMAIN);
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

  public interface Schema {
    EntityType<Entity> TYPE = DOMAIN.entityType("schema");
    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("schema_name"));
  }

  void defineSchema() {
    define(Schema.TYPE, bundle.getString("t_schema"),
            primaryKeyProperty(Schema.NAME, "Name"))
            .orderBy(orderBy().ascending(Schema.NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(Schema.NAME))
            .caption("Schemas");
  }

  public interface Table {
    EntityType<Entity> TYPE = DOMAIN.entityType("table");
    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("table_name"));
    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("table_schema"));
    Attribute<Entity> SCHEMA_FK = TYPE.entityAttribute(bundle.getString("table_schema_ref"));
  }

  void defineTable() {
    define(Table.TYPE, bundle.getString("t_table"),
            foreignKeyProperty(Table.SCHEMA_FK, "Schema", Schema.TYPE,
                    primaryKeyProperty(Table.SCHEMA).primaryKeyIndex(0)),
            primaryKeyProperty(Table.NAME, "Name").primaryKeyIndex(1))
            .orderBy(orderBy().ascending(Table.SCHEMA, Table.NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(Table.SCHEMA_FK).addText(".").addValue(Table.NAME))
            .caption("Tables");
  }

  public interface Column {
    EntityType<Entity> TYPE = DOMAIN.entityType("column");
    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("column_schema"));
    Attribute<String> TABLE_NAME = TYPE.stringAttribute(bundle.getString("column_table_name"));
    Attribute<Entity> TABLE_FK = TYPE.entityAttribute(bundle.getString("column_table_ref"));
    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("column_name"));
    Attribute<String> DATA_TYPE = TYPE.stringAttribute(bundle.getString("column_data_type"));
  }

  void defineColumn() {
    define(Column.TYPE, bundle.getString("t_column"),
            foreignKeyProperty(Column.TABLE_FK, "Table", Table.TYPE,
                    asList(columnProperty(Column.SCHEMA).primaryKeyIndex(0),
                            columnProperty(Column.TABLE_NAME).primaryKeyIndex(1))),
            primaryKeyProperty(Column.NAME, "Column name").primaryKeyIndex(2),
            columnProperty(Column.DATA_TYPE, "Data type"))
            .orderBy(orderBy().ascending(Column.SCHEMA, Column.TABLE_NAME, Column.NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(Column.TABLE_FK).addText(".").addValue(Column.NAME))
            .caption("Columns");
  }

  public interface Constraint {
    EntityType<Entity> TYPE = DOMAIN.entityType("constraint");
    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("constraint_schema"));
    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("constraint_name"));
    Attribute<String> CONSTRAINT_TYPE = TYPE.stringAttribute(bundle.getString("constraint_type"));
    Attribute<Entity> TABLE_FK = TYPE.entityAttribute(bundle.getString("constraint_table_ref"));
    Attribute<String> TABLE_NAME = TYPE.stringAttribute(bundle.getString("constraint_table_name"));
  }

  void defineConstraint() {
    define(Constraint.TYPE, bundle.getString("t_constraint"),
            foreignKeyProperty(Constraint.TABLE_FK, "Table", Table.TYPE,
                    asList(columnProperty(Constraint.SCHEMA).primaryKeyIndex(0),
                            columnProperty(Constraint.TABLE_NAME).primaryKeyIndex(1))),
            primaryKeyProperty(Constraint.NAME, "Constraint name").primaryKeyIndex(2),
            columnProperty(Constraint.CONSTRAINT_TYPE, "Type"))
            .orderBy(orderBy().ascending(Constraint.SCHEMA, Constraint.TABLE_NAME, Constraint.NAME))
            .readOnly(true)
            .stringProvider(new StringProvider(Constraint.TABLE_FK).addText(".").addValue(Constraint.NAME))
            .caption("Constraints");
  }

  public interface ColumnConstraint {
    EntityType<Entity> TYPE = DOMAIN.entityType("column_constraint");
    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("column_constraint_schema"));
    Attribute<String> CONSTRAINT_NAME = TYPE.stringAttribute(bundle.getString("column_constraint_constraint_name"));
    Attribute<Entity> CONSTRAINT_FK = TYPE.entityAttribute(bundle.getString("column_constraint_constraint_ref"));
    Attribute<String> TABLE_NAME = TYPE.stringAttribute(bundle.getString("column_constraint_table_name"));
    Attribute<String> COLUMN_NAME = TYPE.stringAttribute(bundle.getString("column_constraint_column_name"));
    Attribute<Integer> POSITION = TYPE.integerAttribute(bundle.getString("column_constraint_position"));
  }

  void defineColumnConstraint() {
    define(ColumnConstraint.TYPE, bundle.getString("t_column_constraint"),
            foreignKeyProperty(ColumnConstraint.CONSTRAINT_FK, "Constraint", Constraint.TYPE,
                    asList(columnProperty(ColumnConstraint.SCHEMA).primaryKeyIndex(0),
                            columnProperty(ColumnConstraint.TABLE_NAME).primaryKeyIndex(1),
                            columnProperty(ColumnConstraint.CONSTRAINT_NAME).primaryKeyIndex(2))),
            columnProperty(ColumnConstraint.COLUMN_NAME, "Column name"),
            columnProperty(ColumnConstraint.POSITION, "Position"))
            .orderBy(orderBy().ascending(ColumnConstraint.SCHEMA, ColumnConstraint.TABLE_NAME, ColumnConstraint.CONSTRAINT_NAME))
            .readOnly(true)
            .caption("Column constraints");
  }
}
