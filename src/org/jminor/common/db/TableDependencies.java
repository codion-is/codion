/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.common.db;

import java.util.HashSet;
import java.util.Set;

public class TableDependencies {

  private final String table;
  private final String[] columns;

  private final Set<Dependency> dependencies = new HashSet<Dependency>();

  public TableDependencies(String table, String[] cols) {
    this.table = table.toLowerCase();
    this.columns = new String[cols.length];
    for (int i = 0; i < cols.length; i++) {
      columns[i] = cols[i].toLowerCase();
    }
  }

  /**
   * @return Value for property 'columns'.
   */
  public String[] getColumns() {
    return columns;
  }

  /**
   * @return Value for property 'table'.
   */
  public String getTable() {
    return table.toLowerCase();
  }

  /**
   * @return Value for property 'dependencies'.
   */
  public Dependency[] getDependencies() {
    return dependencies.toArray(new Dependency[dependencies.size()]);
  }

  public void addDependency(String table, String[] columns) {
    if (columns.length != this.columns.length)
      throw new IllegalArgumentException("Number of dependency columns does not fit the key");
    dependencies.add(new Dependency(table, columns));
  }

  /** {@inheritDoc} */
  public String toString() {
    final StringBuffer ret = new StringBuffer(getTable());
    ret.append(".");
    ret.append(getColumns());
    final Dependency[] dependencies = this.dependencies.toArray(new Dependency[this.dependencies.size()]);
    for (Dependency dependency : dependencies) {
      ret.append("\n#### ");
      ret.append(dependency);
    }

    return ret.toString();
  }

  public static class Dependency {
    private final String tableName;
    private final String[] dependingColumnNames;

    public Dependency(String tableName, String[] columns) {
      this.tableName = tableName.toLowerCase();
      this.dependingColumnNames = new String[columns.length];
      for (int i = 0; i < columns.length; i++) {
        this.dependingColumnNames[i] = columns[i].toLowerCase();
      }
    }

    /**
     * @return Value for property 'dependingColumnNames'.
     */
    public String[] getDependingColumnNames() {
      return dependingColumnNames;
    }

    /**
     * @return Value for property 'tableName'.
     */
    public String getTableName() {
      return tableName;
    }

    /** {@inheritDoc} */
    public String toString() {
      String ret = getTableName() + ".";
      for (int i = 0; i < dependingColumnNames.length; i++) {
        ret += dependingColumnNames[i];
        if (i < dependingColumnNames.length -1)
          ret += ", ";
      }

      return ret;
    }
  }
}
