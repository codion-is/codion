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

import is.codion.demos.chinook.domain.api.Chinook.Track;
import is.codion.demos.chinook.model.GenreModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.TabbedDetailLayout;

import static is.codion.swing.framework.ui.EntityPanel.PanelState.HIDDEN;

public final class GenrePanel extends EntityPanel {

	public GenrePanel(GenreModel genreModel) {
		super(genreModel, new GenreEditPanel(genreModel.editModel()), config ->
						config.detailLayout(entityPanel -> TabbedDetailLayout.builder()
										.panel(entityPanel)
										.initialDetailState(HIDDEN)
										.build()));
		detailPanels().add(new EntityPanel(genreModel.detailModels().get(Track.TYPE)));
	}
}
