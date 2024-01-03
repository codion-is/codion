/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.metadata;

public final class ForeignKeyColumn {

  private final String pkSchemaName;
  private final String pkTableName;
  private final String pkColumnName;
  private final String fkTableName;
  private final String fkSchemaName;
  private final String fkColumnName;
  private final int keySeq;

  ForeignKeyColumn(String pkSchemaName, String pkTableName, String pkColumnName,
                   String fkTableName, String fkSchemaName, String fkColumnName,
                   int keySeq) {
    this.pkSchemaName = pkSchemaName;
    this.pkTableName = pkTableName;
    this.pkColumnName = pkColumnName;
    this.fkTableName = fkTableName;
    this.fkSchemaName = fkSchemaName;
    this.fkColumnName = fkColumnName;
    this.keySeq = keySeq;
  }

  public String pkSchemaName() {
    return pkSchemaName;
  }

  public String pkTableName() {
    return pkTableName;
  }

  public String pkColumnName() {
    return pkColumnName;
  }

  public String fkTableName() {
    return fkTableName;
  }

  public String fkSchemaName() {
    return fkSchemaName;
  }

  public String fkColumnName() {
    return fkColumnName;
  }

  public int keySeq() {
    return keySeq;
  }
}
