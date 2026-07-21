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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Wildcard;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.framework.ui.EntityTableExportPanel.ExportPreferences;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import static is.codion.common.model.condition.ConditionModel.CASE_SENSITIVE;
import static is.codion.common.model.condition.ConditionModel.WILDCARD;
import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class EntityTablePanelPreferences {

	private static final Logger LOG = LoggerFactory.getLogger(EntityTablePanelPreferences.class);

	private static final String AUTO_RESIZE_MODE = "auto-resize-mode";
	private static final String EMPTY_JSON_OBJECT = "{}";

	private static final String TABLE_KEY = "table";
	private static final String EXPORT_KEY = "export";

	private static final String COLUMNS_KEY = "columns";
	private static final String WIDTH_KEY = "w";
	private static final String INDEX_KEY = "i";
	private static final String CONDITIONS_KEY = "conditions";
	private static final String FILTERS_KEY = "filters";
	private static final String AUTO_ENABLE_KEY = "ae";
	private static final String CASE_SENSITIVE_KEY = "cs";
	private static final String WILDCARD_KEY = "w";

	private final Preferences preferences = jsonPreferences();

	EntityTablePanelPreferences(EntityTablePanel tablePanel) {
		store(preferences, tablePanel);
	}

	void restore(EntityTablePanel tablePanel) {
		restore(preferences, tablePanel);
	}

	/**
	 * Stores all preferences for the given table panel to the given Preferences node.
	 * Uses JSONObject internally to store preferences as JSON strings.
	 * @param preferences the preferences node to store to
	 * @param tablePanel the table panel
	 */
	static void store(Preferences preferences, EntityTablePanel tablePanel) {
		requireNonNull(preferences);
		requireNonNull(tablePanel);
		try {
			preferences.put(TABLE_KEY, createTablePreferences(tablePanel).toString());
		}
		catch (Exception e) {
			LOG.error("Error while storing table preferences", e);
		}
		try {
			preferences.put(COLUMNS_KEY, createColumnPreferences(tablePanel.table().columnModel()).toString());
		}
		catch (Exception e) {
			LOG.error("Error while storing column preferences", e);
		}
		try {
			if (tablePanel.configuration.includeConditions) {
				preferences.put(CONDITIONS_KEY, createConditionPreferences(tablePanel.tableModel().query().condition().get()).toString());
			}
		}
		catch (Exception e) {
			LOG.error("Error while storing condition preferences", e);
		}
		try {
			if (tablePanel.configuration.includeFilters) {
				preferences.put(FILTERS_KEY, createConditionPreferences(tablePanel.tableModel().filters().get()).toString());
			}
		}
		catch (Exception e) {
			LOG.error("Error while storing filter preferences", e);
		}
		try {
			preferences.put(EXPORT_KEY, new ExportPreferences(tablePanel.exportModel()).preferences().toString());
		}
		catch (Exception e) {
			LOG.error("Error while storing export preferences", e);
		}
	}

	/**
	 * Applies preferences from a Preferences node to the given table panel.
	 * Reads JSON strings from the node and applies using JSONObject internally.
	 * @param preferences the preferences node to read from
	 * @param tablePanel the table panel
	 */
	static void restore(Preferences preferences, EntityTablePanel tablePanel) {
		requireNonNull(preferences);
		requireNonNull(tablePanel);
		try {
			restoreTablePreferences(new JSONObject(preferences.get(TABLE_KEY, EMPTY_JSON_OBJECT)), tablePanel);
		}
		catch (Exception e) {
			LOG.error("Error while restoring table preferences", e);
		}
		try {
			JSONObject columnPrefs = new JSONObject(preferences.get(COLUMNS_KEY, EMPTY_JSON_OBJECT));
			if (!columnPrefs.isEmpty()) {
				restoreColumnPreferences(columnPrefs, tablePanel);
			}
		}
		catch (Exception e) {
			LOG.error("Error while restoring column preferences", e);
		}
		try {
			JSONObject conditionPrefs = new JSONObject(preferences.get(CONDITIONS_KEY, EMPTY_JSON_OBJECT));
			if (!conditionPrefs.isEmpty()) {
				restoreConditionPreferences(conditionPrefs, tablePanel.tableModel().query().condition().get());
			}
		}
		catch (Exception e) {
			LOG.error("Error while restoring condition preferences", e);
		}
		try {
			JSONObject filterPrefs = new JSONObject(preferences.get(FILTERS_KEY, EMPTY_JSON_OBJECT));
			if (!filterPrefs.isEmpty()) {
				restoreConditionPreferences(filterPrefs, tablePanel.tableModel().filters().get());
			}
		}
		catch (Exception e) {
			LOG.error("Error while restoring filter preferences", e);
		}
		try {
			String exportJson = preferences.get(EXPORT_KEY, EMPTY_JSON_OBJECT);
			if (!EMPTY_JSON_OBJECT.equals(exportJson)) {
				new ExportPreferences(exportJson).restore(tablePanel.exportModel());
			}
		}
		catch (Exception e) {
			LOG.error("Error while restoring export preferences", e);
		}
	}

	private static JSONObject createColumnPreferences(FilterTableColumnModel<Attribute<?>> columnModel) {
		JSONObject columnPreferences = new JSONObject();
		for (FilterTableColumn<Attribute<?>> column : columnModel.columns()) {
			Attribute<?> attribute = column.identifier();
			int index = columnModel.visible(attribute).is() ? columnModel.getColumnIndex(attribute) : -1;
			JSONObject columnJson = new JSONObject();
			columnJson.put(WIDTH_KEY, column.getWidth());
			columnJson.put(INDEX_KEY, index);
			columnPreferences.put(attribute.name(), columnJson);
		}

		return columnPreferences;
	}

	private static JSONObject createConditionPreferences(Map<Attribute<?>, ConditionModel<?>> conditions) {
		JSONObject conditionPreferences = new JSONObject();
		boolean defaultAutoEnable = true;
		boolean defaultCaseSensitive = CASE_SENSITIVE.getOrThrow();
		Wildcard defaultWildcard = WILDCARD.getOrThrow();
		for (Map.Entry<Attribute<?>, ConditionModel<?>> entry : conditions.entrySet()) {
			ConditionModel<?> condition = entry.getValue();
			JSONObject conditionJson = new JSONObject();
			boolean autoEnable = condition.autoEnable().is();
			boolean caseSensitive = condition.caseSensitive().is();
			Wildcard wildcard = condition.operands().wildcard().getOrThrow();
			if (autoEnable != defaultAutoEnable) {
				conditionJson.put(AUTO_ENABLE_KEY, autoEnable ? 1 : 0);
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

		return conditionPreferences;
	}

	private static JSONObject createTablePreferences(EntityTablePanel tablePanel) {
		JSONObject json = new JSONObject();
		json.put(AUTO_RESIZE_MODE, tablePanel.table().getAutoResizeMode());

		return json;
	}

	private static void restoreTablePreferences(JSONObject tablePreferences, EntityTablePanel tablePanel) {
		if (tablePreferences.has(AUTO_RESIZE_MODE)) {
			tablePanel.table().setAutoResizeMode(tablePreferences.getInt(AUTO_RESIZE_MODE));
		}
	}

	private static void restoreColumnPreferences(JSONObject columnPreferences, EntityTablePanel tablePanel) {
		FilterTableColumnModel<Attribute<?>> columnModel = tablePanel.table().columnModel();
		List<Attribute<?>> attributesWithoutPreferences = new ArrayList<>();
		List<AttributeIndex> attributesWithPreferences = new ArrayList<>();
		for (Attribute<?> attribute : columnModel.identifiers()) {
			if (columnPreferences.has(attribute.name())) {
				JSONObject json = columnPreferences.getJSONObject(attribute.name());
				int width = json.optInt(WIDTH_KEY, -1);
				int index = json.optInt(INDEX_KEY, -1);
				if (width > 0) {
					FilterTableColumn<Attribute<?>> column = columnModel.column(attribute);
					column.setPreferredWidth(width);
					column.setWidth(width);
				}
				if (index >= 0) {
					attributesWithPreferences.add(new AttributeIndex(attribute, index));
				}
			}
			else {
				attributesWithoutPreferences.add(attribute);
			}
		}
		List<Attribute<?>> visibleAttributes = attributesWithPreferences.stream()
						.sorted(Comparator.comparingInt(AttributeIndex::index))
						.map(AttributeIndex::attribute)
						.collect(toList());
		visibleAttributes.addAll(0, attributesWithoutPreferences);
		columnModel.visible().set(visibleAttributes);
	}

	private static void restoreConditionPreferences(JSONObject conditionPreferences, Map<Attribute<?>, ConditionModel<?>> conditions) {
		for (Map.Entry<Attribute<?>, ConditionModel<?>> entry : conditions.entrySet()) {
			Attribute<?> attribute = entry.getKey();
			if (conditionPreferences.has(attribute.name())) {
				ConditionModel<?> condition = entry.getValue();
				try {
					JSONObject conditionJson = conditionPreferences.getJSONObject(attribute.name());
					if (conditionJson.has(AUTO_ENABLE_KEY)) {
						condition.autoEnable().set(conditionJson.getInt(AUTO_ENABLE_KEY) == 1);
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

	private static final class AttributeIndex {

		private final Attribute<?> attribute;
		private final int index;

		private AttributeIndex(Attribute<?> attribute, int index) {
			this.attribute = attribute;
			this.index = index;
		}

		private Attribute<?> attribute() {
			return attribute;
		}

		private int index() {
			return index;
		}
	}
}
