/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class ForeignKeyConstraint {

  private final Table referencedTable;
  private final Map<Column, Column> references = new LinkedHashMap<>();

  ForeignKeyConstraint(Table referencedTable) {
    this.referencedTable = requireNonNull(referencedTable);
  }

  public Table getReferencedTable() {
    return referencedTable;
  }

  public Map<Column, Column> getReferences() {
    return Collections.unmodifiableMap(references);
  }

  void addReference(Column fkColumn, Column pkColumn) {
    references.put(fkColumn, pkColumn);
  }
}
