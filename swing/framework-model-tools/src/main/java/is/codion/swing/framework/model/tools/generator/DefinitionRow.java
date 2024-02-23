/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.generator;

import is.codion.framework.domain.entity.EntityDefinition;

public final class DefinitionRow {

  final String tableType;
  final EntityDefinition definition;

  DefinitionRow(EntityDefinition definition, String tableType) {
    this.definition = definition;
    this.tableType = tableType;
  }
}
