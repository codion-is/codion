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
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.PlaylistTrack;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.JComponent;
import java.util.Optional;

import static is.codion.swing.common.ui.component.Components.splitPane;

public final class PlaylistPanel extends EntityPanel {

	public PlaylistPanel(SwingEntityModel playlistModel) {
		super(playlistModel,
						new PlaylistTablePanel(playlistModel.tableModel()),
						// We specify our custom detail layout
						config -> config.detailLayout(PlaylistDetailLayout::new));

		SwingEntityModel playlistTrackModel = playlistModel.detailModel(PlaylistTrack.TYPE);
		EntityPanel playlistTrackPanel =
						new EntityPanel(playlistTrackModel,
										new PlaylistTrackTablePanel(playlistTrackModel.tableModel()));

		detailPanels().add(playlistTrackPanel);
	}

	private static final class PlaylistDetailLayout implements DetailLayout {

		private final PlaylistPanel playlistPanel;

		public PlaylistDetailLayout(EntityPanel playlistPanel) {
			this.playlistPanel = (PlaylistPanel) playlistPanel;
			playlistPanel.detailPanels().added().addConsumer(this::addDetailPanel);
		}

		@Override
		public Optional<JComponent> layout() {
			return Optional.of(splitPane()
							.leftComponent(playlistPanel.mainPanel())
							.rightComponent(playlistPanel.detailPanels().get(PlaylistTrack.TYPE).initialize())
							.continuousLayout(true)
							.build());
		}

		private void addDetailPanel(EntityPanel detailPanel) {
			detailPanel.displayRequested().addListener(() -> {
				// Make sure the parent panel is initializes
				playlistPanel.initialize();
				// and make sure it is displayed
				playlistPanel.requestDisplay();
			});
		}
	}
}
