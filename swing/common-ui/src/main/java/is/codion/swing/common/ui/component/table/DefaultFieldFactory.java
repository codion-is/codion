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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.FieldFactory;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static is.codion.swing.common.ui.component.Components.*;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.CENTER;

final class DefaultFieldFactory<C> implements FieldFactory<C> {

	private static final List<Class<?>> SUPPORTED_TYPES = Arrays.asList(
					Character.class, String.class, Boolean.class, Short.class, Integer.class, Double.class,
					BigDecimal.class, Long.class, LocalTime.class, LocalDate.class,
					LocalDateTime.class, OffsetDateTime.class);

	@Override
	public boolean supportsType(Class<?> columnClass) {
		return SUPPORTED_TYPES.contains(requireNonNull(columnClass));
	}

	@Override
	public JComponent createEqualField(ColumnConditionModel<? extends C, ?> conditionModel) {
		return createField(conditionModel)
						.link((Value<Object>) conditionModel.equalValue()).build();
	}

	@Override
	public Optional<JComponent> createUpperBoundField(ColumnConditionModel<? extends C, ?> conditionModel) {
		if (conditionModel.columnClass().equals(Boolean.class)) {
			return Optional.empty();//no upper bound field required for boolean values
		}

		return Optional.of(createField(conditionModel)
						.link((Value<Object>) conditionModel.upperBoundValue())
						.build());
	}

	@Override
	public Optional<JComponent> createLowerBoundField(ColumnConditionModel<? extends C, ?> conditionModel) {
		if (conditionModel.columnClass().equals(Boolean.class)) {
			return Optional.empty();//no lower bound field required for boolean values
		}

		return Optional.of(createField(conditionModel)
						.link((Value<Object>) conditionModel.lowerBoundValue())
						.build());
	}

	@Override
	public JComponent createInField(ColumnConditionModel<? extends C, ?> conditionModel) {
		return listBox(createField(conditionModel).buildValue(),
						(ValueSet<Object>) conditionModel.inValues())
						.build();
	}

	private static <T> ComponentBuilder<T, ? extends JComponent, ?> createField(ColumnConditionModel<?, ?> columnConditionModel) {
		Class<?> columnClass = columnConditionModel.columnClass();
		if (columnClass.equals(Boolean.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) checkBox()
							.nullable(true)
							.horizontalAlignment(CENTER);
		}
		if (columnClass.equals(Short.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) shortField()
							.format(columnConditionModel.format());
		}
		if (columnClass.equals(Integer.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) integerField()
							.format(columnConditionModel.format());
		}
		else if (columnClass.equals(Double.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) doubleField()
							.format(columnConditionModel.format());
		}
		else if (columnClass.equals(BigDecimal.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) bigDecimalField()
							.format(columnConditionModel.format());
		}
		else if (columnClass.equals(Long.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) longField()
							.format(columnConditionModel.format());
		}
		else if (columnClass.equals(LocalTime.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) localTimeField(columnConditionModel.dateTimePattern());
		}
		else if (columnClass.equals(LocalDate.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) localDateField(columnConditionModel.dateTimePattern());
		}
		else if (columnClass.equals(LocalDateTime.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) localDateTimeField(columnConditionModel.dateTimePattern());
		}
		else if (columnClass.equals(OffsetDateTime.class)) {
			return (ComponentBuilder<T, ? extends JComponent, ?>) offsetDateTimeField(columnConditionModel.dateTimePattern());
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
