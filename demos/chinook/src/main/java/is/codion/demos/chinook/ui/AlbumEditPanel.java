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

import is.codion.demos.chinook.domain.api.Chinook.Artist;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

import static is.codion.demos.chinook.domain.api.Chinook.Album;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class AlbumEditPanel extends EntityEditPanel {

	public AlbumEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		focus().initial().set(Album.ARTIST_FK);

		createSearchField(Album.ARTIST_FK)
						.columns(15)
						// We provide a edit panel supplier, which enables
						// keyboard shortcuts for adding a new artist (INSERT)
						// or editing the currently selected one (CTRL-INSERT).
						.editPanel(this::createArtistEditPanel);
		createTextField(Album.TITLE)
						.columns(15);
		// We create a custom component for the album tags,
		// the JList it is based on is automatically associated
		// with Album.TAGS, since we use the createList() method.
		AlbumTagPanel albumTagPanel = createAlbumTagPanel();
		// We create a custom component for the album cover art
		CoverArtPanel coverArtPanel = new CoverArtPanel(editModel().editor().value(Album.COVER));
		// We set the CoverArtPanel as the component for Album.COVER,
		// so that it will appear in the input component selection dialog
		component(Album.COVER).set(coverArtPanel);

		JPanel centerPanel = flexibleGridLayoutPanel(2, 2)
						.add(createInputPanel(Album.ARTIST_FK))
						.add(createInputPanel(Album.TITLE))
						.add(createInputPanel(Album.TAGS, albumTagPanel))
						.add(createInputPanel(Album.COVER))
						.build();

		setLayout(borderLayout());
		add(centerPanel, BorderLayout.CENTER);
	}

	private AlbumTagPanel createAlbumTagPanel() {
		// We create JList based value for the album tags.
		ComponentValue<List<String>, FilterList<String>> tagsValue =
						createList(FilterListModel.<String>filterListModel())
										// The value should be based on the items in
										// the list as opposed to the selected items
										.items(Album.TAGS)
										.buildValue();
		// We then base the custom AlbumTagPanel component
		// on the above component value
		return new AlbumTagPanel(tagsValue);
	}

	private EntityEditPanel createArtistEditPanel() {
		return new ArtistEditPanel(new SwingEntityEditModel(Artist.TYPE, editModel().connectionProvider()));
	}
}
