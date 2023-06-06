/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.model.PlaylistTableModel;
import is.codion.swing.common.ui.component.AbstractComponentValue;
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
            .inputValidState(playlistParametersValue.component().parametersValidState())
            .show();

    ((PlaylistTableModel) tableModel()).createRandomPlaylist(randomPlaylistParameters);
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
