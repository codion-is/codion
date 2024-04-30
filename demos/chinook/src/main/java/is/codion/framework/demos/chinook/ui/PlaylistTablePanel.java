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
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import java.util.ResourceBundle;

import static is.codion.swing.framework.ui.EntityTablePanel.EntityTablePanelControl.DELETE;
import static is.codion.swing.framework.ui.EntityTablePanel.EntityTablePanelControl.EDIT_ATTRIBUTE_CONTROLS;
import static java.util.ResourceBundle.getBundle;

public final class PlaylistTablePanel extends EntityTablePanel {

	private static final ResourceBundle BUNDLE = getBundle(PlaylistTablePanel.class.getName());

	public PlaylistTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, new PlaylistEditPanel(tableModel.editModel()));
		configurePopupMenu(config -> config.clear()
						.defaults(DELETE)
						.separator()
						.control(Control.builder(this::randomPlaylist)
										.name(BUNDLE.getString("random_playlist"))
										.smallIcon(FrameworkIcons.instance().add()))
						.separator()
						.defaults());
	}

	@Override
	protected void setupControls() {
		// No need for the edit attribute controls in the popup menu
		control(EDIT_ATTRIBUTE_CONTROLS).clear();
	}

	private void randomPlaylist() throws DatabaseException {
		RandomPlaylistParametersValue playlistParametersValue = new RandomPlaylistParametersValue(tableModel().connectionProvider());
		RandomPlaylistParameters randomPlaylistParameters = Dialogs.inputDialog(playlistParametersValue)
						.owner(this)
						.title(BUNDLE.getString("random_playlist"))
						.valid(playlistParametersValue.component().parametersValid())
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
