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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.condition.ConditionModel.Wildcard;
import is.codion.common.model.preferences.UserPreferences;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableExportPanel.ExportPreferences;

import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.prefs.Preferences;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class EntityTablePanelPreferences {

	private static final Logger LOG = LoggerFactory.getLogger(EntityTablePanelPreferences.class);

	private static final String COLUMN_PREFERENCES = "-columns";
	private static final String CONDITIONS_PREFERENCES = "-conditions";
	private static final String EXPORT_PREFERENCES = "-export";
	private static final String EMPTY_JSON_OBJECT = "{}";

	private final Map<Attribute<?>, ColumnPreferences> columnPreferences;
	private final Map<Attribute<?>, ConditionPreferences> conditionPreferences;
	private final @Nullable ExportPreferences exportPreferences;
	private final String columnsKey;
	private final String conditionsKey;
	private final String exportKey;

	EntityTablePanelPreferences(EntityTablePanel tablePanel, Preferences preferences) {
		this.columnsKey = tablePanel.preferencesKey() + COLUMN_PREFERENCES;
		this.conditionsKey = tablePanel.preferencesKey() + CONDITIONS_PREFERENCES;
		this.exportKey = tablePanel.preferencesKey() + EXPORT_PREFERENCES;
		Collection<Attribute<?>> identifiers = tablePanel.table().columnModel().identifiers();
		this.columnPreferences = ColumnPreferences.fromString(identifiers, preferences.get(columnsKey, EMPTY_JSON_OBJECT));
		this.conditionPreferences = ConditionPreferences.fromString(identifiers, preferences.get(conditionsKey, EMPTY_JSON_OBJECT));
		this.exportPreferences = new ExportPreferences(preferences.get(exportKey, EMPTY_JSON_OBJECT));
	}

	EntityTablePanelPreferences(EntityTablePanel tablePanel) {
		this(createColumnPreferences(tablePanel.table().columnModel()),
						createConditionPreferences(tablePanel.tableModel()),
						new ExportPreferences(tablePanel.exportPanel()),
						tablePanel.preferencesKey() + COLUMN_PREFERENCES,
						tablePanel.preferencesKey() + CONDITIONS_PREFERENCES,
						tablePanel.preferencesKey() + EXPORT_PREFERENCES);
	}

	private EntityTablePanelPreferences(Map<Attribute<?>, ColumnPreferences> columnPreferences,
																			Map<Attribute<?>, ConditionPreferences> conditionPreferences,
																			@Nullable ExportPreferences exportPreferences,
																			String columnKey, String conditionKey, String exportKey) {
		this.columnsKey = columnKey;
		this.conditionsKey = conditionKey;
		this.exportKey = exportKey;
		this.columnPreferences = columnPreferences;
		this.conditionPreferences = conditionPreferences;
		this.exportPreferences = exportPreferences;
	}

	void apply(EntityTablePanel tablePanel) {
		if (!columnPreferences.isEmpty()) {
			try {
				ColumnPreferences.apply(tablePanel, columnPreferences);
			}
			catch (Exception e) {
				LOG.error("Error while applying column preferences: {}", columnPreferences, e);
			}
		}
		if (!conditionPreferences.isEmpty()) {
			try {
				ConditionPreferences.apply(tablePanel.tableModel(), conditionPreferences);
			}
			catch (Exception e) {
				LOG.error("Error while applying condition preferences: {}", conditionPreferences, e);
			}
		}
		if (exportPreferences != null) {// not strictly necessary, but prevents null warning
			try {
				exportPreferences.apply(tablePanel.exportPanel());
			}
			catch (Exception e) {
				LOG.error("Error while applying export preferences: {}", exportPreferences, e);
			}
		}
	}

	void saveLegacy() {
		try {
			UserPreferences.set(columnsKey, ColumnPreferences.toJsonLegacy(columnPreferences).toString());
		}
		catch (Exception e) {
			LOG.error("Error while saving legacy column preferences", e);
		}
		try {
			UserPreferences.set(conditionsKey, ConditionPreferences.toJsonLegacy(conditionPreferences).toString());
		}
		catch (Exception e) {
			LOG.error("Error while saving legacy condition preferences", e);
		}
	}

	void save(Preferences preferences) {
		try {
			preferences.put(columnsKey, ColumnPreferences.toJson(columnPreferences).toString());
		}
		catch (Exception e) {
			LOG.error("Error while saving column preferences", e);
		}
		try {
			preferences.put(conditionsKey, ConditionPreferences.toJson(conditionPreferences).toString());
		}
		catch (Exception e) {
			LOG.error("Error while saving condition preferences", e);
		}
		if (exportPreferences != null) {// not strictly necessary, but prevents null warning
			try {
				preferences.put(exportKey, exportPreferences.preferences().toString());
			}
			catch (Exception e) {
				LOG.error("Error while saving export preferences", e);
			}
		}
	}

	static void applyLegacy(EntityTablePanel tablePanel) {
		String columnsKey = tablePanel.preferencesKey() + COLUMN_PREFERENCES;
		String conditionsKey = tablePanel.preferencesKey() + CONDITIONS_PREFERENCES;

		Collection<Attribute<?>> identifiers = tablePanel.table().columnModel().identifiers();
		Map<Attribute<?>, ColumnPreferences> columnPreferences =
						ColumnPreferences.fromStringLegacy(identifiers, UserPreferences.get(columnsKey, EMPTY_JSON_OBJECT));
		Map<Attribute<?>, ConditionPreferences> conditionPreferences =
						ConditionPreferences.fromStringLegacy(identifiers, UserPreferences.get(conditionsKey, EMPTY_JSON_OBJECT));

		new EntityTablePanelPreferences(columnPreferences, conditionPreferences,
						/* No legacy export preferences*/ null, columnsKey, conditionsKey,
						/* No legacy export preferences*/"no-export-prefs").apply(tablePanel);
	}

	static void clearLegacyPreferences(EntityTablePanel tablePanel) {
		String preferencesKey = tablePanel.preferencesKey();
		UserPreferences.remove(preferencesKey + COLUMN_PREFERENCES);
		UserPreferences.remove(preferencesKey + CONDITIONS_PREFERENCES);
	}

	private static Map<Attribute<?>, ColumnPreferences> createColumnPreferences(FilterTableColumnModel<Attribute<?>> columnModel) {
		Map<Attribute<?>, ColumnPreferences> columnPreferencesMap = new HashMap<>();
		for (FilterTableColumn<Attribute<?>> column : columnModel.columns()) {
			Attribute<?> attribute = column.identifier();
			int index = columnModel.visible(attribute).is() ? columnModel.getColumnIndex(attribute) : -1;
			columnPreferencesMap.put(attribute, new ColumnPreferences(attribute, index, column.getWidth()));
		}

		return columnPreferencesMap;
	}

	private static Map<Attribute<?>, ConditionPreferences> createConditionPreferences(SwingEntityTableModel tableModel) {
		Map<Attribute<?>, ConditionPreferences> conditionPreferencesMap = new HashMap<>();
		for (Attribute<?> attribute : tableModel.columns().identifiers()) {
			tableModel.queryModel().condition().optional(attribute)
							.ifPresent(condition ->
											conditionPreferencesMap.put(attribute, new ConditionPreferences(attribute,
															condition.autoEnable().is(),
															condition.caseSensitive().is(),
															condition.operands().wildcard().getOrThrow())));
		}

		return conditionPreferencesMap;
	}

	private static final class ColumnPreferences {

		private static final String LEGACY_COLUMN_INDEX = "index";
		private static final String LEGACY_COLUMN_WIDTH = "width";
		private static final String COLUMNS_KEY = "columns";
		private static final String WIDTH_KEY = "w";
		private static final String INDEX_KEY = "i";

		private final Attribute<?> attribute;
		private final int index;
		private final int width;

		private ColumnPreferences(Attribute<?> attribute, int index, int width) {
			this.attribute = requireNonNull(attribute);
			this.index = index;
			this.width = width;
		}

		@Override
		public String toString() {
			return toJSONObject().toString();
		}

		private Attribute<?> attribute() {
			return attribute;
		}

		private int index() {
			return index;
		}

		private boolean visible() {
			return index != -1;
		}

		private JSONObject toJSONObject() {
			JSONObject columnObject = new JSONObject();
			columnObject.put(WIDTH_KEY, width);
			columnObject.put(INDEX_KEY, index);

			return columnObject;
		}

		private static JSONObject toJsonLegacy(Map<Attribute<?>, ColumnPreferences> columnPreferences) {
			JSONObject preferences = new JSONObject();
			preferences.put(COLUMNS_KEY, toJson(columnPreferences));

			return preferences;
		}

		private static JSONObject toJson(Map<Attribute<?>, ColumnPreferences> columnPreferences) {
			JSONObject json = new JSONObject();
			columnPreferences.forEach((attribute, preferences) ->
							json.put(attribute.name(), preferences.toJSONObject()));

			return json;
		}

		private static Map<Attribute<?>, ColumnPreferences> fromStringLegacy(Collection<Attribute<?>> attributes, String preferencesString) {
			JSONObject json = new JSONObject(preferencesString);
			if (!json.has(COLUMNS_KEY)) {
				return Collections.emptyMap();
			}

			return fromJson(attributes, json.getJSONObject(COLUMNS_KEY));
		}

		private static Map<Attribute<?>, ColumnPreferences> fromString(Collection<Attribute<?>> attributes, String preferencesString) {
			return fromJson(attributes, new JSONObject(preferencesString));
		}

		private static Map<Attribute<?>, ColumnPreferences> fromJson(Collection<Attribute<?>> attributes, JSONObject preferences) {
			return attributes.stream()
							.map(attribute -> ColumnPreferences.fromJson(attribute, preferences))
							.filter(Optional::isPresent)
							.map(Optional::get)
							.collect(toMap(ColumnPreferences::attribute, Function.identity()));
		}

		private static void apply(EntityTablePanel tablePanel, Map<Attribute<?>, ColumnPreferences> columnPreferences) {
			List<Attribute<?>> columnAttributesWithoutPreferences = new ArrayList<>();
			for (Attribute<?> attribute : tablePanel.table().columnModel().identifiers()) {
				ColumnPreferences preferences = columnPreferences.get(attribute);
				if (preferences == null) {
					columnAttributesWithoutPreferences.add(attribute);
				}
				else {
					tablePanel.table().columnModel().column(attribute).setPreferredWidth(preferences.width);
				}
			}
			List<Attribute<?>> visibleColumnAttributes = columnPreferences.values().stream()
							.filter(ColumnPreferences::visible)
							.sorted(Comparator.comparingInt(ColumnPreferences::index))
							.map(ColumnPreferences::attribute)
							.collect(toList());
			visibleColumnAttributes.addAll(0, columnAttributesWithoutPreferences);
			tablePanel.table().columnModel().visible().set(visibleColumnAttributes);
		}

		private static Optional<ColumnPreferences> fromJson(Attribute<?> attribute, JSONObject preferences) {
			if (preferences.has(attribute.name())) {
				JSONObject jsonObject = preferences.getJSONObject(attribute.name());
				return Optional.of(jsonObject.has(LEGACY_COLUMN_INDEX) ?
								fromLegacyJSONObject(attribute, jsonObject) :
								fromJSONObject(attribute, jsonObject));
			}

			return Optional.empty();
		}

		private static ColumnPreferences fromJSONObject(Attribute<?> attribute, JSONObject jsonObject) {
			return new ColumnPreferences(attribute,
							jsonObject.getInt(INDEX_KEY),
							jsonObject.getInt(WIDTH_KEY));
		}

		private static ColumnPreferences fromLegacyJSONObject(Attribute<?> attribute, JSONObject jsonObject) {
			return new ColumnPreferences(attribute,
							jsonObject.getInt(LEGACY_COLUMN_INDEX),
							jsonObject.getInt(LEGACY_COLUMN_WIDTH));
		}
	}

	private static final class ConditionPreferences {

		private static final String CONDITIONS_KEY = "conditions";
		private static final String AUTO_ENABLE_KEY = "ae";
		private static final String CASE_SENSITIVE_KEY = "cs";
		private static final String WILDCARD_KEY = "w";

		private final Attribute<?> attribute;
		private final boolean autoEnable;
		private final boolean caseSensitive;
		private final Wildcard wildcard;

		private ConditionPreferences(Attribute<?> attribute, boolean autoEnable, boolean caseSensitive, Wildcard wildcard) {
			this.attribute = attribute;
			this.autoEnable = autoEnable;
			this.caseSensitive = caseSensitive;
			this.wildcard = requireNonNull(wildcard);
		}

		@Override
		public String toString() {
			return toJSONObject().toString();
		}

		private Attribute<?> attribute() {
			return attribute;
		}

		private JSONObject toJSONObject() {
			JSONObject conditionObject = new JSONObject();
			conditionObject.put(AUTO_ENABLE_KEY, autoEnable ? 1 : 0);
			conditionObject.put(CASE_SENSITIVE_KEY, caseSensitive ? 1 : 0);
			conditionObject.put(WILDCARD_KEY, wildcard);

			return conditionObject;
		}

		private static JSONObject toJsonLegacy(Map<Attribute<?>, ConditionPreferences> conditionPreferences) {
			JSONObject preferences = new JSONObject();
			preferences.put(CONDITIONS_KEY, toJson(conditionPreferences));

			return preferences;
		}

		private static JSONObject toJson(Map<Attribute<?>, ConditionPreferences> conditionPreferences) {
			JSONObject json = new JSONObject();
			conditionPreferences.forEach((attribute, preferences) ->
							json.put(attribute.name(), preferences.toJSONObject()));

			return json;
		}

		private static Map<Attribute<?>, ConditionPreferences> fromStringLegacy(Collection<Attribute<?>> attributes, String preferencesString) {
			JSONObject json = new JSONObject(preferencesString);
			if (!json.has(CONDITIONS_KEY)) {
				return Collections.emptyMap();
			}

			return fromJson(attributes, json.getJSONObject(CONDITIONS_KEY));
		}

		private static Map<Attribute<?>, ConditionPreferences> fromString(Collection<Attribute<?>> attributes, String preferencesString) {
			return fromJson(attributes, new JSONObject(preferencesString));
		}

		private static Map<Attribute<?>, ConditionPreferences> fromJson(Collection<Attribute<?>> attributes, JSONObject jsonObject) {
			return attributes.stream()
							.map(attribute -> fromJson(attribute, jsonObject))
							.filter(Optional::isPresent)
							.map(Optional::get)
							.collect(toMap(ConditionPreferences::attribute, Function.identity()));
		}

		private static void apply(SwingEntityTableModel tableModel, Map<Attribute<?>, ConditionPreferences> conditionPreferences) {
			for (Attribute<?> attribute : tableModel.columns().identifiers()) {
				ConditionPreferences preferences = conditionPreferences.get(attribute);
				if (preferences != null) {
					tableModel.queryModel().condition().optional(attribute)
									.ifPresent(condition -> {
										condition.caseSensitive().set(preferences.caseSensitive);
										condition.autoEnable().set(preferences.autoEnable);
										condition.operands().wildcard().set(preferences.wildcard);
									});
				}
			}
		}

		private static Optional<ConditionPreferences> fromJson(Attribute<?> attribute, JSONObject preferences) {
			if (preferences.has(attribute.name())) {
				JSONObject conditionObject = preferences.getJSONObject(attribute.name());
				try {
					return Optional.of(new ConditionPreferences(attribute,
								conditionObject.getInt(AUTO_ENABLE_KEY) == 1,
								conditionObject.getInt(CASE_SENSITIVE_KEY) == 1,
								Wildcard.valueOf(conditionObject.getString(WILDCARD_KEY))));
				}
				catch (Exception e) {
					LOG.error("Error parsing condition preferences for attribute: {}", attribute, e);
				}
			}

			return Optional.empty();
		}
	}
}
