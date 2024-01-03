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
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.framework.ui.component.EntitySearchField.Selector;
import is.codion.swing.framework.ui.component.EntitySearchField.TableSelector;

import java.awt.Dimension;
import java.util.function.Function;

import static is.codion.swing.framework.ui.component.EntitySearchField.tableSelector;
import static javax.swing.SortOrder.ASCENDING;

final class TrackSelectorFactory implements Function<EntitySearchModel, Selector> {

  @Override
  public TableSelector apply(EntitySearchModel searchModel) {
    TableSelector selector = tableSelector(searchModel);
    FilteredTableModel<Entity, Attribute<?>> tableModel = selector.table().getModel();
    tableModel.columnModel().setVisibleColumns(Track.ARTIST_DENORM, Track.ALBUM_FK, Track.NAME);
    tableModel.sortModel().setSortOrder(Track.ARTIST_DENORM, ASCENDING);
    tableModel.sortModel().addSortOrder(Track.ALBUM_FK, ASCENDING);
    tableModel.sortModel().addSortOrder(Track.NAME, ASCENDING);
    selector.setPreferredSize(new Dimension(500, 300));

    return selector;
  }
}
