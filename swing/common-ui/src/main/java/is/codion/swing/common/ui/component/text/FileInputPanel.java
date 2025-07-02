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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;

/**
 * For instances use the {@link #builder()} method.
 * @see #builder()
 */
public final class FileInputPanel extends JPanel {

	private final JTextField filePathField;
	private final JButton browseButton;

	private FileInputPanel(AbstractBuilder<?> builder) {
		CommandControl browseControl = Control.builder()
						.command(this::browseFile)
						.caption(builder.buttonCaption)
						.smallIcon(builder.buttonIcon)
						.build();
		filePathField = builder.pathFieldBuilder
						.keyEvent(KeyEvents.builder(KeyEvent.VK_INSERT)
										.action(browseControl))
						.build();
		browseButton = ButtonBuilder.builder(browseControl)
						.preferredSize(new Dimension(filePathField.getPreferredSize().height, filePathField.getPreferredSize().height))
						.build();
		setLayout(borderLayout());
		add(filePathField, BorderLayout.CENTER);
		add(browseButton, BorderLayout.EAST);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		filePathField.setEnabled(enabled);
		browseButton.setEnabled(enabled);
	}

	@Override
	public void setToolTipText(String text) {
		filePathField.setToolTipText(text);
	}

	/**
	 * @return a new {@link FileInputPanel.BuilderFactory} instance.
	 */
	public static FileInputPanel.BuilderFactory builder() {
		return new DefaultBuilderFactory();
	}

	private void browseFile() {
		filePathField.setText(Dialogs.select()
						.files()
						.owner(filePathField)
						.title("Select file")
						.selectFile().toString());
	}

	/**
	 * Provides either a {@link Path} or a byte array based {@link FileInputPanel.Builder}.
	 */
	public interface BuilderFactory {

		/**
		 * Provides builder for a {@link Path} based file input panel.
		 * @return a new builder
		 */
		Builder<Path> path();

		/**
		 * Provides builder for a byte[] based file input panel.
		 * @return a new builder
		 */
		Builder<byte[]> byteArray();
	}

	/**
	 * Builds a {@link FileInputPanel}
	 */
	public interface Builder<T> extends ComponentBuilder<T, FileInputPanel, Builder<T>> {

		/**
		 * @param buttonCaption the browse button caption
		 * @return this builder instance
		 */
		Builder<T> buttonCaption(String buttonCaption);

		/**
		 * @param buttonIcon the browse button icon
		 * @return this builder instance
		 */
		Builder<T> buttonIcon(Icon buttonIcon);

		/**
		 * The field has already been rendered non-editable, use {@link TextFieldBuilder#editable(boolean)} to revert.
		 * @param filePathField the file path field builder
		 * @return this builder instance
		 */
		Builder<T> filePathField(Consumer<TextFieldBuilder<String, JTextField, ?>> filePathField);
	}

	private static final class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public Builder<Path> path() {
			return new PathInputPanelBuilder();
		}

		@Override
		public Builder<byte[]> byteArray() {
			return new ByteArrayInputPanelBuilder();
		}
	}

	private abstract static class AbstractBuilder<T> extends AbstractComponentBuilder<T, FileInputPanel, Builder<T>> implements Builder<T> {

		private final TextFieldBuilder<String, JTextField, ?> pathFieldBuilder = TextFieldBuilder.builder(String.class)
						.editable(false);

		private String buttonCaption = "...";
		private Icon buttonIcon;

		@Override
		public final Builder<T> buttonCaption(String buttonCaption) {
			this.buttonCaption = requireNonNull(buttonCaption);
			return this;
		}

		@Override
		public Builder<T> buttonIcon(Icon buttonIcon) {
			this.buttonIcon = requireNonNull(buttonIcon);
			return this;
		}

		@Override
		public final Builder<T> filePathField(Consumer<TextFieldBuilder<String, JTextField, ?>> filePathField) {
			requireNonNull(filePathField).accept(pathFieldBuilder);
			return this;
		}

		@Override
		protected final void enableTransferFocusOnEnter(FileInputPanel component, TransferFocusOnEnter transferFocusOnEnter) {
			transferFocusOnEnter.enable(component.filePathField, component.browseButton);
		}
	}

	private static final class PathInputPanelBuilder extends AbstractBuilder<Path> implements Builder<Path> {

		@Override
		protected FileInputPanel createComponent() {
			return new FileInputPanel(this);
		}

		@Override
		protected ComponentValue<Path, FileInputPanel> createComponentValue(FileInputPanel component) {
			return new PathInputPanelValue(component);
		}
	}

	private static final class ByteArrayInputPanelBuilder extends AbstractBuilder<byte[]> implements Builder<byte[]> {

		@Override
		protected FileInputPanel createComponent() {
			return new FileInputPanel(this);
		}

		@Override
		protected ComponentValue<byte[], FileInputPanel> createComponentValue(FileInputPanel component) {
			return new ByteArrayInputPanelValue(component);
		}
	}

	private static final class PathInputPanelValue extends AbstractComponentValue<Path, FileInputPanel> {

		private PathInputPanelValue(FileInputPanel fileInputPanel) {
			super(fileInputPanel);
			fileInputPanel.filePathField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyListeners());
		}

		@Override
		protected Path getComponentValue() {
			String filePath = component().filePathField.getText();
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
			fileInputPanel.filePathField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyListeners());
		}

		@Override
		protected byte[] getComponentValue() {
			String filePath = component().filePathField.getText();
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
		protected void setComponentValue(byte[] value) {}
	}
}
