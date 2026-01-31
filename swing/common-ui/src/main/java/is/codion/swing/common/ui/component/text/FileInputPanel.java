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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;

import org.jspecify.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * For instances use the {@link #builder()} method.
 * @see #builder()
 */
public final class FileInputPanel extends JPanel {

	private static final DefaultBuilderFactory BUILDER_FACTORY = new DefaultBuilderFactory();

	private final JTextField filePathField;
	private final JButton browseButton;

	private FileInputPanel(AbstractBuilder<?> builder) {
		CommandControl browseControl = Control.builder()
						.command(this::browseFile)
						.caption(builder.buttonIcon == null ? builder.buttonCaption : null)
						.smallIcon(builder.buttonIcon)
						.build();
		filePathField = builder.pathFieldBuilder
						.keyEvent(KeyEvents.builder()
										.keyCode(KeyEvent.VK_INSERT)
										.action(browseControl))
						.build();
		browseButton = ButtonBuilder.builder()
						.control(browseControl)
						.preferredSize(new Dimension(filePathField.getPreferredSize().height, filePathField.getPreferredSize().height))
						.build();
		setLayout(new BorderLayout());
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
		return BUILDER_FACTORY;
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
	public interface Builder<T> extends ComponentValueBuilder<FileInputPanel, T, Builder<T>> {

		/**
		 * Note that the button caption is not used if a {@link #buttonIcon(Icon)} is specified
		 * @param buttonCaption the browse button caption, used in case no icon is specified
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
		Builder<T> filePathField(Consumer<TextFieldBuilder<JTextField, String, ?>> filePathField);
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

	private abstract static class AbstractBuilder<T> extends AbstractComponentValueBuilder<FileInputPanel, T, Builder<T>> implements Builder<T> {

		private final TextFieldBuilder<JTextField, String, ?> pathFieldBuilder = TextFieldBuilder.builder()
						.valueClass(String.class)
						.editable(false);

		private String buttonCaption = "...";
		private @Nullable Icon buttonIcon;

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
		public final Builder<T> filePathField(Consumer<TextFieldBuilder<JTextField, String, ?>> filePathField) {
			requireNonNull(filePathField).accept(pathFieldBuilder);
			return this;
		}

		@Override
		protected final void enable(TransferFocusOnEnter transferFocusOnEnter, FileInputPanel component) {
			transferFocusOnEnter.enable(component.filePathField, component.browseButton);
		}
	}

	private static final class PathInputPanelBuilder extends AbstractBuilder<Path> implements Builder<Path> {

		@Override
		protected FileInputPanel createComponent() {
			return new FileInputPanel(this);
		}

		@Override
		protected ComponentValue<FileInputPanel, Path> createValue(FileInputPanel component) {
			return new PathInputPanelValue(component);
		}
	}

	private static final class ByteArrayInputPanelBuilder extends AbstractBuilder<byte[]> implements Builder<byte[]> {

		@Override
		protected FileInputPanel createComponent() {
			return new FileInputPanel(this);
		}

		@Override
		protected ComponentValue<FileInputPanel, byte[]> createValue(FileInputPanel component) {
			return new ByteArrayInputPanelValue(component);
		}
	}

	private static final class PathInputPanelValue extends AbstractComponentValue<FileInputPanel, Path> {

		private PathInputPanelValue(FileInputPanel fileInputPanel) {
			super(fileInputPanel);
			fileInputPanel.filePathField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyObserver());
		}

		@Override
		protected @Nullable Path getComponentValue() {
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

	private static final class ByteArrayInputPanelValue extends AbstractComponentValue<FileInputPanel, byte[]> {

		private ByteArrayInputPanelValue(FileInputPanel fileInputPanel) {
			super(fileInputPanel);
			fileInputPanel.filePathField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyObserver());
		}

		@Override
		protected byte @Nullable [] getComponentValue() {
			String filePath = component().filePathField.getText();
			if (filePath.isEmpty()) {
				return null;
			}
			try {
				return Files.readAllBytes(Paths.get(filePath));
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		protected void setComponentValue(byte[] value) {}
	}
}
