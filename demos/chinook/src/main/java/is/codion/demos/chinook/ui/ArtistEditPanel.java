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

import is.codion.demos.chinook.domain.api.Chinook.ArtistTag;
import is.codion.demos.chinook.model.ArtistEditModel;
import is.codion.swing.common.ui.component.panel.GridLayoutPanelBuilder;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EditorComponents;
import is.codion.swing.framework.ui.EditorComponents.CreateComponents;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JLabel;
import java.awt.BorderLayout;

import static is.codion.demos.chinook.domain.api.Chinook.Artist;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

public final class ArtistEditPanel extends EntityEditPanel {

	public ArtistEditPanel(SwingEntityEditModel editModel) {
		super(editModel);
	}

	@Override
	protected void initializeUI() {
		create().textField(Artist.NAME)
						.columns(18);

		GridLayoutPanelBuilder tagPanel = gridLayoutPanel(3, 2);
		for (int i = 0; i < ArtistEditModel.TAG_SLOTS; i++) {
			addTagPanel(i, tagPanel);
		}
		setLayout(borderLayout());
		addInputPanel(Artist.NAME, BorderLayout.NORTH);
		add(tagPanel, BorderLayout.CENTER);
	}

	private void addTagPanel(int i, GridLayoutPanelBuilder tagPanel) {
		String detailName = ArtistEditModel.TAG_PREFIX + i;
		EditorComponents artistTag = components().detail(detailName);
		CreateComponents create = artistTag.create();
		create.textField(ArtistTag.TAG)
						.columns(8);
		JLabel label = artistTag.component(ArtistTag.TAG).label();
		label.setText(label.getText() + " " + (i + 1));
		tagPanel.add(create.inputPanel(ArtistTag.TAG)
						.label(label));
	}
}