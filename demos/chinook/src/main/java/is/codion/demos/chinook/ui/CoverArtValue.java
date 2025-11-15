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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.image.ImagePane;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.transfer.FileTransferHandler;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.common.reactive.state.State.present;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEtchedBorder;

final class CoverArtValue extends AbstractComponentValue<CoverArtValue.CoverArtPanel, byte[]> {

	private static final ResourceBundle BUNDLE = getBundle(CoverArtValue.class.getName());

	CoverArtValue() {
		super(new CoverArtPanel());
		component().value.addListener(this::notifyObserver);
	}

	@Override
	protected byte[] getComponentValue() {
		return component().value.get();
	}

	@Override
	protected void setComponentValue(byte[] value) {
		component().value.set(value);
	}

	static final class CoverArtPanel extends JPanel {

		private static final FrameworkIcons ICONS = FrameworkIcons.instance();

		private static final Dimension DIALOG_SIZE = new Dimension(400, 400);
		private static final FileNameExtensionFilter IMAGE_FILE_FILTER =
						new FileNameExtensionFilter(BUNDLE.getString("images"),
										new String[] {"jpg", "jpeg", "png", "bmp", "gif"});

		private final ComponentValue<ImagePane, byte[]> value = ImagePane.builder()
						.transferHandler(new CoverTransferHandler())
						.mouseListener(new EmbeddingMouseListener())
						.border(createEtchedBorder())
						.autoResize(true)
						.buildValue();
		private final ImagePane imagePane = value.component();
		private final JButton addButton = button()
						.control(Control.builder()
										.command(this::addCover)
										.smallIcon(ICONS.get(Foundation.PLUS).small()))
						.transferFocusOnEnter(true)
						.build();
		private final JButton removeButton = button()
						.control(Control.builder()
										.command(this::removeCover)
										.smallIcon(ICONS.get(Foundation.MINUS).small()))
						.transferFocusOnEnter(true)
						.enabled(present(value))
						.build();
		private final JPanel centerPanel = createCenterPanel();
		private final State embedded = State.builder()
						.value(true)
						.consumer(this::setEmbedded)
						.build();

		CoverArtPanel() {
			super(borderLayout());
			add(centerPanel, BorderLayout.CENTER);
		}

		@Override
		public boolean requestFocusInWindow() {
			// The panel itself is not focusable,
			// request focus for the add button instead
			return addButton.requestFocusInWindow();
		}

		private JPanel createCenterPanel() {
			return borderLayoutPanel()
							.center(imagePane)
							.south(borderLayoutPanel()
											.east(panel()
															.layout(new GridLayout(1, 2, 0, 0))
															.addAll(addButton, removeButton)))
							.build();
		}

		private void addCover() throws IOException {
			File coverFile = Dialogs.select()
							.files()
							.owner(this)
							.title(BUNDLE.getString("select_image"))
							.filter(IMAGE_FILE_FILTER)
							.selectFile();
			value.set(Files.readAllBytes(coverFile.toPath()));
		}

		private void removeCover() {
			value.clear();
		}

		private void setEmbedded(boolean embedded) {
			configureImagePane(embedded);
			if (embedded) {
				embed();
			}
			else {
				displayInDialog();
			}
		}

		private void embed() {
			Utilities.disposeParentWindow(centerPanel);
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
							.onOpened(windowEvent -> imagePane.reset())
							.size(DIALOG_SIZE)
							.show();
		}

		private void configureImagePane(boolean embedded) {
			imagePane.zoomDevice().set(embedded ? ImagePane.ZoomDevice.NONE : ImagePane.ZoomDevice.MOUSE_WHEEL);
			imagePane.movable().set(!embedded);
			imagePane.navigable().set(!embedded);
		}

		private final class EmbeddingMouseListener extends MouseAdapter {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					embedded.toggle();
				}
			}
		}

		private final class CoverTransferHandler extends FileTransferHandler {

			@Override
			protected boolean importFiles(Component component, List<File> files) {
				try {
					if (singleImage(files)) {
						value.set(Files.readAllBytes(files.get(0).toPath()));

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
}
