/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class MetaDataForeignKeyConstraint {

  private final MetaDataTable referencedTable;
  private final Map<MetaDataColumn, MetaDataColumn> references = new LinkedHashMap<>();

  MetaDataForeignKeyConstraint(MetaDataTable referencedTable) {
    this.referencedTable = requireNonNull(referencedTable);
  }

  public MetaDataTable referencedTable() {
    return referencedTable;
  }

  public Map<MetaDataColumn, MetaDataColumn> references() {
    return Collections.unmodifiableMap(references);
  }

  void addReference(MetaDataColumn fkColumn, MetaDataColumn pkColumn) {
    references.put(fkColumn, pkColumn);
  }
}
