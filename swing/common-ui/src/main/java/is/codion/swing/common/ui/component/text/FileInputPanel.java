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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.model.CancelException;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;

/**
 * For instances use the {@link #fileInputPanel(JTextField)} factory method or the {@link #builder()} method.
 * @see #fileInputPanel(JTextField)
 * @see #builder()
 */
public final class FileInputPanel extends JPanel {

	private final JTextField filePathField;

	private FileInputPanel(JTextField filePathField) {
		this.filePathField = requireNonNull(filePathField);
		setLayout(Layouts.borderLayout());
		add(filePathField, BorderLayout.CENTER);
		JButton browseButton = new JButton(new AbstractAction("...") {
			@Override
			public void actionPerformed(ActionEvent e) {
				browseFile();
			}
		});
		browseButton.setPreferredSize(new Dimension(filePathField.getPreferredSize().height, filePathField.getPreferredSize().height));
		add(browseButton, BorderLayout.EAST);
	}

	public JTextField filePathField() {
		return filePathField;
	}

	/**
	 * @param filePathField the file path input field
	 * @return a new {@link FileInputPanel} instance.
	 */
	public static FileInputPanel fileInputPanel(JTextField filePathField) {
		return new FileInputPanel(filePathField);
	}

	/**
	 * @return a new {@link FileInputPanel.BuilderFactory} instance.
	 */
	public static FileInputPanel.BuilderFactory builder() {
		return new DefaultBuilderFactory();
	}

	private void browseFile() {
		try {
			filePathField.setText(Dialogs.fileSelectionDialog()
							.owner(filePathField)
							.title("Select file")
							.selectFile().toString());
		}
		catch (CancelException e) {
			filePathField.setText("");
			throw e;
		}
	}

	/**
	 * Provides either a {@link Path} or a byte array based {@link FileInputPanel.Builder}.
	 */
	public interface BuilderFactory {

		/**
		 * Provides builder for a {@link Path} based file input panel.
		 * @param filePathField the field providing the file path
		 * @return a new builder
		 */
		ComponentBuilder<Path, FileInputPanel, Builder<Path>> path(JTextField filePathField);

		/**
		 * Provides builder for a byte[] based file input panel.
		 * @param filePathField the field providing the file path
		 * @return a new builder
		 */
		ComponentBuilder<byte[], FileInputPanel, Builder<byte[]>> byteArray(JTextField filePathField);
	}

	/**
	 * Builds a {@link FileInputPanel}
	 */
	public interface Builder<T> extends ComponentBuilder<T, FileInputPanel, Builder<T>> {}

	private static final class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public ComponentBuilder<Path, FileInputPanel, Builder<Path>> path(JTextField filePathField) {
			return new PathInputPanelBuilder(filePathField);
		}

		@Override
		public ComponentBuilder<byte[], FileInputPanel, Builder<byte[]>> byteArray(JTextField filePathField) {
			return new ByteArrayInputPanelBuilder(filePathField);
		}
	}

	private static final class PathInputPanelBuilder extends AbstractComponentBuilder<Path, FileInputPanel, Builder<Path>> implements Builder<Path> {

		private final JTextField filePathField;

		private PathInputPanelBuilder(JTextField filePathField) {
			this.filePathField = requireNonNull(filePathField);
		}

		@Override
		protected FileInputPanel createComponent() {
			return fileInputPanel(filePathField);
		}

		@Override
		protected ComponentValue<Path, FileInputPanel> createComponentValue(FileInputPanel component) {
			return new PathInputPanelValue(component);
		}
	}

	private static final class ByteArrayInputPanelBuilder extends AbstractComponentBuilder<byte[], FileInputPanel, Builder<byte[]>> implements Builder<byte[]> {

		private final JTextField filePathField;

		private ByteArrayInputPanelBuilder(JTextField filePathField) {
			this.filePathField = requireNonNull(filePathField);
		}

		@Override
		protected FileInputPanel createComponent() {
			return fileInputPanel(filePathField);
		}

		@Override
		protected ComponentValue<byte[], FileInputPanel> createComponentValue(FileInputPanel component) {
			return new ByteArrayInputPanelValue(component);
		}
	}

	private static final class PathInputPanelValue extends AbstractComponentValue<Path, FileInputPanel> {

		private PathInputPanelValue(FileInputPanel fileInputPanel) {
			super(fileInputPanel);
			fileInputPanel.filePathField().getDocument().addDocumentListener((DocumentAdapter) e -> notifyListeners());
		}

		@Override
		protected Path getComponentValue() {
			String filePath = component().filePathField().getText();
			if (filePath.isEmpty()) {
				return null;
			}

			return Path.of(filePath);
		}

		@Override
		protected void setComponentValue(Path path) {
			component().filePathField.setText(path == null ? "" : path.toString());
		}
	}

	private static final class ByteArrayInputPanelValue extends AbstractComponentValue<byte[], FileInputPanel> {

		private ByteArrayInputPanelValue(FileInputPanel fileInputPanel) {
			super(fileInputPanel);
			fileInputPanel.filePathField().getDocument().addDocumentListener((DocumentAdapter) e -> notifyListeners());
		}

		@Override
		protected byte[] getComponentValue() {
			String filePath = component().filePathField().getText();
			if (filePath.isEmpty()) {
				return null;
			}
			try {
				return Files.readAllBytes(Paths.get(filePath));
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		protected void setComponentValue(byte[] value) {
			throw new UnsupportedOperationException();
		}
	}
}
