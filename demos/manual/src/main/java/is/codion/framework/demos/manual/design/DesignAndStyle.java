/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.design;

import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.demos.manual.store.domain.Store.Customer;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.model.component.table.FilteredTableSelectionModel;
import is.codion.swing.common.ui.component.text.TemporalField;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static is.codion.common.event.Event.event;
import static is.codion.common.state.State.state;
import static is.codion.common.value.Value.value;
import static is.codion.framework.model.EntityTableConditionModel.entityTableConditionModel;
import static javax.swing.BorderFactory.createTitledBorder;

public final class DesignAndStyle {

  public static void main(String[] args) {
    LocalEntityConnectionProvider connectionProvider = null;
    Entity entity = null;
    FilteredTableSelectionModel<List<String>> selectionModel = null;

    //tag::factories[]
    Event<String> event = event();              // Event.event()

    Value<Integer> value = value(42); // Value.value()

    State state = state(true);            // State.state()

    EntityTableConditionModel<Attribute<?>> conditionModel =
            entityTableConditionModel(Customer.TYPE, connectionProvider);
    //end::factories[]

    //tag::builders[]
    TaskScheduler scheduler =
            TaskScheduler.builder(() -> {})
                    .interval(5, TimeUnit.SECONDS)
                    .initialDelay(15)
                    .build();

    TemporalField<LocalDate> field =
            TemporalField.builder(LocalDate.class, "dd.MM.yyyy")
                    .columns(12)
                    .border(createTitledBorder("Date"))
                    .build();
    //end::builders[]

    //tag::accessors[]
    EventObserver<String> observer = event.observer();

    LocalEntityConnection connection = connectionProvider.connection();

    boolean modified = entity.modified();

    Entity.Key primaryKey = entity.primaryKey();
    //end::accessors[]

    //tag::getters[]
    boolean optimisticLocking = connection.isOptimisticLocking();

    connection.setOptimisticLocking(false);

    List<Integer> selectedIndexes = selectionModel.getSelectedIndexes();

    selectionModel.setSelectedIndexes(Arrays.asList(0, 1, 2));
    //end::getters[]
  }
}
