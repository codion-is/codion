/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class ForeignKeyConstraint {

  private final Table referencedTable;
  private final Map<MetadataColumn, MetadataColumn> references = new LinkedHashMap<>();

  ForeignKeyConstraint(Table referencedTable) {
    this.referencedTable = requireNonNull(referencedTable);
  }

  public Table referencedTable() {
    return referencedTable;
  }

  public Map<MetadataColumn, MetadataColumn> references() {
    return Collections.unmodifiableMap(references);
  }

  void addReference(MetadataColumn fkColumn, MetadataColumn pkColumn) {
    references.put(fkColumn, pkColumn);
  }
}
