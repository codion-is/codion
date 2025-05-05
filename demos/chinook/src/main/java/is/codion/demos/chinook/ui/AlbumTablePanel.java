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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.plugin.imagepanel.NavigableImagePanel;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.component.EditComponentFactory;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static is.codion.demos.chinook.ui.TrackTablePanel.RATINGS;

public final class AlbumTablePanel extends EntityTablePanel {

	private final NavigableImagePanel coverPanel;

	public AlbumTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						// A custom input component for editing Album.TAGS
						.editComponentFactory(Album.TAGS, new TagEditComponentFactory())
						// Custom cell renderer for Album.RATING
						// rendering the rating as stars, i.e. *****
						.cellRenderer(Album.RATING, EntityTableCellRenderer.builder(Album.RATING, tableModel)
										.string(RATINGS::get)
										.toolTipData(true)
										.build()));
		coverPanel = new NavigableImagePanel();
		coverPanel.setPreferredSize(Windows.screenSizeRatio(0.5));
		table().doubleClick().set(viewCoverControl());
	}

	private Control viewCoverControl() {
		return Control.builder()
						.command(this::viewSelectedCover)
						.enabled(tableModel().selection().single())
						.build();
	}

	private void viewSelectedCover() {
		tableModel().selection().item().optional()
						.filter(album -> !album.isNull(Album.COVER))
						.ifPresent(album -> displayCover(album.get(Album.TITLE), album.get(Album.COVER)));
	}

	private void displayCover(String title, byte[] coverBytes) {
		coverPanel.setImage(readImage(coverBytes));
		if (coverPanel.isShowing()) {
			JDialog dialog = Utilities.parentDialog(coverPanel);
			dialog.setTitle(title);
			dialog.toFront();
		}
		else {
			Dialogs.componentDialog(coverPanel)
							.owner(Utilities.parentWindow(this))
							.title(title)
							.modal(false)
							.onClosed(dialog -> coverPanel.setImage(null))
							.show();
		}
	}

	private static BufferedImage readImage(byte[] bytes) {
		try {
			return ImageIO.read(new ByteArrayInputStream(bytes));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static final class TagEditComponentFactory
					implements EditComponentFactory<List<String>, AlbumTagPanel> {

		@Override
		public ComponentValue<List<String>, AlbumTagPanel> component(SwingEntityEditModel editModel,
																																 List<String> value) {
			return new TagComponentValue(value);
		}
	}

	private static final class TagComponentValue extends AbstractComponentValue<List<String>, AlbumTagPanel> {

		private TagComponentValue(List<String> tags) {
			super(new AlbumTagPanel(Components.list(FilterListModel.<String>filterListModel())
							// A list component value based on the items in
							// the model, as opposed to the selected items
							.items()
							// The initial tags to display
							.value(tags)
							.buildValue()));
		}

		@Override
		protected List<String> getComponentValue() {
			return component().tagsValue().get();
		}

		@Override
		protected void setComponentValue(List<String> value) {
			component().tagsValue().set(value);
		}
	}
}
