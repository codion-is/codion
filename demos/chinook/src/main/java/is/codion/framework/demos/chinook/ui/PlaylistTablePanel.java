/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.model.PlaylistTableModel;
import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class PlaylistTablePanel extends EntityTablePanel {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(PlaylistTablePanel.class.getName());

  public PlaylistTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    return super.createPopupMenuControls(additionalPopupMenuControls)
            .addAt(0, Control.builder(this::createRandomPlaylist)
                    .caption(BUNDLE.getString("create_random_playlist"))
                    .build())
            .addSeparatorAt(1);
  }

  private void createRandomPlaylist() throws DatabaseException {
    RandomPlaylistParameters randomPlaylistParameters = Dialogs.inputDialog(new RandomPlaylistParametersValue())
            .owner(this)
            .title(BUNDLE.getString("create_random_playlist"))
            .show();
    if (randomPlaylistParameters.playlistName() != null) {
      ((PlaylistTableModel) tableModel()).createRandomPlaylist(randomPlaylistParameters);
    }
  }

  private static final class RandomPlaylistParametersValue
          extends AbstractComponentValue<RandomPlaylistParameters, RandomPlaylistParametersPanel> {

    private RandomPlaylistParametersValue() {
      super(new RandomPlaylistParametersPanel());
    }

    @Override
    protected RandomPlaylistParameters getComponentValue() {
      return new RandomPlaylistParameters(component().playlistNameField.getText(), component().noOfTracksField.getNumber());
    }

    @Override
    protected void setComponentValue(RandomPlaylistParameters parameters) {
      component().playlistNameField.setText(parameters.playlistName());
      component().noOfTracksField.setNumber(parameters.noOfTracks());
    }
  }

  private static final class RandomPlaylistParametersPanel extends JPanel {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(RandomPlaylistParametersPanel.class.getName());

    private final JTextField playlistNameField = Components.textField()
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(10)
            .build();
    private final NumberField<Integer> noOfTracksField = Components.integerField()
            .valueRange(1, 5000)
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(3)
            .build();

    private RandomPlaylistParametersPanel() {
      super(borderLayout());
      Components.panel(gridLayout(1, 2))
              .add(new JLabel(BUNDLE.getString("playlist_name")))
              .add(new JLabel(BUNDLE.getString("no_of_tracks")))
              .build(panel -> add(panel, BorderLayout.NORTH));
      Components.panel(gridLayout(1, 2))
              .add(playlistNameField)
              .add(noOfTracksField)
              .build(panel -> add(panel, BorderLayout.CENTER));
    }
  }
}
