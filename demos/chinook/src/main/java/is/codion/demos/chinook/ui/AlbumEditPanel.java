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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.demos.chinook.domain.api.Chinook.Album;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class AlbumEditPanel extends EntityEditPanel {

	public AlbumEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		create().searchField(Album.ARTIST_FK)
						.columns(15)
						// We provide an edit panel supplier, which enables
						// keyboard shortcuts for adding a new artist (INSERT)
						// or editing the currently selected one (CTRL-INSERT).
						.editPanel(this::createArtistEditPanel);
		create().textField(Album.TITLE)
						.columns(15);
		// We use our custom component for the album tags,
		// which is automatically linked to Album.TAGS.
		component(Album.TAGS).set(new AlbumTagsPanelBuilder());
		// And our custom component for the album cover
		// which is automatically linked to Album.COVER.
		component(Album.COVER).set(new CoverArtPanelBuilder());

		JPanel centerPanel = flexibleGridLayoutPanel(2, 2)
						.add(create().inputPanel(Album.ARTIST_FK))
						.add(create().inputPanel(Album.TITLE))
						.add(create().inputPanel(Album.TAGS))
						.add(create().inputPanel(Album.COVER))
						.build();

		setLayout(borderLayout());
		add(centerPanel, BorderLayout.CENTER);
	}

	private EntityEditPanel createArtistEditPanel() {
		return new ArtistEditPanel(new SwingEntityEditModel(Artist.TYPE, editModel().connectionProvider()));
	}
}
