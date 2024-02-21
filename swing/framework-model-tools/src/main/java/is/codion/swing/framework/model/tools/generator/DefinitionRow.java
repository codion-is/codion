/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.generator;

import is.codion.framework.domain.entity.EntityDefinition;

public final class DefinitionRow {

  final DatabaseDomain domain;
  final String tableType;
  final EntityDefinition definition;

  DefinitionRow(DatabaseDomain domain, String tableType, EntityDefinition definition) {
    this.domain = domain;
    this.tableType = tableType;
    this.definition = definition;
  }
}
