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
package is.codion.framework.model;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityTableModel.ColumnPreferences;

import org.json.JSONObject;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultColumnPreferences implements ColumnPreferences {

	private static final String LEGACY_COLUMN_INDEX = "index";
	private static final String LEGACY_COLUMN_WIDTH = "width";

	private final Attribute<?> attribute;
	private final int index;
	private final int width;

	DefaultColumnPreferences(Attribute<?> attribute, int index, int width) {
		this.attribute = requireNonNull(attribute);
		this.index = index;
		this.width = width;
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
	public JSONObject toJSONObject() {
		JSONObject columnObject = new JSONObject();
		columnObject.put(WIDTH_KEY, width());
		columnObject.put(INDEX_KEY, index());

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
		return new DefaultColumnPreferences(attribute,
						jsonObject.getInt(INDEX_KEY),
						jsonObject.getInt(WIDTH_KEY));
	}

	private static ColumnPreferences fromLegacyJSONObject(Attribute<?> attribute, JSONObject jsonObject) {
		return new DefaultColumnPreferences(attribute,
						jsonObject.getInt(LEGACY_COLUMN_INDEX),
						jsonObject.getInt(LEGACY_COLUMN_WIDTH));
	}
}
