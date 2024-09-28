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
import java.io.File;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;

/**
 * For instances use the {@link #fileInputPanel(JTextField)} factory method.
 * @see #fileInputPanel(JTextField)
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
	 * @param filePathField the file path input field
	 * @return a new {@link FileInputPanel.Builder} instance.
	 */
	public static FileInputPanel.Builder builder(JTextField filePathField) {
		return new FileInputPanel.DefaultFileInputPanelBuilder(filePathField);
	}

	private void browseFile() {
		try {
			File file = Dialogs.fileSelectionDialog()
							.owner(filePathField)
							.title("Select file")
							.selectFile();
			filePathField.setText(file.toString());
		}
		catch (CancelException e) {
			filePathField.setText("");
			throw e;
		}
	}

	/**
	 * Builds a {@link FileInputPanel}
	 */
	public interface Builder extends ComponentBuilder<Path, FileInputPanel, Builder> {}

	private static final class DefaultFileInputPanelBuilder extends AbstractComponentBuilder<Path, FileInputPanel, Builder> implements Builder {

		private final JTextField filePathField;

		private DefaultFileInputPanelBuilder(JTextField filePathField) {
			this.filePathField = requireNonNull(filePathField);
		}

		@Override
		protected FileInputPanel createComponent() {
			return fileInputPanel(filePathField);
		}

		@Override
		protected ComponentValue<Path, FileInputPanel> createComponentValue(FileInputPanel component) {
			return new FileInputPanelValue(component);
		}

		@Override
		protected void setInitialValue(FileInputPanel component, Path initialValue) {}
	}

	private static final class FileInputPanelValue extends AbstractComponentValue<Path, FileInputPanel> {

		private FileInputPanelValue(FileInputPanel fileInputPanel) {
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
}
