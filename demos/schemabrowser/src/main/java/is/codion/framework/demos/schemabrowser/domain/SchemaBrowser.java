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
import is.codion.framework.domain.entity.ForeignKey;

import java.sql.SQLException;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
import static is.codion.framework.domain.property.Properties.*;

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
            .stringFactory(stringFactory(Schema.NAME))
            .caption("Schemas");
  }

  public interface Table {
    EntityType<Entity> TYPE = DOMAIN.entityType("table");
    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("table_name"));
    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("table_schema"));
    ForeignKey SCHEMA_FK = TYPE.foreignKey(bundle.getString("table_schema_ref"), Table.SCHEMA, Schema.NAME);
  }

  void defineTable() {
    define(Table.TYPE, bundle.getString("t_table"),
            columnProperty(Table.SCHEMA).primaryKeyIndex(0),
            foreignKeyProperty(Table.SCHEMA_FK, "Schema"),
            primaryKeyProperty(Table.NAME, "Name").primaryKeyIndex(1))
            .orderBy(orderBy().ascending(Table.SCHEMA, Table.NAME))
            .readOnly(true)
            .stringFactory(stringFactory(Table.SCHEMA_FK).text(".").value(Table.NAME))
            .caption("Tables");
  }

  public interface Column {
    EntityType<Entity> TYPE = DOMAIN.entityType("column");
    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("column_schema"));
    Attribute<String> TABLE_NAME = TYPE.stringAttribute(bundle.getString("column_table_name"));
    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("column_name"));
    Attribute<String> DATA_TYPE = TYPE.stringAttribute(bundle.getString("column_data_type"));
    ForeignKey TABLE_FK = TYPE.foreignKey(bundle.getString("column_table_ref"),
            Column.SCHEMA, Table.SCHEMA,
            Column.TABLE_NAME, Table.NAME);
  }

  void defineColumn() {
    define(Column.TYPE, bundle.getString("t_column"),
            columnProperty(Column.SCHEMA).primaryKeyIndex(0),
            columnProperty(Column.TABLE_NAME).primaryKeyIndex(1),
            foreignKeyProperty(Column.TABLE_FK, "Table"),
            primaryKeyProperty(Column.NAME, "Column name").primaryKeyIndex(2),
            columnProperty(Column.DATA_TYPE, "Data type"))
            .orderBy(orderBy().ascending(Column.SCHEMA, Column.TABLE_NAME, Column.NAME))
            .readOnly(true)
            .stringFactory(stringFactory(Column.TABLE_FK).text(".").value(Column.NAME))
            .caption("Columns");
  }

  public interface Constraint {
    EntityType<Entity> TYPE = DOMAIN.entityType("constraint");
    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("constraint_schema"));
    Attribute<String> NAME = TYPE.stringAttribute(bundle.getString("constraint_name"));
    Attribute<String> CONSTRAINT_TYPE = TYPE.stringAttribute(bundle.getString("constraint_type"));
    Attribute<String> TABLE_NAME = TYPE.stringAttribute(bundle.getString("constraint_table_name"));
    ForeignKey TABLE_FK = TYPE.foreignKey(bundle.getString("constraint_table_ref"),
            Constraint.SCHEMA, Table.SCHEMA,
            Constraint.TABLE_NAME, Table.NAME);
  }

  void defineConstraint() {
    define(Constraint.TYPE, bundle.getString("t_constraint"),
            columnProperty(Constraint.SCHEMA).primaryKeyIndex(0),
            columnProperty(Constraint.TABLE_NAME).primaryKeyIndex(1),
            foreignKeyProperty(Constraint.TABLE_FK, "Table"),
            primaryKeyProperty(Constraint.NAME, "Constraint name").primaryKeyIndex(2),
            columnProperty(Constraint.CONSTRAINT_TYPE, "Type"))
            .orderBy(orderBy().ascending(Constraint.SCHEMA, Constraint.TABLE_NAME, Constraint.NAME))
            .readOnly(true)
            .stringFactory(stringFactory(Constraint.TABLE_FK).text(".").value(Constraint.NAME))
            .caption("Constraints");
  }

  public interface ColumnConstraint {
    EntityType<Entity> TYPE = DOMAIN.entityType("column_constraint");
    Attribute<String> SCHEMA = TYPE.stringAttribute(bundle.getString("column_constraint_schema"));
    Attribute<String> CONSTRAINT_NAME = TYPE.stringAttribute(bundle.getString("column_constraint_constraint_name"));
    Attribute<String> TABLE_NAME = TYPE.stringAttribute(bundle.getString("column_constraint_table_name"));
    Attribute<String> COLUMN_NAME = TYPE.stringAttribute(bundle.getString("column_constraint_column_name"));
    Attribute<Integer> POSITION = TYPE.integerAttribute(bundle.getString("column_constraint_position"));
    ForeignKey CONSTRAINT_FK = TYPE.foreignKey(bundle.getString("column_constraint_constraint_ref"),
            ColumnConstraint.SCHEMA, Constraint.SCHEMA,
            ColumnConstraint.TABLE_NAME, Constraint.TABLE_NAME,
            ColumnConstraint.CONSTRAINT_NAME, Constraint.NAME);
  }

  void defineColumnConstraint() {
    define(ColumnConstraint.TYPE, bundle.getString("t_column_constraint"),
            columnProperty(ColumnConstraint.SCHEMA).primaryKeyIndex(0),
            columnProperty(ColumnConstraint.TABLE_NAME).primaryKeyIndex(1),
            columnProperty(ColumnConstraint.CONSTRAINT_NAME).primaryKeyIndex(2),
            foreignKeyProperty(ColumnConstraint.CONSTRAINT_FK, "Constraint"),
            columnProperty(ColumnConstraint.COLUMN_NAME, "Column name"),
            columnProperty(ColumnConstraint.POSITION, "Position"))
            .orderBy(orderBy().ascending(ColumnConstraint.SCHEMA, ColumnConstraint.TABLE_NAME, ColumnConstraint.CONSTRAINT_NAME))
            .readOnly(true)
            .caption("Column constraints");
  }
}
