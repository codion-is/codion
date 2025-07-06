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

import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.plugin.imagepanel.NavigableImagePanel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.transfer.FileTransferHandler;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEtchedBorder;

/**
 * A panel for displaying a cover image, based on a byte array.
 */
final class CoverArtPanel extends JPanel {

	private static final ResourceBundle BUNDLE = getBundle(CoverArtPanel.class.getName());
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	private static final Dimension EMBEDDED_SIZE = new Dimension(200, 200);
	private static final Dimension DIALOG_SIZE = new Dimension(400, 400);
	private static final FileNameExtensionFilter IMAGE_FILE_FILTER =
					new FileNameExtensionFilter(BUNDLE.getString("images"),
									new String[] {"jpg", "jpeg", "png", "bmp", "gif"});

	private final JButton addButton;
	private final JButton removeButton;
	private final JPanel centerPanel;
	private final NavigableImagePanel imagePanel;
	private final Value<byte[]> imageBytes;
	private final State imageSelected;
	private final State embedded = State.state(true);

	/**
	 * @param imageBytes the image bytes value to base this panel on.
	 */
	CoverArtPanel(Value<byte[]> imageBytes) {
		super(borderLayout());
		this.imageBytes = imageBytes;
		this.imageSelected = State.state(!imageBytes.isNull());
		this.imagePanel = createImagePanel();
		this.addButton = button()
						.control(Control.builder()
										.command(this::addCover)
										.smallIcon(ICONS.get(Foundation.PLUS)))
						.transferFocusOnEnter(true)
						.build();
		this.removeButton = button()
						.control(Control.builder()
										.command(this::removeCover)
										.smallIcon(ICONS.get(Foundation.MINUS)))
						.transferFocusOnEnter(true)
						.enabled(imageSelected)
						.build();
		this.centerPanel = createCenterPanel();
		add(centerPanel, BorderLayout.CENTER);
		bindEvents();
	}

	@Override
	public boolean requestFocusInWindow() {
		// The panel itself is not focusable,
		// request focus for the add button instead
		return addButton.requestFocusInWindow();
	}

	private JPanel createCenterPanel() {
		return borderLayoutPanel()
						.preferredSize(EMBEDDED_SIZE)
						.centerComponent(imagePanel)
						.southComponent(borderLayoutPanel()
										.eastComponent(panel(new GridLayout(1, 2, 0, 0))
														.addAll(addButton, removeButton)
														.build())
										.build())
						.build();
	}

	private void bindEvents() {
		imageBytes.addConsumer(bytes -> imagePanel.setImage(readImage(bytes)));
		imageBytes.addConsumer(bytes -> imageSelected.set(bytes != null));
		embedded.addConsumer(this::setEmbedded);
		imagePanel.addMouseListener(new EmbeddingMouseListener());
	}

	private void addCover() throws IOException {
		File coverFile = Dialogs.select()
						.files()
						.owner(this)
						.title(BUNDLE.getString("select_image"))
						.filter(IMAGE_FILE_FILTER)
						.selectFile();
		imageBytes.set(Files.readAllBytes(coverFile.toPath()));
	}

	private void removeCover() {
		imageBytes.clear();
	}

	private void setEmbedded(boolean embedded) {
		configureImagePanel(embedded);
		if (embedded) {
			embed();
		}
		else {
			displayInDialog();
		}
	}

	private void embed() {
		Utilities.disposeParentWindow(centerPanel);
		centerPanel.setSize(EMBEDDED_SIZE);
		imagePanel.resetView();
		add(centerPanel, BorderLayout.CENTER);
		revalidate();
		repaint();
	}

	private void displayInDialog() {
		remove(centerPanel);
		revalidate();
		repaint();
		Dialogs.builder()
						.component(centerPanel)
						.owner(this)
						.modal(false)
						.title(BUNDLE.getString("cover"))
						.onClosed(windowEvent -> embedded.set(true))
						.onOpened(windowEvent -> imagePanel.resetView())
						.size(DIALOG_SIZE)
						.show();
	}

	private void configureImagePanel(boolean embedded) {
		imagePanel.setZoomDevice(embedded ? NavigableImagePanel.ZoomDevice.NONE : NavigableImagePanel.ZoomDevice.MOUSE_WHEEL);
		imagePanel.setMoveImageEnabled(!embedded);
	}

	private NavigableImagePanel createImagePanel() {
		NavigableImagePanel panel = new NavigableImagePanel();
		panel.setZoomDevice(NavigableImagePanel.ZoomDevice.NONE);
		panel.setNavigationImageEnabled(false);
		panel.setMoveImageEnabled(false);
		panel.setTransferHandler(new CoverTransferHandler());
		panel.setBorder(createEtchedBorder());

		return panel;
	}

	private static BufferedImage readImage(byte[] bytes) {
		if (bytes == null) {
			return null;
		}
		try {
			return ImageIO.read(new ByteArrayInputStream(bytes));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private final class EmbeddingMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getClickCount() == 2) {
				embedded.set(!embedded.get());
			}
		}
	}

	private final class CoverTransferHandler extends FileTransferHandler {

		@Override
		protected boolean importFiles(Component component, List<File> files) {
			try {
				if (singleImage(files)) {
					imageBytes.set(Files.readAllBytes(files.get(0).toPath()));

					return true;
				}

				return false;
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private boolean singleImage(List<File> files) {
			return files.size() == 1 && IMAGE_FILE_FILTER.accept(files.get(0));
		}
	}
}
