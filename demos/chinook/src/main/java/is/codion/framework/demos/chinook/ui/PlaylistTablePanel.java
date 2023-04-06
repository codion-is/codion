/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Genre;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.model.PlaylistTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.framework.db.condition.Condition.condition;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.Objects.requireNonNull;

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
    RandomPlaylistParametersValue playlistParametersValue = new RandomPlaylistParametersValue(tableModel().connectionProvider());
    RandomPlaylistParameters randomPlaylistParameters = Dialogs.inputDialog(playlistParametersValue)
            .owner(this)
            .title(BUNDLE.getString("create_random_playlist"))
            .show();
    if (nullOrEmpty(randomPlaylistParameters.playlistName())) {
      throw new IllegalArgumentException(BUNDLE.getString("playlist_name_missing"));
    }
    if (randomPlaylistParameters.noOfTracks() == null) {
      throw new IllegalArgumentException(BUNDLE.getString("no_of_tracks_missing"));
    }
    if (requireNonNull(randomPlaylistParameters.genres()).isEmpty()) {
      throw new IllegalArgumentException(BUNDLE.getString("playlist_genre_missing"));
    }
    ((PlaylistTableModel) tableModel()).createRandomPlaylist(randomPlaylistParameters);
  }

  private static final class RandomPlaylistParametersValue
          extends AbstractComponentValue<RandomPlaylistParameters, RandomPlaylistParametersPanel> {

    private RandomPlaylistParametersValue(EntityConnectionProvider connectionProvider) {
      super(new RandomPlaylistParametersPanel(connectionProvider));
    }

    @Override
    protected RandomPlaylistParameters getComponentValue() {
      return new RandomPlaylistParameters(component().playlistNameField.getText(),
              component().noOfTracksField.getNumber(),
              component().genreList.getSelectedValuesList());
    }

    @Override
    protected void setComponentValue(RandomPlaylistParameters parameters) {
      component().playlistNameField.setText(parameters.playlistName());
      component().noOfTracksField.setNumber(parameters.noOfTracks());
      component().genreList.setSelectedIndices(parameters.genres().stream()
              .map(component().genreListModel::indexOf)
              .mapToInt(Integer::intValue)
              .toArray());
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

    private final JList<Entity> genreList;
    private final DefaultListModel<Entity> genreListModel;

    private RandomPlaylistParametersPanel(EntityConnectionProvider connectionProvider) {
      super(borderLayout());
      this.genreListModel = createGenreListModel(connectionProvider);
      this.genreList = Components.list(genreListModel)
              .selectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
              .visibleRowCount(5)
              .build();
      Components.panel(gridLayout(1, 2))
              .add(new JLabel(BUNDLE.getString("playlist_name")))
              .add(new JLabel(BUNDLE.getString("no_of_tracks")))
              .build(panel -> add(panel, BorderLayout.NORTH));
      Components.panel(gridLayout(1, 2))
              .add(playlistNameField)
              .add(noOfTracksField)
              .build(panel -> add(panel, BorderLayout.CENTER));
      Components.panel(borderLayout())
              .add(new JLabel(BUNDLE.getString("genres")), BorderLayout.NORTH)
              .add(new JScrollPane(genreList), BorderLayout.CENTER)
              .build(panel -> add(panel, BorderLayout.SOUTH));
    }

    private DefaultListModel<Entity> createGenreListModel(EntityConnectionProvider connectionProvider) {
      DefaultListModel<Entity> listModel = new DefaultListModel<>();
      try {
        connectionProvider.connection().select(condition(Genre.TYPE)
                        .selectBuilder()
                        .orderBy(ascending(Genre.NAME))
                        .build())
                .forEach(listModel::addElement);

        return listModel;
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
