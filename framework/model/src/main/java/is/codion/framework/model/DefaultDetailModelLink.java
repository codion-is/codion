/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import java.util.Collection;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link DetailModelLink} implementation which does nothing.
 * Override one or more methods that define the detail model behaviour.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @see #onSelection(Collection)
 * @see #onInsert(Collection)
 * @see #onUpdate(Map)
 * @see #onDelete(Collection)
 */
public class DefaultDetailModelLink<M extends DefaultEntityModel<M, E, T>, E extends AbstractEntityEditModel,
        T extends EntityTableModel<E>> implements DetailModelLink<M, E, T> {

  private final M detailModel;
  private final State activeState = State.state();

  public DefaultDetailModelLink(M detailModel) {
    this.detailModel = requireNonNull(detailModel, "detailModel");
    if (detailModel.containsTableModel()) {
      detailModel.tableModel().queryConditionRequiredState().set(true);
    }
  }

  @Override
  public final M detailModel() {
    return detailModel;
  }

  @Override
  public final StateObserver activeObserver() {
    return activeState.observer();
  }

  @Override
  public final boolean isActive() {
    return activeState.get();
  }

  @Override
  public final void setActive(boolean active) {
    activeState.set(active);
  }
}
