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
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.demos.chinook.model.PlaylistTrackEditModel;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

final class PlaylistTrackEditPanel extends EntityEditPanel {

	PlaylistTrackEditPanel(PlaylistTrackEditModel editModel) {
		super(editModel, config -> config
						// No confirmation needed when deleting
						.deleteConfirmer(dialogOwner -> true));
		editModel.persist(PlaylistTrack.TRACK_FK).set(false);
	}

	@Override
	protected void initializeUI() {
		initialFocusAttribute().set(PlaylistTrack.TRACK_FK);
		createForeignKeySearchField(PlaylistTrack.TRACK_FK)
						.selectorFactory(new TrackSelectorFactory())
						.transferFocusOnEnter(false)
						.columns(25);

		setLayout(borderLayout());
		add(borderLayoutPanel()
						.westComponent(createLabel(PlaylistTrack.TRACK_FK).build())
						.centerComponent(component(PlaylistTrack.TRACK_FK).get())
						.border(new EmptyBorder(Layouts.GAP.get(), Layouts.GAP.get(), 0, Layouts.GAP.get()))
						.build(), BorderLayout.CENTER);
	}
}