/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;

import static java.util.Objects.requireNonNull;

final class DefaultConditionPreferences implements EntityTableModel.ColumnPreferences.ConditionPreferences {

  private final boolean autoEnable;
  private final boolean caseSensitive;
  private final AutomaticWildcard automaticWildcard;

  DefaultConditionPreferences(boolean autoEnable, boolean caseSensitive, AutomaticWildcard automaticWildcard) {
    this.autoEnable = autoEnable;
    this.caseSensitive = caseSensitive;
    this.automaticWildcard = requireNonNull(automaticWildcard);
  }

  @Override
  public boolean autoEnable() {
    return autoEnable;
  }

  @Override
  public boolean caseSensitive() {
    return caseSensitive;
  }

  @Override
  public AutomaticWildcard automaticWildcard() {
    return automaticWildcard;
  }
}
