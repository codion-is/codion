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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.value.Value.Validator;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Track.RaisePriceParameters;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.math.BigDecimal;
import java.util.Collection;

import static is.codion.framework.demos.chinook.domain.Chinook.Track;

public final class TrackTableModel extends SwingEntityTableModel {

  private static final int DEFAULT_LIMIT = 1_000;
  private static final int MAXIMUM_LIMIT = 10_000;

  public TrackTableModel(EntityConnectionProvider connectionProvider) {
    super(Track.TYPE, connectionProvider);
    editable().set(true);
    configureLimit();
  }

  public void raisePriceOfSelected(BigDecimal increase) throws DatabaseException {
    if (selectionModel().selectionNotEmpty().get()) {
      Collection<Long> trackIds = Entity.values(Track.ID, selectionModel().getSelectedItems());
      Collection<Entity> result = connection()
              .execute(Track.RAISE_PRICE, new RaisePriceParameters(trackIds, increase));
      replace(result);
    }
  }

  private void configureLimit() {
    limit().set(DEFAULT_LIMIT);
    limit().addListener(this::refresh);
    limit().addValidator(new LimitValidator());
  }

  private static final class LimitValidator implements Validator<Integer> {

    @Override
    public void validate(Integer value) {
      if (value != null && value > MAXIMUM_LIMIT) {
        // The error message is never displayed, so not required
        throw new IllegalArgumentException();
      }
    }
  }
}
