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

import is.codion.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;

import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class PlaylistPanel extends EntityPanel {

	public PlaylistPanel(SwingEntityModel playlistModel) {
		super(playlistModel,
						new PlaylistTablePanel(playlistModel.tableModel()),
						config -> config.detailLayout(DetailLayout.NONE));

		SwingEntityModel playlistTrackModel = playlistModel.detailModel(PlaylistTrack.TYPE);
		EntityPanel playlistTrackPanel = new EntityPanel(playlistTrackModel,
						new PlaylistTrackEditPanel(playlistTrackModel.editModel()),
						new PlaylistTrackTablePanel(playlistTrackModel.tableModel()));

		addDetailPanel(playlistTrackPanel);
	}

	@Override
	protected void initializeUI() {
		setLayout(borderLayout());
		add(splitPane()
						.leftComponent(mainPanel())
						.rightComponent(detailPanel(PlaylistTrack.TYPE).initialize())
						.continuousLayout(true)
						.build(), BorderLayout.CENTER);
	}
}
