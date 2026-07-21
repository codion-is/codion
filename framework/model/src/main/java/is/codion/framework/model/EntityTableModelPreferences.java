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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.component.table.FilterTableSort;
import is.codion.common.model.component.table.FilterTableSort.ColumnSortOrder;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Wildcard;
import is.codion.common.model.filter.SortOrder;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import static is.codion.common.model.condition.ConditionModel.CASE_SENSITIVE;
import static is.codion.common.model.condition.ConditionModel.WILDCARD;
import static java.util.Objects.requireNonNull;

/**
 * Stores and restores the model-owned persistent state of a {@link EntityTableModel} - the query condition
 * settings, filter settings and sort order - to and from a {@link Preferences} node. The state is written
 * as JSON string values under the {@link #CONDITIONS}, {@link #FILTERS} and {@link #SORT} keys. This is the
 * shared, client agnostic half of table preferences; view state (columns, layout) is persisted by the UI.
 */
final class EntityTableModelPreferences {

	private static final Logger LOG = LoggerFactory.getLogger(EntityTableModelPreferences.class);

	private static final String CONDITIONS = "conditions";
	private static final String FILTERS = "filters";
	private static final String SORT = "sort";

	private static final String AUTO_ENABLE = "ae";
	private static final String CASE_SENSITIVE_KEY = "cs";
	private static final String WILDCARD_KEY = "w";

	private static final String SORT_COLUMN = "c";
	private static final String SORT_ORDER = "o";

	private static final String EMPTY_JSON_OBJECT = "{}";
	private static final String EMPTY_JSON_ARRAY = "[]";

	private EntityTableModelPreferences() {}

	static void store(Preferences preferences, EntityTableModel<?, ?> tableModel) {
		requireNonNull(preferences);
		requireNonNull(tableModel);
		try {
			putOrRemove(preferences, CONDITIONS, createConditionPreferences(tableModel.query().condition().get()));
		}
		catch (Exception e) {
			LOG.error("Error while storing condition preferences", e);
		}
		try {
			putOrRemove(preferences, FILTERS, createConditionPreferences(tableModel.filters().get()));
		}
		catch (Exception e) {
			LOG.error("Error while storing filter preferences", e);
		}
		try {
			JSONArray sort = createSortPreferences(tableModel.sort());
			if (sort.isEmpty()) {
				preferences.remove(SORT);
			}
			else {
				preferences.put(SORT, sort.toString());
			}
		}
		catch (Exception e) {
			LOG.error("Error while storing sort preferences", e);
		}
	}

	static void restore(Preferences preferences, EntityTableModel<?, ?> tableModel) {
		requireNonNull(preferences);
		requireNonNull(tableModel);
		try {
			JSONObject conditions = new JSONObject(preferences.get(CONDITIONS, EMPTY_JSON_OBJECT));
			if (!conditions.isEmpty()) {
				restoreConditionPreferences(conditions, tableModel.query().condition().get());
			}
		}
		catch (Exception e) {
			LOG.error("Error while restoring condition preferences", e);
		}
		try {
			JSONObject filters = new JSONObject(preferences.get(FILTERS, EMPTY_JSON_OBJECT));
			if (!filters.isEmpty()) {
				restoreConditionPreferences(filters, tableModel.filters().get());
			}
		}
		catch (Exception e) {
			LOG.error("Error while restoring filter preferences", e);
		}
		try {
			JSONArray sort = new JSONArray(preferences.get(SORT, EMPTY_JSON_ARRAY));
			if (!sort.isEmpty()) {
				restoreSortPreferences(sort, tableModel);
			}
		}
		catch (Exception e) {
			LOG.error("Error while restoring sort preferences", e);
		}
	}

	private static void putOrRemove(Preferences preferences, String key, JSONObject value) {
		if (value.isEmpty()) {
			preferences.remove(key);
		}
		else {
			preferences.put(key, value.toString());
		}
	}

	private static JSONObject createConditionPreferences(Map<Attribute<?>, ConditionModel<?>> conditions) {
		JSONObject conditionPreferences = new JSONObject();
		// No configuration property for the autoEnable default (unlike CASE_SENSITIVE/WILDCARD) - update if one appears
		boolean defaultAutoEnable = true;
		boolean defaultCaseSensitive = CASE_SENSITIVE.getOrThrow();
		Wildcard defaultWildcard = WILDCARD.getOrThrow();
		for (Map.Entry<Attribute<?>, ConditionModel<?>> entry : conditions.entrySet()) {
			try {
				ConditionModel<?> condition = entry.getValue();
				JSONObject conditionJson = new JSONObject();
				boolean autoEnable = condition.autoEnable().is();
				boolean caseSensitive = condition.caseSensitive().is();
				Wildcard wildcard = condition.operands().wildcard().getOrThrow();
				if (autoEnable != defaultAutoEnable) {
					conditionJson.put(AUTO_ENABLE, autoEnable ? 1 : 0);
				}
				if (caseSensitive != defaultCaseSensitive) {
					conditionJson.put(CASE_SENSITIVE_KEY, caseSensitive ? 1 : 0);
				}
				if (wildcard != defaultWildcard) {
					conditionJson.put(WILDCARD_KEY, wildcard.name());
				}
				if (!conditionJson.isEmpty()) {
					conditionPreferences.put(entry.getKey().name(), conditionJson);
				}
			}
			catch (Exception e) {
				// Same per-item granularity as restore - one misbehaving condition model doesn't cost the rest
				LOG.error("Error while storing condition/filter preferences for attribute: {}", entry.getKey(), e);
			}
		}

		return conditionPreferences;
	}

	private static void restoreConditionPreferences(JSONObject conditionPreferences, Map<Attribute<?>, ConditionModel<?>> conditions) {
		for (Map.Entry<Attribute<?>, ConditionModel<?>> entry : conditions.entrySet()) {
			Attribute<?> attribute = entry.getKey();
			if (conditionPreferences.has(attribute.name())) {
				ConditionModel<?> condition = entry.getValue();
				try {
					JSONObject conditionJson = conditionPreferences.getJSONObject(attribute.name());
					if (conditionJson.has(AUTO_ENABLE)) {
						condition.autoEnable().set(conditionJson.getInt(AUTO_ENABLE) == 1);
					}
					if (conditionJson.has(CASE_SENSITIVE_KEY)) {
						condition.caseSensitive().set(conditionJson.getInt(CASE_SENSITIVE_KEY) == 1);
					}
					if (conditionJson.has(WILDCARD_KEY)) {
						condition.operands().wildcard().set(Wildcard.valueOf(conditionJson.getString(WILDCARD_KEY)));
					}
				}
				catch (Exception e) {
					LOG.error("Error parsing condition/filter preferences for attribute: {}", attribute, e);
				}
			}
		}
	}

	private static JSONArray createSortPreferences(FilterTableSort<Entity, Attribute<?>> sort) {
		JSONArray sortPreferences = new JSONArray();
		for (ColumnSortOrder<Attribute<?>> columnSortOrder : sort.columns().get()) {
			JSONObject sortJson = new JSONObject();
			sortJson.put(SORT_COLUMN, columnSortOrder.identifier().name());
			sortJson.put(SORT_ORDER, columnSortOrder.sortOrder().name());
			sortPreferences.put(sortJson);
		}

		return sortPreferences;
	}

	private static void restoreSortPreferences(JSONArray sortPreferences, EntityTableModel<?, ?> tableModel) {
		// Resolve first, clear only if anything resolves - otherwise stale/unknown stored attributes
		// would wipe a programmatically configured default sort and restore nothing in its place
		Map<Attribute<?>, SortOrder> sortOrders = new LinkedHashMap<>();
		EntityDefinition definition = tableModel.entityDefinition();
		for (int i = 0; i < sortPreferences.length(); i++) {
			try {
				JSONObject sortJson = sortPreferences.getJSONObject(i);
				Attribute<?> attribute = definition.attributes().get(sortJson.getString(SORT_COLUMN));
				if (attribute != null) {
					sortOrders.put(attribute, SortOrder.valueOf(sortJson.getString(SORT_ORDER)));
				}
			}
			catch (Exception e) {
				LOG.error("Error parsing sort preferences", e);
			}
		}
		if (!sortOrders.isEmpty()) {
			FilterTableSort<Entity, Attribute<?>> sort = tableModel.sort();
			sort.clear();
			sortOrders.forEach((attribute, sortOrder) -> {
				try {
					sort.order(attribute).add(sortOrder);
				}
				catch (Exception e) {
					// IllegalStateException in case sorting is locked for the column
					LOG.error("Error restoring sort preferences for attribute: {}", attribute, e);
				}
			});
		}
	}
}
