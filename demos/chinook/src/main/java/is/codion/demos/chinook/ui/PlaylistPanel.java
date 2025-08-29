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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.PlaylistTrack;
import is.codion.demos.chinook.model.PlaylistModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class PlaylistPanel extends EntityPanel {

	public PlaylistPanel(PlaylistModel playlistModel) {
		super(playlistModel,
						new PlaylistTablePanel(playlistModel.tableModel()),
						// We override initializeUI(), so we don't need a detail layout
						config -> config.detailLayout(DetailLayout.NONE));

		SwingEntityModel playlistTrackModel =
						playlistModel.detailModels().get(PlaylistTrack.TYPE);
		EntityPanel playlistTrackPanel =
						new EntityPanel(playlistTrackModel,
										new PlaylistTrackTablePanel(playlistTrackModel.tableModel()));

		// We still add the detail panel, for keyboard navigation
		detailPanels().add(playlistTrackPanel);
	}

	@Override
	protected void initializeUI() {
		setLayout(borderLayout());
		add(splitPane()
						.leftComponent(mainPanel())
						.rightComponent(detailPanels().get(PlaylistTrack.TYPE).initialize())
						.continuousLayout(true)
						.build(), BorderLayout.CENTER);
	}
}
