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

import is.codion.common.reactive.state.State;
import is.codion.demos.chinook.ui.CoverArtPanelBuilder.CoverArtPanel;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.image.ImagePane;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;
import is.codion.swing.common.ui.transfer.FileTransferHandler;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.common.reactive.state.State.present;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.image.ImagePane.ZoomDevice.MOUSE_WHEEL;
import static is.codion.swing.common.ui.component.image.ImagePane.ZoomDevice.NONE;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEtchedBorder;

final class CoverArtPanelBuilder extends AbstractComponentValueBuilder<CoverArtPanel, byte[], CoverArtPanelBuilder> {

	private static final ResourceBundle BUNDLE = getBundle(CoverArtPanelBuilder.class.getName());

	@Override
	protected CoverArtPanel createComponent() {
		return new CoverArtPanel();
	}

	@Override
	protected ComponentValue<CoverArtPanel, byte[]> createValue(CoverArtPanel component) {
		return new CoverArtValue(component);
	}

	@Override
	protected void enable(TransferFocusOnEnter transferFocusOnEnter, CoverArtPanel component) {
		transferFocusOnEnter.enable(component.addButton, component.removeButton);
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
										.smallIcon(ICONS.get("plus").small()))
						.build();
		private final JButton removeButton = button()
						.control(Control.builder()
										.command(this::removeCover)
										.smallIcon(ICONS.get("minus").small()))
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
			Ancestor.window().of(centerPanel).dispose();
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
			imagePane.zoomDevice().set(embedded ? NONE : MOUSE_WHEEL);
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
					throw new UncheckedIOException(e);
				}
			}

			private boolean singleImage(List<File> files) {
				return files.size() == 1 && IMAGE_FILE_FILTER.accept(files.get(0));
			}
		}
	}

	private static final class CoverArtValue extends AbstractComponentValue<CoverArtPanel, byte[]> {

		private CoverArtValue(CoverArtPanel component) {
			super(component);
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
	}
}
