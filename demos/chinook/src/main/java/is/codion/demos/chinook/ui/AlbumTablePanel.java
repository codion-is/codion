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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.demos.chinook.domain.api.Chinook.Album;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.image.ImagePane;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static is.codion.demos.chinook.ui.TrackTablePanel.RATINGS;
import static is.codion.swing.common.ui.component.image.ImagePane.ZoomDevice.MOUSE_WHEEL;
import static is.codion.swing.common.ui.window.Windows.screenSizeRatio;

public final class AlbumTablePanel extends EntityTablePanel {

	private final ImagePane coverPane;

	public AlbumTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						// A custom input component for editing Album.TAGS
						.editComponent(Album.TAGS, editModel -> new AlbumTagsValue())
						// Custom cell renderer for Album.RATING
						// rendering the rating as stars, i.e. *****
						.cellRenderer(Album.RATING, renderer -> renderer
										.formatter(RATINGS::get)
										.toolTip(Object::toString)));
		coverPane = ImagePane.builder()
						.preferredSize(screenSizeRatio(0.5))
						.zoomDevice(MOUSE_WHEEL)
						.navigable(true)
						.movable(true)
						.build();
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
		coverPane.image().set(readImage(coverBytes));
		if (coverPane.isShowing()) {
			Ancestor.ofType(JDialog.class).of(coverPane).optional().ifPresent(dialog -> {
				dialog.setTitle(title);
				dialog.toFront();
			});
		}
		else {
			Dialogs.builder()
							.component(coverPane)
							.owner(Ancestor.window().of(this).get())
							.title(title)
							.modal(false)
							.onClosed(dialog -> coverPane.image().clear())
							.show();
		}
	}

	private static BufferedImage readImage(byte[] bytes) {
		try {
			return ImageIO.read(new ByteArrayInputStream(bytes));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
