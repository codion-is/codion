/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.metadata;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ForeignKey {

  private final Table referencedTable;
  private final Map<Column, Column> references = new LinkedHashMap<>();

  ForeignKey(final Table referencedTable) {
    this.referencedTable = referencedTable;
  }

  public Table getReferencedTable() {
    return referencedTable;
  }

  public Map<Column, Column> getReferences() {
    return references;
  }

  public void addReference(final Column fkColumn, final Column pkColumn) {
    references.put(fkColumn, pkColumn);
  }
}
