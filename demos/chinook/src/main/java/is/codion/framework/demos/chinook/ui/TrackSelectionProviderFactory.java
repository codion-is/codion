/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.framework.ui.component.EntitySearchField.SelectionProvider;
import is.codion.swing.framework.ui.component.EntitySearchField.TableSelectionProvider;

import java.awt.Dimension;
import java.util.function.Function;

import static is.codion.swing.framework.ui.component.EntitySearchField.tableSelectionProvider;
import static javax.swing.SortOrder.ASCENDING;

final class TrackSelectionProviderFactory implements Function<EntitySearchModel, SelectionProvider> {

  @Override
  public TableSelectionProvider apply(EntitySearchModel searchModel) {
    TableSelectionProvider selectionProvider = tableSelectionProvider(searchModel);
    FilteredTableModel<Entity, Attribute<?>> tableModel = selectionProvider.table().getModel();
    tableModel.columnModel().setVisibleColumns(Track.ARTIST_DENORM, Track.ALBUM_FK, Track.NAME);
    tableModel.sortModel().setSortOrder(Track.ARTIST_DENORM, ASCENDING);
    tableModel.sortModel().addSortOrder(Track.ALBUM_FK, ASCENDING);
    tableModel.sortModel().addSortOrder(Track.NAME, ASCENDING);
    selectionProvider.setPreferredSize(new Dimension(500, 300));

    return selectionProvider;
  }
}
