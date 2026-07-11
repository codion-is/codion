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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Operands;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.inspect.UiInspector;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * A {@link UiInspector} exposing the state of the {@link ConditionModel} behind the focused condition field —
 * its attribute, whether it is a query <b>condition</b> or a client-side <b>filter</b>, its operator, operands
 * and enabled state. Applies only when focus is within a {@link ColumnConditionPanel}, so it complements
 * {@link EntityTableModelInspector}, which reports the table's rows and selection for focus elsewhere in the
 * panel. Located via {@link java.util.ServiceLoader}, so the consumer stays framework-agnostic.
 */
public final class EntityConditionInspector implements UiInspector {

	@Override
	public Optional<Map<String, Object>> state(Component focusOwner) {
		ColumnConditionPanel<?> conditionPanel = Ancestor.ofType(ColumnConditionPanel.class).of(focusOwner).get();
		if (conditionPanel == null) {
			return Optional.empty();
		}
		EntityTablePanel tablePanel = Ancestor.ofType(EntityTablePanel.class).of(focusOwner).get();
		if (tablePanel == null) {
			return Optional.empty();
		}

		return state(tablePanel.tableModel(), conditionPanel.model());
	}

	//package-private for direct testing without realizing the component hierarchy
	static Optional<Map<String, Object>> state(SwingEntityTableModel tableModel, ConditionModel<?> focused) {
		//the condition and filter models are distinct instances per attribute, so identity locates both which
		//system the focused field belongs to and its attribute; the field itself carries neither
		Attribute<?> attribute = identifier(tableModel.query().condition(), focused);
		String type = "condition";
		if (attribute == null) {
			attribute = identifier(tableModel.filters(), focused);
			type = "filter";
		}
		if (attribute == null) {
			return Optional.empty();
		}
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("entityType", tableModel.entityDefinition().type().toString());
		state.put("type", type);
		state.put("attribute", attribute.toString());
		state.put("enabled", focused.enabled().is());
		state.put("operator", focused.operator().get().name());
		state.put("operands", operands(focused.operands()));

		return Optional.of(state);
	}

	private static @Nullable Attribute<?> identifier(TableConditionModel<Attribute<?>> conditions, ConditionModel<?> focused) {
		return conditions.get().entrySet().stream()
						.filter(entry -> entry.getValue() == focused)
						.map(Map.Entry::getKey)
						.findFirst()
						.orElse(null);
	}

	private static Map<String, Object> operands(Operands<?> operands) {
		//only the operands the focused operator uses hold a value; the others are absent rather than null
		Map<String, Object> state = new LinkedHashMap<>();
		putIfPresent(state, "equal", operands.equal().get());
		putIfPresent(state, "lower", operands.lower().get());
		putIfPresent(state, "upper", operands.upper().get());
		Collection<?> in = operands.in().get();
		if (!in.isEmpty()) {
			state.put("in", in.stream().map(String::valueOf).collect(toList()));
		}

		return state;
	}

	private static void putIfPresent(Map<String, Object> state, String key, @Nullable Object value) {
		if (value != null) {
			//stringified, an operand can be an Entity, a foreign key condition, which is not JSON friendly
			state.put(key, String.valueOf(value));
		}
	}
}
