/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.chinook.model.PlaylistTableModel;
import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.panel.Panels;
import is.codion.swing.common.ui.textfield.IntegerField;
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

  public PlaylistTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected Controls getPopupControls(final List<Controls> additionalPopupControls) {
    final Controls popupControls = super.getPopupControls(additionalPopupControls);
    popupControls.addAt(0, Control.builder(this::createRandomPlaylist)
            .caption(BUNDLE.getString("create_random_playlist"))
            .build());
    popupControls.addSeparatorAt(1);

    return popupControls;
  }

  private void createRandomPlaylist() throws DatabaseException {
    final RandomPlaylistParameters randomPlaylistParameters = new RandomPlaylistParameterValue()
            .showDialog(this, BUNDLE.getString("create_random_playlist"));
    if (randomPlaylistParameters.playlistName != null && randomPlaylistParameters.noOfTracks != null) {
      ((PlaylistTableModel) getTableModel()).createRandomPlaylist(randomPlaylistParameters.playlistName, randomPlaylistParameters.noOfTracks);
    }
  }

  private static class RandomPlaylistParameters {

    private final String playlistName;
    private final Integer noOfTracks;

    private RandomPlaylistParameters(final String playlistName, final Integer noOfTracks) {
      this.playlistName = playlistName;
      this.noOfTracks = noOfTracks;
    }
  }

  private static final class RandomPlaylistParameterValue
          extends AbstractComponentValue<RandomPlaylistParameters, RandomPlaylistParameterPanel> {

    private RandomPlaylistParameterValue() {
      super(new RandomPlaylistParameterPanel());
    }

    @Override
    protected RandomPlaylistParameters getComponentValue(final RandomPlaylistParameterPanel component) {
      return new RandomPlaylistParameters(component.playlistNameField.getText(), component.noOfTracks.getInteger());
    }

    @Override
    protected void setComponentValue(final RandomPlaylistParameterPanel component,
                                     final RandomPlaylistParameters playlistParameters) {
      component.playlistNameField.setText(playlistParameters.playlistName);
      component.noOfTracks.setInteger(playlistParameters.noOfTracks);
    }
  }

  private static final class RandomPlaylistParameterPanel extends JPanel {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(RandomPlaylistParameterPanel.class.getName());

    private final JTextField playlistNameField = Components.textField()
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(10)
            .build();
    private final IntegerField noOfTracks = Components.integerField()
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(3)
            .build();

    private RandomPlaylistParameterPanel() {
      super(borderLayout());
      Panels.builder(gridLayout(1, 2))
              .add(new JLabel(BUNDLE.getString("playlist_name")))
              .add(new JLabel(BUNDLE.getString("no_of_tracks")))
              .build(panel -> add(panel, BorderLayout.NORTH));
      Panels.builder(gridLayout(1, 2))
              .add(playlistNameField)
              .add(noOfTracks)
              .build(panel -> add(panel, BorderLayout.CENTER));
    }
  }
}
