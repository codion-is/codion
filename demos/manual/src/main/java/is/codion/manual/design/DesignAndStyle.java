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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.manual.design;

import is.codion.common.event.Event;
import is.codion.common.observable.Observer;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueList;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.manual.store.domain.Store.Customer;
import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.text.TemporalField;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static is.codion.common.event.Event.event;
import static is.codion.framework.model.EntityTableConditionModel.entityTableConditionModel;
import static javax.swing.BorderFactory.createTitledBorder;

public final class DesignAndStyle {

	public static void main(String[] args) {
		LocalEntityConnectionProvider connectionProvider = null;
		Entity entity = null;
		FilterTableModel<List<String>, Integer> tableModel = null;
		FilterTable<List<String>, Integer> table = null;

		//tag::factories[]
		Event<String> event = event(); // Event.event()

		Value<Integer> value = Value.nullable();

		State state = State.state(true);

		EntityTableConditionModel conditionModel =
						entityTableConditionModel(Customer.TYPE, connectionProvider);
		//end::factories[]

		//tag::builders[]
		TaskScheduler scheduler =
						TaskScheduler.builder()
										.task(() -> {})
										.interval(5, TimeUnit.SECONDS)
										.initialDelay(15)
										.build();

		TemporalField<LocalDate> field =
						TemporalField.builder(LocalDate.class)
										.dateTimePattern("dd.MM.yyyy")
										.columns(12)
										.border(createTitledBorder("Date"))
										.build();
		//end::builders[]

		//tag::accessors[]
		Observer<String> observer = event.observer();

		LocalEntityConnection connection = connectionProvider.connection();

		boolean modified = entity.modified();

		Entity.Key primaryKey = entity.primaryKey();
		//end::accessors[]

		//tag::mutable[]
		FilterListSelection<List<String>> selection = tableModel.selection();

		List<Integer> selectedIndexes = selection.indexes().get();

		selection.indexes().set(List.of(0, 1, 2));

		selection.items().addListener(() -> System.out.println("Selected items changed"));

		table.sortable().set(false);
		//end::mutable[]

		//tag::exceptions[]
		Value<Integer> integer = Value.nullable();

		boolean isNull = integer.isNull();
		boolean isNullable = integer.isNullable();

		ValueList<Integer> integers = ValueList.valueList();

		boolean isEmpty = integers.isEmpty();
		//end::exceptions[]
	}
}
