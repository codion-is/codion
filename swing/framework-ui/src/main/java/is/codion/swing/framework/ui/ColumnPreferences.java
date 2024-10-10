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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.framework.domain.entity.attribute.Attribute;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class ColumnPreferences {

	private static final String LEGACY_COLUMN_INDEX = "index";
	private static final String LEGACY_COLUMN_WIDTH = "width";

	/**
	 * The key identifying column preferences
	 */
	static final String COLUMNS_KEY = "columns";

	/**
	 * The key for the 'width' property
	 */
	static final String WIDTH_KEY = "w";

	/**
	 * The key for the 'index' property
	 */
	static final String INDEX_KEY = "i";

	private final Attribute<?> attribute;
	private final int index;
	private final int width;

	ColumnPreferences(Attribute<?> attribute, int index, int width) {
		this.attribute = requireNonNull(attribute);
		this.index = index;
		this.width = width;
	}

	Attribute<?> attribute() {
		return attribute;
	}

	int index() {
		return index;
	}

	boolean visible() {
		return index != -1;
	}

	int width() {
		return width;
	}

	JSONObject toJSONObject() {
		JSONObject columnObject = new JSONObject();
		columnObject.put(WIDTH_KEY, width());
		columnObject.put(INDEX_KEY, index());

		return columnObject;
	}

	/**
	 * Creates a new {@link ColumnPreferences} instance.
	 * @param attribute the attribute
	 * @param index the column index, -1 if not visible
	 * @param width the column width
	 * @return a new {@link ColumnPreferences} instance.
	 */
	static ColumnPreferences columnPreferences(Attribute<?> attribute, int index, int width) {
		return new ColumnPreferences(attribute, index, width);
	}

	/**
	 * @param columnPreferences the column preferences mapped to their respective attribute
	 * @return a string encoding of the given preferences
	 */
	static String toString(Map<Attribute<?>, ColumnPreferences> columnPreferences) {
		requireNonNull(columnPreferences);
		JSONObject jsonColumnPreferences = new JSONObject();
		columnPreferences.forEach((attribute, preferences) -> jsonColumnPreferences.put(attribute.name(), preferences.toJSONObject()));
		JSONObject preferencesRoot = new JSONObject();
		preferencesRoot.put(COLUMNS_KEY, jsonColumnPreferences);

		return preferencesRoot.toString();
	}

	/**
	 * @param attributes the attributes
	 * @param preferencesString the preferences encoded as as string
	 * @return a map containing the {@link ColumnPreferences} instances parsed from the given string
	 */
	static Map<Attribute<?>, ColumnPreferences> fromString(Collection<Attribute<?>> attributes, String preferencesString) {
		requireNonNull(preferencesString);
		JSONObject jsonObject = new JSONObject(preferencesString).getJSONObject(COLUMNS_KEY);
		return requireNonNull(attributes).stream()
						.map(attribute -> ColumnPreferences.columnPreferences(attribute, requireNonNull(jsonObject)))
						.flatMap(Optional::stream)
						.collect(toMap(ColumnPreferences::attribute, Function.identity()));
	}

	/**
	 * Applies the given column preferences to the given table model
	 * @param tableModel the table model to apply the preferences to
	 * @param columnAttributes the available column attributes
	 * @param preferencesString the preferences string
	 * @param setColumnWidth sets the column width
	 */
	static void apply(EntityTablePanel tablePanel, Collection<Attribute<?>> columnAttributes,
										String preferencesString, BiConsumer<Attribute<?>, Integer> setColumnWidth) {
		requireNonNull(tablePanel);
		requireNonNull(columnAttributes);
		requireNonNull(preferencesString);
		requireNonNull(setColumnWidth);

		Map<Attribute<?>, ColumnPreferences> columnPreferences = fromString(columnAttributes, preferencesString);
		List<Attribute<?>> columnAttributesWithoutPreferences = new ArrayList<>();
		for (Attribute<?> attribute : columnAttributes) {
			ColumnPreferences preferences = columnPreferences.get(attribute);
			if (preferences == null) {
				columnAttributesWithoutPreferences.add(attribute);
			}
			else {
				setColumnWidth.accept(attribute, preferences.width());
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
