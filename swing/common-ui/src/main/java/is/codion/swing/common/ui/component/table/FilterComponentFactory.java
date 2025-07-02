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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.condition.ConditionModel;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ComponentFactory;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static is.codion.swing.common.ui.component.Components.*;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

final class FilterComponentFactory implements ComponentFactory {

	private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(
					Character.class, String.class, Boolean.class, Short.class, Integer.class, Double.class,
					BigDecimal.class, Long.class, LocalTime.class, LocalDate.class,
					LocalDateTime.class, OffsetDateTime.class);

	@Override
	public boolean supportsType(Class<?> valueClass) {
		return SUPPORTED_TYPES.contains(requireNonNull(valueClass));
	}

	@Override
	public <T> JComponent equal(ConditionModel<T> conditionModel) {
		return createField(conditionModel)
						.link(conditionModel.operands().equal())
						.build();
	}

	@Override
	public <T> JComponent lower(ConditionModel<T> conditionModel) {
		return createField(conditionModel)
						.link(conditionModel.operands().lower())
						.build();
	}

	@Override
	public <T> JComponent upper(ConditionModel<T> conditionModel) {
		return createField(conditionModel)
						.link(conditionModel.operands().upper())
						.build();
	}

	@Override
	public <T> JComponent in(ConditionModel<T> conditionModel) {
		return listBox(createField(conditionModel).buildValue(), conditionModel.operands().in()).build();
	}

	private static <T> ComponentBuilder<T, ? extends JComponent, ?> createField(ConditionModel<T> conditionModel) {
		Class<T> columnClass = conditionModel.valueClass();
		if (columnClass.equals(Boolean.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) checkBox()
							.nullable(true)
							.horizontalAlignment(CENTER);
		}
		if (columnClass.equals(Short.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) shortField()
							.format(conditionModel.format().orElse(null));
		}
		if (columnClass.equals(Integer.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) integerField()
							.format(conditionModel.format().orElse(null));
		}
		else if (columnClass.equals(Double.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) doubleField()
							.format(conditionModel.format().orElse(null));
		}
		else if (columnClass.equals(BigDecimal.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) bigDecimalField()
							.format(conditionModel.format().orElse(null));
		}
		else if (columnClass.equals(Long.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) longField()
							.format(conditionModel.format().orElse(null));
		}
		else if (columnClass.equals(LocalTime.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) localTimeField()
							.dateTimePattern(conditionModel.dateTimePattern().orElseThrow());
		}
		else if (columnClass.equals(LocalDate.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) localDateField()
							.dateTimePattern(conditionModel.dateTimePattern().orElseThrow());
		}
		else if (columnClass.equals(LocalDateTime.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) localDateTimeField()
							.dateTimePattern(conditionModel.dateTimePattern().orElseThrow());
		}
		else if (columnClass.equals(OffsetDateTime.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) offsetDateTimeField()
							.dateTimePattern(conditionModel.dateTimePattern().orElseThrow());
		}
		else if (columnClass.equals(String.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) stringField();
		}
		else if (columnClass.equals(Character.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) characterField();
		}

		throw new IllegalArgumentException("Unsupported type: " + columnClass);
	}
}
