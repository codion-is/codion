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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.model.PlaylistTableModel;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import java.util.List;
import java.util.ResourceBundle;

public final class PlaylistTablePanel extends EntityTablePanel {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(PlaylistTablePanel.class.getName());

  public PlaylistTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    return super.createPopupMenuControls(additionalPopupMenuControls)
            .addAt(0, Control.builder(this::createRandomPlaylist)
                    .name(BUNDLE.getString("create_random_playlist"))
                    .smallIcon(FrameworkIcons.instance().add())
                    .build())
            .addSeparatorAt(1);
  }

  private void createRandomPlaylist() throws DatabaseException {
    RandomPlaylistParametersValue playlistParametersValue = new RandomPlaylistParametersValue(tableModel().connectionProvider());
    RandomPlaylistParameters randomPlaylistParameters = Dialogs.inputDialog(playlistParametersValue)
            .owner(this)
            .title(BUNDLE.getString("create_random_playlist"))
            .inputValid(playlistParametersValue.component().parametersValid())
            .show();

    PlaylistTableModel playlistTableModel = tableModel();
    playlistTableModel.createRandomPlaylist(randomPlaylistParameters);
  }

  private static final class RandomPlaylistParametersValue
          extends AbstractComponentValue<RandomPlaylistParameters, RandomPlaylistParametersPanel> {

    private RandomPlaylistParametersValue(EntityConnectionProvider connectionProvider) {
      super(new RandomPlaylistParametersPanel(connectionProvider));
    }

    @Override
    protected RandomPlaylistParameters getComponentValue() {
      return component().get();
    }

    @Override
    protected void setComponentValue(RandomPlaylistParameters parameters) {/* Read only value, not required */}
  }
}
