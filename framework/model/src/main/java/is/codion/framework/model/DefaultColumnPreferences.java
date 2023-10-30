/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityTableModel.ColumnPreferences;

import org.json.JSONObject;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultColumnPreferences implements ColumnPreferences {

  private static final String LEGACY_CONDITION = "condition";
  private static final String LEGACY_AUTO_ENABLE = "autoEnable";
  private static final String LEGACY_CASE_SENSITIVE = "caseSensitive";
  private static final String LEGACY_AUTOMATIC_WILDCARD = "automaticWildcard";
  private static final String LEGACY_COLUMN_INDEX = "index";
  private static final String LEGACY_COLUMN_WIDTH = "width";

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
      conditionObject.put(ConditionPreferences.PREFERENCE_AUTO_ENABLE, conditionPreferences.autoEnable() ? 1 : 0);
      conditionObject.put(ConditionPreferences.PREFERENCE_CASE_SENSITIVE, conditionPreferences.caseSensitive() ? 1 : 0);
      conditionObject.put(ConditionPreferences.PREFERENCE_AUTOMATIC_WILDCARD, conditionPreferences.automaticWildcard());
      columnObject.put(ConditionPreferences.CONDITION, conditionObject);
    }

    return columnObject;
  }

  static Optional<ColumnPreferences> columnPreferences(Attribute<?> attribute, JSONObject preferences) {
    if (preferences.has(attribute.name())) {
      JSONObject jsonObject = preferences.getJSONObject(attribute.name());
      return Optional.of(jsonObject.has(LEGACY_COLUMN_INDEX) ?
              fromLegacyJSONObject(attribute, jsonObject) :
              fromJSONObject(attribute, jsonObject));
    }

    return Optional.empty();
  }

  private static ColumnPreferences fromJSONObject(Attribute<?> attribute, JSONObject jsonObject) {
    ConditionPreferences conditionPreferences = null;
    if (jsonObject.has(ConditionPreferences.CONDITION)) {
      JSONObject conditionObject = jsonObject.getJSONObject(ConditionPreferences.CONDITION);
      conditionPreferences = new DefaultConditionPreferences(
              conditionObject.getInt(ConditionPreferences.PREFERENCE_AUTO_ENABLE) == 1,
              conditionObject.getInt(ConditionPreferences.PREFERENCE_CASE_SENSITIVE) == 1,
              AutomaticWildcard.valueOf(conditionObject.getString(ConditionPreferences.PREFERENCE_AUTOMATIC_WILDCARD)));
    }

    return new DefaultColumnPreferences(attribute,
            jsonObject.getInt(ColumnPreferences.PREFERENCE_COLUMN_INDEX),
            jsonObject.getInt(ColumnPreferences.PREFERENCE_COLUMN_WIDTH),
            conditionPreferences);
  }

  private static ColumnPreferences fromLegacyJSONObject(Attribute<?> attribute, JSONObject jsonObject) {
    ConditionPreferences conditionPreferences = null;
    if (jsonObject.has(LEGACY_CONDITION)) {
      JSONObject conditionObject = jsonObject.getJSONObject(LEGACY_CONDITION);
      conditionPreferences = new DefaultConditionPreferences(
              conditionObject.getBoolean(LEGACY_AUTO_ENABLE),
              conditionObject.getBoolean(LEGACY_CASE_SENSITIVE),
              AutomaticWildcard.valueOf(conditionObject.getString(LEGACY_AUTOMATIC_WILDCARD)));
    }

    return new DefaultColumnPreferences(attribute,
            jsonObject.getInt(LEGACY_COLUMN_INDEX),
            jsonObject.getInt(LEGACY_COLUMN_WIDTH),
            conditionPreferences);
  }
}
