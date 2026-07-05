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
package is.codion.swing.framework.ui.inspect;

import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.model.EntityEditor.EditorValue;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.inspect.UiInspector;
import is.codion.swing.framework.model.SwingEntityEditor;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link UiInspector} exposing the state of the {@link is.codion.framework.model.EntityEditor} behind the
 * focus owner's enclosing {@link EntityPanel} — the same state the {@code EditorInspector} developer tool shows,
 * projected to a map for tooling. Located via {@link java.util.ServiceLoader}, so the consumer stays
 * framework-agnostic.
 */
public final class EntityEditorInspector implements UiInspector {

	@Override
	public Optional<Map<String, Object>> state(Component focusOwner) {
		EntityEditPanel editPanel = Ancestor.ofType(EntityEditPanel.class).of(focusOwner).get();
		if (editPanel == null) {
			return Optional.empty();
		}

		return Optional.of(state(editPanel.editor()));
	}

	private static Map<String, Object> state(SwingEntityEditor editor) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("entityType", editor.entityDefinition().type().toString());
		state.put("exists", editor.entity().exists().is());
		state.put("modified", editor.entity().modified().is());
		state.put("valid", editor.entity().valid().is());
		List<Map<String, Object>> attributes = new ArrayList<>();
		for (AttributeDefinition<?> definition : editor.entityDefinition().attributes().definitions()) {
			attributes.add(attributeState(definition, editor.value(definition.attribute())));
		}
		state.put("attributes", attributes);

		return state;
	}

	private static Map<String, Object> attributeState(AttributeDefinition<?> definition, EditorValue<?> value) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("attribute", definition.attribute().name());
		state.put("value", String.valueOf(value.get()));
		state.put("valid", value.valid().is());
		state.put("modified", value.modified().is());
		if (value.modified().is()) {
			state.put("original", String.valueOf(value.original()));
		}
		if (!value.valid().is()) {
			state.put("message", value.message().get());
		}

		return state;
	}
}
