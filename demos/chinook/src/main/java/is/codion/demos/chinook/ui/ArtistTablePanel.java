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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.model.CancelException;
import is.codion.common.utilities.Text;
import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.model.ArtistTableModel;
import is.codion.demos.chinook.model.ArtistTableModel.CombineArtistsTask;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static is.codion.framework.db.EntityConnection.Count.where;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;

public final class ArtistTablePanel extends EntityTablePanel {

	private static final ResourceBundle BUNDLE = getBundle(ArtistTablePanel.class.getName());

	public ArtistTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel);
		configurePopupMenu(layout -> layout.clear()
						.control(createCombineControl())
						.separator()
						.defaults());
	}

	private Control createCombineControl() {
		return Control.builder()
						.command(this::combineSelected)
						.caption(BUNDLE.getString("combine") + "...")
						.enabled(tableModel().selection().multiple())
						.build();
	}

	private void combineSelected() {
		List<Entity> selectedArtists = tableModel().selection().items().get();
		Entity artistToKeep = Dialogs.select()
						.list(selectedArtists)
						.owner(this)
						.title(BUNDLE.getString("select_artists_to_keep"))
						.comparator(Text.collator())
						.select()
						.single()
						.orElseThrow(CancelException::new);

		List<Entity> artistsToDelete = new ArrayList<>(selectedArtists);
		artistsToDelete.remove(artistToKeep);
		int albumCount = tableModel().connection().count(where(Album.ARTIST_FK.in(artistsToDelete)));
		if (confirmCombination(artistsToDelete, artistToKeep, albumCount)) {
			CombineArtistsTask task = ((ArtistTableModel) tableModel()).combine(artistsToDelete, artistToKeep);
			Dialogs.progressWorker()
							.task(task)
							.owner(this)
							.title(BUNDLE.getString("combining_artists") + "...")
							.onResult(this::onArtistsCombined)
							.execute();
		}
	}

	private void onArtistsCombined() {
		showMessageDialog(this, BUNDLE.getString("artists_combined"));
	}

	private boolean confirmCombination(List<Entity> artistsToDelete, Entity artistToKeep, int albumCount) {
		StringBuilder message = new StringBuilder();
		if (albumCount > 0) {
			message.append(BUNDLE.getString("associate") + " ").append(albumCount).append(" " + BUNDLE.getString("albums"))
							.append(" " + BUNDLE.getString("with") + ":").append("\n\n").append(artistToKeep).append("?").append("\n\n");
		}
		message.append(BUNDLE.getString("delete_the_following") + ":").append("\n\n")
						.append(artistsToDelete.stream()
										.map(Objects::toString)
										.collect(joining("\n"))).append("\n\n")
						.append(BUNDLE.getString("while_keeping") + ": ").append(artistToKeep).append("?");

		return showConfirmDialog(ArtistTablePanel.this, message,
						BUNDLE.getString("confirm_artist_combination"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}
}
