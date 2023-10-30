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
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityTableModel.ColumnPreferences.ConditionPreferences;

import org.json.JSONObject;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultConditionPreferences implements ConditionPreferences {

  private final Attribute<?> attribute;
  private final boolean autoEnable;
  private final boolean caseSensitive;
  private final AutomaticWildcard automaticWildcard;

  DefaultConditionPreferences(Attribute<?> attribute, boolean autoEnable, boolean caseSensitive, AutomaticWildcard automaticWildcard) {
    this.attribute = attribute;
    this.autoEnable = autoEnable;
    this.caseSensitive = caseSensitive;
    this.automaticWildcard = requireNonNull(automaticWildcard);
  }

  @Override
  public Attribute<?> attribute() {
    return attribute;
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

  @Override
  public JSONObject toJSONObject() {
    JSONObject conditionObject = new JSONObject();
    conditionObject.put(AUTO_ENABLE_KEY, autoEnable() ? 1 : 0);
    conditionObject.put(CASE_SENSITIVE_KEY, caseSensitive() ? 1 : 0);
    conditionObject.put(AUTOMATIC_WILDCARD_KEY, automaticWildcard());

    return conditionObject;
  }

  static Optional<ConditionPreferences> conditionPreferences(Attribute<?> attribute, JSONObject preferences) {
    if (preferences.has(attribute.name())) {
      return Optional.of(fromJSONObject(attribute, preferences.getJSONObject(attribute.name())));
    }

    return Optional.empty();
  }

  private static ConditionPreferences fromJSONObject(Attribute<?> attribute, JSONObject conditionObject) {
    return new DefaultConditionPreferences(attribute,
            conditionObject.getInt(AUTO_ENABLE_KEY) == 1,
            conditionObject.getInt(CASE_SENSITIVE_KEY) == 1,
            AutomaticWildcard.valueOf(conditionObject.getString(AUTOMATIC_WILDCARD_KEY)));
  }
}
