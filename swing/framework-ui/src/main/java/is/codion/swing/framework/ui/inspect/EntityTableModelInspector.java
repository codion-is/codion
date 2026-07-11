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

import is.codion.common.model.selection.MultiSelection;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.inspect.UiInspector;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.awt.Component;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A {@link UiInspector} exposing the state of the {@link is.codion.framework.model.EntityTableModel} behind the
 * focus owner's enclosing {@link EntityTablePanel} — the row count and the selection. Located via
 * {@link java.util.ServiceLoader}, so the consumer stays framework-agnostic.
 */
public final class EntityTableModelInspector implements UiInspector {

	@Override
	public Optional<Map<String, Object>> state(Component focusOwner) {
		EntityTablePanel tablePanel = Ancestor.ofType(EntityTablePanel.class).of(focusOwner).get();
		if (tablePanel == null) {
			return Optional.empty();
		}
		if (Ancestor.ofType(ColumnConditionPanel.class).of(focusOwner).get() != null) {
			//focus is in a condition/filter field, EntityConditionInspector reports it
			return Optional.empty();
		}

		return Optional.of(state(tablePanel.tableModel()));
	}

	private static Map<String, Object> state(SwingEntityTableModel tableModel) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("entityType", tableModel.entityDefinition().type().toString());
		state.put("rowCount", tableModel.getRowCount());
		MultiSelection<Entity> selection = tableModel.selection();
		state.put("selectionCount", selection.count());
		state.put("selectedIndex", selection.index().get());
		List<Object> selected = new ArrayList<>();
		for (Entity entity : selection.items().get()) {
			selected.add(entity.toString());
		}
		state.put("selected", selected);

		return state;
	}
}
