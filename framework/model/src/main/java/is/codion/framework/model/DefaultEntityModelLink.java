/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityModelLink} implementation which does nothing.
 * Override one or more methods the define the detail model behaviour.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @see #onSelection(List)
 * @see #onInsert(List)
 * @see #onUpdate(Map)
 * @see #onDelete(List)
 */
public class DefaultEntityModelLink<M extends DefaultEntityModel<M, E, T>, E extends DefaultEntityEditModel,
        T extends EntityTableModel<E>> implements EntityModelLink<M, E, T> {

  private final M detailModel;

  public DefaultEntityModelLink(M detailModel) {
    this.detailModel = requireNonNull(detailModel, "detailModel");
    if (detailModel.containsTableModel()) {
      detailModel.tableModel().queryConditionRequiredState().set(true);
    }
  }

  @Override
  public final M detailModel() {
    return detailModel;
  }
}
