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

import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class PlaylistTrackEditPanel extends EntityEditPanel {

  public PlaylistTrackEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(PlaylistTrack.PLAYLIST_FK);

    createForeignKeyComboBox(PlaylistTrack.PLAYLIST_FK);
    createForeignKeySearchField(PlaylistTrack.TRACK_FK)
            .selectorFactory(new TrackSelectorFactory())
            .columns(30);

    setLayout(gridLayout(2, 1));
    addInputPanel(PlaylistTrack.PLAYLIST_FK);
    addInputPanel(PlaylistTrack.TRACK_FK);
  }
}