/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.framework.domain.entity.EntityDefinition;

public final class DefinitionRow {

  final DatabaseDomain domain;
  final EntityDefinition definition;

  DefinitionRow(DatabaseDomain domain, EntityDefinition definition) {
    this.domain = domain;
    this.definition = definition;
  }
}
