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

import org.json.JSONObject;
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
	private static final String EMPTY_JSON_OBJECT = "{}";

	private final Map<Attribute<?>, ColumnPreferences> columnPreferences;
	private final Map<Attribute<?>, ConditionPreferences> conditionPreferences;
	private final String columnsKey;
	private final String conditionsKey;

	private EntityTablePanelPreferences(EntityTablePanel tablePanel, Preferences preferences) {
		this.columnsKey = tablePanel.userPreferencesKey() + COLUMN_PREFERENCES;
		this.conditionsKey = tablePanel.userPreferencesKey() + CONDITIONS_PREFERENCES;
		Collection<Attribute<?>> identifiers = tablePanel.table().columnModel().identifiers();
		this.columnPreferences = ColumnPreferences.fromString(identifiers, preferences.get(columnsKey, EMPTY_JSON_OBJECT));
		this.conditionPreferences = ConditionPreferences.fromString(identifiers, preferences.get(conditionsKey, EMPTY_JSON_OBJECT));
	}

	EntityTablePanelPreferences(EntityTablePanel tablePanel) {
		this(createColumnPreferences(tablePanel.table().columnModel()),
						createConditionPreferences(tablePanel.tableModel()),
						tablePanel.userPreferencesKey() + COLUMN_PREFERENCES,
						tablePanel.userPreferencesKey() + CONDITIONS_PREFERENCES);
	}

	private EntityTablePanelPreferences(Map<Attribute<?>, ColumnPreferences> columnPreferences,
																			Map<Attribute<?>, ConditionPreferences> conditionPreferences,
																			String columnKey, String conditionKey) {
		this.columnsKey = columnKey;
		this.conditionsKey = conditionKey;
		this.columnPreferences = columnPreferences;
		this.conditionPreferences = conditionPreferences;
	}

	void apply(EntityTablePanel tablePanel) {
		applyColumnPreferences(tablePanel);
		applyConditionPreferences(tablePanel);
	}

	static EntityTablePanelPreferences preferences(EntityTablePanel tablePanel) {
		return new EntityTablePanelPreferences(tablePanel);
	}

	void saveLegacyPreferences() {
		try {
			UserPreferences.set(columnsKey, ColumnPreferences.toString(columnPreferences));
		}
		catch (Exception e) {
			LOG.error("Error while saving legacy column preferences", e);
		}
		try {
			UserPreferences.set(conditionsKey, ConditionPreferences.toString(conditionPreferences));
		}
		catch (Exception e) {
			LOG.error("Error while saving legacy condition preferences", e);
		}
	}

	void savePreferences(Preferences preferences) {
		try {
			preferences.put(columnsKey, ColumnPreferences.toString(columnPreferences));
		}
		catch (Exception e) {
			LOG.error("Error while saving column preferences", e);
		}
		try {
			preferences.put(conditionsKey, ConditionPreferences.toString(conditionPreferences));
		}
		catch (Exception e) {
			LOG.error("Error while saving condition preferences", e);
		}
	}

	static void applyLegacyPreferences(EntityTablePanel tablePanel) {
		fromLegacyPreferences(tablePanel).apply(tablePanel);
	}

	static void applyPreferences(Preferences preferences, EntityTablePanel tablePanel) {
		new EntityTablePanelPreferences(tablePanel, preferences).apply(tablePanel);
	}

	/**
	 * Clears any user preferences saved for this table model
	 */
	static void clearLegacyPreferences(EntityTablePanel tablePanel) {
		String userPreferencesKey = tablePanel.userPreferencesKey();
		UserPreferences.remove(userPreferencesKey + COLUMN_PREFERENCES);
		UserPreferences.remove(userPreferencesKey + CONDITIONS_PREFERENCES);
	}

	private void applyColumnPreferences(EntityTablePanel tablePanel) {
		try {
			ColumnPreferences.apply(tablePanel, columnPreferences);
		}
		catch (Exception e) {
			LOG.error("Error while applying column preferences: {}", columnPreferences, e);
		}
	}

	private void applyConditionPreferences(EntityTablePanel tablePanel) {
		try {
			ConditionPreferences.apply(tablePanel.tableModel(), conditionPreferences);
		}
		catch (Exception e) {
			LOG.error("Error while applying condition preferences: {}", conditionPreferences, e);
		}
	}

	private static Map<Attribute<?>, ColumnPreferences> createColumnPreferences(FilterTableColumnModel<Attribute<?>> columnModel) {
		Map<Attribute<?>, ColumnPreferences> columnPreferencesMap = new HashMap<>();
		for (FilterTableColumn<Attribute<?>> column : columnModel.columns()) {
			Attribute<?> attribute = column.identifier();
			int index = columnModel.visible(attribute).get() ? columnModel.getColumnIndex(attribute) : -1;
			columnPreferencesMap.put(attribute, ColumnPreferences.columnPreferences(attribute, index, column.getWidth()));
		}

		return columnPreferencesMap;
	}

	private static Map<Attribute<?>, ConditionPreferences> createConditionPreferences(SwingEntityTableModel tableModel) {
		Map<Attribute<?>, ConditionPreferences> conditionPreferencesMap = new HashMap<>();
		for (Attribute<?> attribute : tableModel.columns().identifiers()) {
			tableModel.queryModel().condition().optional(attribute)
							.ifPresent(condition ->
											conditionPreferencesMap.put(attribute, ConditionPreferences.conditionPreferences(attribute,
															condition.autoEnable().get(),
															condition.caseSensitive().get(),
															condition.operands().wildcard().get())));
		}

		return conditionPreferencesMap;
	}

	private static EntityTablePanelPreferences fromLegacyPreferences(EntityTablePanel tablePanel) {
		String columnsKey = tablePanel.userPreferencesKey() + COLUMN_PREFERENCES;
		String conditionsKey = tablePanel.userPreferencesKey() + CONDITIONS_PREFERENCES;

		Collection<Attribute<?>> identifiers = tablePanel.table().columnModel().identifiers();
		Map<Attribute<?>, ColumnPreferences> columnPreferences =
						ColumnPreferences.fromString(identifiers, UserPreferences.get(columnsKey, EMPTY_JSON_OBJECT));
		Map<Attribute<?>, ConditionPreferences> conditionPreferences =
						ConditionPreferences.fromString(identifiers, UserPreferences.get(conditionsKey, EMPTY_JSON_OBJECT));

		return new EntityTablePanelPreferences(columnPreferences, conditionPreferences, columnsKey, conditionsKey);
	}

	private static final class ColumnPreferences {

		private static final String LEGACY_COLUMN_INDEX = "index";
		private static final String LEGACY_COLUMN_WIDTH = "width";

		/**
		 * The key identifying column preferences
		 */
		private static final String COLUMNS_KEY = "columns";

		/**
		 * The key for the 'width' property
		 */
		private static final String WIDTH_KEY = "w";

		/**
		 * The key for the 'index' property
		 */
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

		/**
		 * Creates a new {@link ColumnPreferences} instance.
		 * @param attribute the attribute
		 * @param index the column index, -1 if not visible
		 * @param width the column width
		 * @return a new {@link ColumnPreferences} instance.
		 */
		private static ColumnPreferences columnPreferences(Attribute<?> attribute, int index, int width) {
			return new ColumnPreferences(attribute, index, width);
		}

		/**
		 * @param columnPreferences the column preferences mapped to their respective attribute
		 * @return a string encoding of the given preferences
		 */
		private static String toString(Map<Attribute<?>, ColumnPreferences> columnPreferences) {
			requireNonNull(columnPreferences);
			JSONObject jsonColumnPreferences = new JSONObject();
			columnPreferences.forEach((attribute, preferences) ->
							jsonColumnPreferences.put(attribute.name(), preferences.toJSONObject()));
			JSONObject preferencesRoot = new JSONObject();
			preferencesRoot.put(COLUMNS_KEY, jsonColumnPreferences);

			return preferencesRoot.toString();
		}

		/**
		 * @param attributes the attributes
		 * @param preferencesString the preferences encoded as a string
		 * @return a map containing the {@link ColumnPreferences} instances parsed from the given string
		 */
		private static Map<Attribute<?>, ColumnPreferences> fromString(Collection<Attribute<?>> attributes, String preferencesString) {
			requireNonNull(preferencesString);
			JSONObject preferences = new JSONObject(preferencesString);
			if (!preferences.has(COLUMNS_KEY)) {
				return Collections.emptyMap();
			}
			JSONObject jsonObject = preferences.getJSONObject(COLUMNS_KEY);
			return requireNonNull(attributes).stream()
							.map(attribute -> ColumnPreferences.columnPreferences(attribute, requireNonNull(jsonObject)))
							.flatMap(Optional::stream)
							.collect(toMap(ColumnPreferences::attribute, Function.identity()));
		}

		/**
		 * Applies the given column preferences to the given table model
		 * @param tableModel the table model to apply the preferences to
		 * @param columnPreferences the column preferences
		 */
		static void apply(EntityTablePanel tablePanel, Map<Attribute<?>, ColumnPreferences> columnPreferences) {
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

		/**
		 * The key identifying condition preferences
		 */
		private static final String CONDITIONS_KEY = "conditions";

		/**
		 * The key for the 'autoEnable' property
		 */
		private static final String AUTO_ENABLE_KEY = "ae";

		/**
		 * The key for the 'caseSensitive' property
		 */
		private static final String CASE_SENSITIVE_KEY = "cs";

		/**
		 * The key for the 'wildcard' property
		 */
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

		/**
		 * Creates a new {@link ConditionPreferences} instance.
		 * @param attribute the attribute
		 * @param autoEnable true if auto enable is enabled
		 * @param caseSensitive true if case-sensitive
		 * @param wildcard the wildcard state
		 * @return a new {@link ConditionPreferences} instance.
		 */
		private static ConditionPreferences conditionPreferences(Attribute<?> attribute, boolean autoEnable, boolean caseSensitive,
																														 Wildcard wildcard) {
			return new ConditionPreferences(attribute, autoEnable, caseSensitive, wildcard);
		}

		/**
		 * @param conditionPreferences the condition preferences mapped to their respective attribute
		 * @return a string encoding of the given preferences
		 */
		private static String toString(Map<Attribute<?>, ConditionPreferences> conditionPreferences) {
			requireNonNull(conditionPreferences);
			JSONObject jsonConditionPreferences = new JSONObject();
			conditionPreferences.forEach((attribute, preferences) ->
							jsonConditionPreferences.put(attribute.name(), preferences.toJSONObject()));
			JSONObject preferencesRoot = new JSONObject();
			preferencesRoot.put(ConditionPreferences.CONDITIONS_KEY, jsonConditionPreferences);

			return preferencesRoot.toString();
		}

		private static Map<Attribute<?>, ConditionPreferences> fromString(Collection<Attribute<?>> attributes, String preferencesString) {
			requireNonNull(preferencesString);
			JSONObject preferences = new JSONObject(preferencesString);
			if (!preferences.has(ConditionPreferences.CONDITIONS_KEY)) {
				return Collections.emptyMap();
			}
			JSONObject jsonObject = preferences.getJSONObject(ConditionPreferences.CONDITIONS_KEY);
			return requireNonNull(attributes).stream()
							.map(attribute -> conditionPreferences(attribute, requireNonNull(jsonObject)))
							.flatMap(Optional::stream)
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

		static Optional<ConditionPreferences> conditionPreferences(Attribute<?> attribute, JSONObject preferences) {
			if (preferences.has(attribute.name())) {
				return Optional.of(fromJSONObject(attribute, preferences.getJSONObject(attribute.name())));
			}

			return Optional.empty();
		}

		private static ConditionPreferences fromJSONObject(Attribute<?> attribute, JSONObject conditionObject) {
			return new ConditionPreferences(attribute,
							conditionObject.getInt(AUTO_ENABLE_KEY) == 1,
							conditionObject.getInt(CASE_SENSITIVE_KEY) == 1,
							Wildcard.valueOf(conditionObject.getString(WILDCARD_KEY)));
		}
	}
}
