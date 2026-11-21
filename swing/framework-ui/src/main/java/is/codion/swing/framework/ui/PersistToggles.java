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
package is.codion.swing.framework.ui;

import is.codion.common.reactive.state.State;
import is.codion.framework.domain.entity.attribute.Attribute;

import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.KeyStroke;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * The persist toggles of an {@link EditorComponents} instance and of its detail instances, recursively,
 * with the persist states they had when enabled, their defaults.
 * <p>The states differing from their defaults are stored as such, not as the fact that they differ,
 * which a later change to a default would reverse, the details by link name, unique among the detail
 * editors of a master, for example
 * {@code {"attributes": {"country": true}, "details": {"customer_fk": {"attributes": {"newsletter": false}}}}}.
 */
final class PersistToggles {

	private static final Logger LOG = LoggerFactory.getLogger(PersistToggles.class);

	private static final String ATTRIBUTES = "attributes";
	private static final String DETAILS = "details";

	private final EditorComponents components;
	private final Map<Attribute<?>, Boolean> defaults = new LinkedHashMap<>();
	private final Map<String, PersistToggles> details = new LinkedHashMap<>();

	private PersistToggles(EditorComponents components, @Nullable KeyStroke keyStroke, String caption) {
		this.components = components;
		components.persistToggle().locked().set(true);
		components.persistToggle().get().forEach(attribute -> {
			State persist = persist(attribute);
			defaults.put(attribute, persist.is());
			PersistToggle.enable(components.component(attribute).get(), persist, keyStroke, caption);
		});
		components.detail().components().forEach(detail ->
						details.put(detail.name(), new PersistToggles(detail.components(), keyStroke, caption)));
	}

	/**
	 * Enables the persist toggles of the given instance and of its detail instances, recursively, locking their
	 * persist toggle sets.
	 * @param components the {@link EditorComponents} instance
	 * @param keyStroke the key stroke toggling a state while its component has the focus, null for none
	 * @param caption the caption of the toggles
	 * @return the enabled persist toggles
	 * @throws IllegalStateException in case an attribute with a persist toggle has no component
	 */
	static PersistToggles enable(EditorComponents components, @Nullable KeyStroke keyStroke, String caption) {
		return new PersistToggles(requireNonNull(components), keyStroke, requireNonNull(caption));
	}

	/**
	 * @return the states differing from their defaults, empty in case none do
	 */
	JSONObject changed() {
		JSONObject attributes = new JSONObject();
		defaults.forEach((attribute, defaultState) -> {
			boolean state = persist(attribute).is();
			if (state != defaultState) {
				attributes.put(attribute.name(), state);
			}
		});
		JSONObject detailStates = new JSONObject();
		details.forEach((name, detail) -> {
			JSONObject changed = detail.changed();
			if (!changed.isEmpty()) {
				detailStates.put(name, changed);
			}
		});
		JSONObject changed = new JSONObject();
		if (!attributes.isEmpty()) {
			changed.put(ATTRIBUTES, attributes);
		}
		if (!detailStates.isEmpty()) {
			changed.put(DETAILS, detailStates);
		}

		return changed;
	}

	/**
	 * The stored states replace the current ones, a state not stored returning to its default,
	 * and a stored name no longer belonging to an attribute with a persist toggle, or to a detail, being ignored.
	 * @param stored the stored states, as returned by {@link #changed()}
	 */
	void restore(JSONObject stored) {
		JSONObject attributes = object(stored, ATTRIBUTES);
		defaults.forEach((attribute, defaultState) -> {
			State state = persist(attribute);
			boolean restored = attributes.optBoolean(attribute.name(), defaultState);
			if (state.is() != restored) {
				state.set(restored);
			}
		});
		JSONObject detailStates = object(stored, DETAILS);
		details.forEach((name, detail) -> detail.restore(object(detailStates, name)));
	}

	/**
	 * @param json the stored states
	 * @return the stored states, empty in case they are unreadable, reverting to the defaults
	 */
	static JSONObject parse(String json) {
		try {
			return new JSONObject(requireNonNull(json));
		}
		catch (JSONException e) {
			LOG.error("Unreadable persist preferences, reverting to defaults", e);
			return new JSONObject();
		}
	}

	private State persist(Attribute<?> attribute) {
		return components.editor().value(attribute).persist();
	}

	private static JSONObject object(JSONObject object, String key) {
		JSONObject value = object.optJSONObject(key);

		return value == null ? new JSONObject() : value;
	}
}
