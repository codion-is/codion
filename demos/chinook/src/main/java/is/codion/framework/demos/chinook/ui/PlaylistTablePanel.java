/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.chinook.domain.Chinook.RandomPlaylistParameters;
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
    final RandomPlaylistParameters randomPlaylistParameters = new RandomPlaylistParametersValue()
            .showDialog(this, BUNDLE.getString("create_random_playlist"));
    if (randomPlaylistParameters.getPlaylistName() != null) {
      ((PlaylistTableModel) getTableModel()).createRandomPlaylist(randomPlaylistParameters);
    }
  }

  private static final class RandomPlaylistParametersValue
          extends AbstractComponentValue<RandomPlaylistParameters, RandomPlaylistParametersPanel> {

    private RandomPlaylistParametersValue() {
      super(new RandomPlaylistParametersPanel());
    }

    @Override
    protected RandomPlaylistParameters getComponentValue(final RandomPlaylistParametersPanel component) {
      return new RandomPlaylistParameters(component.playlistNameField.getText(), component.noOfTracksField.getInteger());
    }

    @Override
    protected void setComponentValue(final RandomPlaylistParametersPanel component,
                                     final RandomPlaylistParameters parameters) {
      component.playlistNameField.setText(parameters.getPlaylistName());
      component.noOfTracksField.setInteger(parameters.getNoOfTracks());
    }
  }

  private static final class RandomPlaylistParametersPanel extends JPanel {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(RandomPlaylistParametersPanel.class.getName());

    private final JTextField playlistNameField = Components.textField()
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(10)
            .build();
    private final IntegerField noOfTracksField = Components.integerField()
            .range(1, 5000)
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(3)
            .build();

    private RandomPlaylistParametersPanel() {
      super(borderLayout());
      Panels.builder(gridLayout(1, 2))
              .add(new JLabel(BUNDLE.getString("playlist_name")))
              .add(new JLabel(BUNDLE.getString("no_of_tracks")))
              .build(panel -> add(panel, BorderLayout.NORTH));
      Panels.builder(gridLayout(1, 2))
              .add(playlistNameField)
              .add(noOfTracksField)
              .build(panel -> add(panel, BorderLayout.CENTER));
    }
  }
}