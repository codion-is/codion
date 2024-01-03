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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.state.State;

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
  private final State active = State.state();

  public DefaultDetailModelLink(M detailModel) {
    this.detailModel = requireNonNull(detailModel, "detailModel");
    if (detailModel.containsTableModel()) {
      detailModel.tableModel().conditionRequired().set(true);
    }
  }

  @Override
  public final M detailModel() {
    return detailModel;
  }

  @Override
  public final State active() {
    return active;
  }
}
