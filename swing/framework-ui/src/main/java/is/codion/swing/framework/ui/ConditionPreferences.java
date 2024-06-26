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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityTableModel;

import org.json.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

final class ConditionPreferences {

	/**
	 * The key identifying condition preferences
	 */
	static final String CONDITIONS_KEY = "conditions";

	/**
	 * The key for the 'autoEnable' property
	 */
	static final String AUTO_ENABLE_KEY = "ae";

	/**
	 * The key for the 'caseSensitive' property
	 */
	static final String CASE_SENSITIVE_KEY = "cs";

	/**
	 * The key for the 'automaticWildcard' property
	 */
	static final String AUTOMATIC_WILDCARD_KEY = "aw";

	private final Attribute<?> attribute;
	private final boolean autoEnable;
	private final boolean caseSensitive;
	private final AutomaticWildcard automaticWildcard;

	ConditionPreferences(Attribute<?> attribute, boolean autoEnable, boolean caseSensitive, AutomaticWildcard automaticWildcard) {
		this.attribute = attribute;
		this.autoEnable = autoEnable;
		this.caseSensitive = caseSensitive;
		this.automaticWildcard = requireNonNull(automaticWildcard);
	}

	Attribute<?> attribute() {
		return attribute;
	}

	boolean autoEnable() {
		return autoEnable;
	}

	boolean caseSensitive() {
		return caseSensitive;
	}

	AutomaticWildcard automaticWildcard() {
		return automaticWildcard;
	}

	JSONObject toJSONObject() {
		JSONObject conditionObject = new JSONObject();
		conditionObject.put(AUTO_ENABLE_KEY, autoEnable() ? 1 : 0);
		conditionObject.put(CASE_SENSITIVE_KEY, caseSensitive() ? 1 : 0);
		conditionObject.put(AUTOMATIC_WILDCARD_KEY, automaticWildcard());

		return conditionObject;
	}

	/**
	 * Creates a new {@link ConditionPreferences} instance.
	 * @param attribute the attribute
	 * @param autoEnable true if auto enable is enabled
	 * @param caseSensitive true if case sensitive
	 * @param automaticWildcard the automatic wildcard state
	 * @return a new {@link ConditionPreferences} instance.
	 */
	static ConditionPreferences conditionPreferences(Attribute<?> attribute, boolean autoEnable, boolean caseSensitive,
																									 AutomaticWildcard automaticWildcard) {
		return new ConditionPreferences(attribute, autoEnable, caseSensitive, automaticWildcard);
	}

	/**
	 * @param conditionPreferences the condition preferences mapped to their respective attribute
	 * @return a string encoding of the given preferences
	 */
	static String toString(Map<Attribute<?>, ConditionPreferences> conditionPreferences) {
		requireNonNull(conditionPreferences);
		JSONObject jsonConditionPreferences = new JSONObject();
		conditionPreferences.forEach((attribute, preferences) -> jsonConditionPreferences.put(attribute.name(), preferences.toJSONObject()));
		JSONObject preferencesRoot = new JSONObject();
		preferencesRoot.put(ConditionPreferences.CONDITIONS_KEY, jsonConditionPreferences);

		return preferencesRoot.toString();
	}

	/**
	 * @param attributes the attributes
	 * @param preferencesString the preferences encoded as as string
	 * @return a map containing the {@link EntityTablePanel.ColumnPreferences} instances parsed from the given string
	 */
	static Map<Attribute<?>, ConditionPreferences> fromString(Collection<Attribute<?>> attributes, String preferencesString) {
		requireNonNull(preferencesString);
		JSONObject jsonObject = new JSONObject(preferencesString).getJSONObject(ConditionPreferences.CONDITIONS_KEY);
		return requireNonNull(attributes).stream()
						.map(attribute -> conditionPreferences(attribute, requireNonNull(jsonObject)))
						.filter(Optional::isPresent)
						.map(Optional::get)
						.collect(toMap(ConditionPreferences::attribute, Function.identity()));
	}

	/**
	 * Applies the given condition preferences to the given table model
	 * @param tableModel the table model to apply the preferences to
	 * @param columnAttributes the available column attributes
	 * @param preferencesString the condition preferences string
	 */
	static void apply(EntityTableModel<?> tableModel, List<Attribute<?>> columnAttributes, String preferencesString) {
		requireNonNull(tableModel);
		requireNonNull(columnAttributes);
		requireNonNull(preferencesString);

		Map<Attribute<?>, ConditionPreferences> conditionPreferences = fromString(columnAttributes, preferencesString);
		for (Attribute<?> attribute : columnAttributes) {
			ConditionPreferences preferences = conditionPreferences.get(attribute);
			if (preferences != null) {
				ColumnConditionModel<? extends Attribute<?>, Object> conditionModel = tableModel.conditionModel().conditionModel(attribute);
				if (conditionModel != null) {
					conditionModel.caseSensitive().set(preferences.caseSensitive());
					conditionModel.autoEnable().set(preferences.autoEnable());
					conditionModel.automaticWildcard().set(preferences.automaticWildcard());
				}
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
						AutomaticWildcard.valueOf(conditionObject.getString(AUTOMATIC_WILDCARD_KEY)));
	}
}
