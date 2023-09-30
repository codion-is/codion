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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityTableModel.ColumnPreferences;

import org.json.JSONObject;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultColumnPreferences implements ColumnPreferences {

  private final Attribute<?> attribute;
  private final int index;
  private final int width;
  private final ConditionPreferences conditionPreferences;

  DefaultColumnPreferences(Attribute<?> attribute, int index, int width, ConditionPreferences conditionPreferences) {
    this.attribute = requireNonNull(attribute);
    this.index = index;
    this.width = width;
    this.conditionPreferences = conditionPreferences;
  }

  @Override
  public Attribute<?> attribute() {
    return attribute;
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public boolean visible() {
    return index != -1;
  }

  @Override
  public int width() {
    return width;
  }

  @Override
  public Optional<ConditionPreferences> conditionPreferences() {
    return Optional.ofNullable(conditionPreferences);
  }

  @Override
  public JSONObject toJSONObject() {
    JSONObject columnObject = new JSONObject();
    columnObject.put(ColumnPreferences.PREFERENCE_COLUMN_WIDTH, width());
    columnObject.put(ColumnPreferences.PREFERENCE_COLUMN_INDEX, index());
    if (conditionPreferences != null) {
      JSONObject conditionObject = new JSONObject();
      conditionObject.put(ConditionPreferences.PREFERENCE_AUTO_ENABLE, conditionPreferences.autoEnable());
      conditionObject.put(ConditionPreferences.PREFERENCE_CASE_SENSITIVE, conditionPreferences.caseSensitive());
      conditionObject.put(ConditionPreferences.PREFERENCE_AUTOMATIC_WILDCARD, conditionPreferences.automaticWildcard());
      columnObject.put(ConditionPreferences.CONDITION, conditionObject);
    }

    return columnObject;
  }

  static Optional<ColumnPreferences> columnPreferences(Attribute<?> attribute, JSONObject preferences) {
    if (preferences.has(attribute.name())) {
      return Optional.of(fromJSONObject(attribute, preferences.getJSONObject(attribute.name())));
    }

    return Optional.empty();
  }

  private static ColumnPreferences fromJSONObject(Attribute<?> attribute, JSONObject jsonObject) {
    ConditionPreferences conditionPreferences = null;
    if (jsonObject.has(ConditionPreferences.CONDITION)) {
      JSONObject conditionObject = jsonObject.getJSONObject(ConditionPreferences.CONDITION);
      conditionPreferences = new DefaultConditionPreferences(
              conditionObject.getBoolean(ConditionPreferences.PREFERENCE_AUTO_ENABLE),
              conditionObject.getBoolean(ConditionPreferences.PREFERENCE_CASE_SENSITIVE),
              AutomaticWildcard.valueOf(conditionObject.getString(ConditionPreferences.PREFERENCE_AUTOMATIC_WILDCARD)));
    }

    return new DefaultColumnPreferences(attribute,
            jsonObject.getInt(ColumnPreferences.PREFERENCE_COLUMN_INDEX),
            jsonObject.getInt(ColumnPreferences.PREFERENCE_COLUMN_WIDTH),
            conditionPreferences);
  }
}
