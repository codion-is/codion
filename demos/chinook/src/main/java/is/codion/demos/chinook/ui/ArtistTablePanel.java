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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.Text;
import is.codion.common.model.CancelException;
import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.demos.chinook.model.ArtistTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static is.codion.framework.db.EntityConnection.Count.where;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;

public final class ArtistTablePanel extends EntityTablePanel {

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
						.caption("Combine...")
						.enabled(tableModel().selection().multiple())
						.build();
	}

	private void combineSelected() {
		List<Entity> selectedArtists = tableModel().selection().items().get();
		Entity artistToKeep = Dialogs.select()
						.list(selectedArtists)
						.owner(this)
						.title("Select the artist to keep")
						.comparator(Text.collator())
						.select()
						.single()
						.orElseThrow(CancelException::new);

		List<Entity> artistsToDelete = new ArrayList<>(selectedArtists);
		artistsToDelete.remove(artistToKeep);
		int albumCount = tableModel().connection().count(where(Album.ARTIST_FK.in(artistsToDelete)));
		if (confirmCombination(artistsToDelete, artistToKeep, albumCount)) {
			ArtistTableModel tableModel = (ArtistTableModel) tableModel();
			Dialogs.progressWorker()
							.task(() -> tableModel.combine(artistsToDelete, artistToKeep))
							.owner(this)
							.title("Updating albums...")
							.onResult(() -> {
								tableModel.onCombined(artistsToDelete, artistToKeep);
								showMessageDialog(this, "Artists combined!");
							})
							.execute();
		}
	}

	private boolean confirmCombination(List<Entity> artistsToDelete, Entity artistToKeep, int albumCount) {
		StringBuilder message = new StringBuilder();
		if (albumCount > 0) {
			message.append("Associate ").append(albumCount).append(" albums(s) ").append(lineSeparator())
							.append("with ").append(artistToKeep).append("?").append(lineSeparator());
		}
		message.append("Delete the following:").append(lineSeparator())
						.append(artistsToDelete.stream()
										.map(Objects::toString)
										.collect(joining(lineSeparator()))).append(lineSeparator())
						.append("while keeping: ").append(artistToKeep).append("?");

		return showConfirmDialog(ArtistTablePanel.this, message,
						"Confirm artist combination", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
	}
}
