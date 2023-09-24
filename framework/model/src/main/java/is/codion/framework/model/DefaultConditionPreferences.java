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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
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
