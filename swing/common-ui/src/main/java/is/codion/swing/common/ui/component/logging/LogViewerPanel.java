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
package is.codion.swing.common.ui.component.logging;

import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;
import is.codion.swing.common.ui.component.text.SearchHighlighter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.Components.menu;
import static is.codion.swing.common.ui.component.Components.textArea;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * A simple log viewer panel with search functionality.
 * @see #logViewerPanel(Document)
 * @see #logViewerPanel(Document, Supplier)
 */
public final class LogViewerPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(CalendarPanel.class, getBundle(LogViewerPanel.class.getName()));

	private final JTextArea logTextArea;
	private final Supplier<String> filename;
	private final JTextField searchField;

	private LogViewerPanel(Document logDocument, Supplier<String> filename) {
		super(borderLayout());
		this.logTextArea = createLogTextArea(logDocument);
		this.filename = filename;
		this.searchField = SearchHighlighter.builder()
						.component(logTextArea)
						.build()
						.createSearchField();
		KeyEvents.builder()
						.keyCode(KeyEvent.VK_F)
						.modifiers(InputEvent.CTRL_DOWN_MASK)
						.action(command(searchField::requestFocusInWindow))
						.enable(logTextArea);
		add(new JScrollPane(logTextArea), BorderLayout.CENTER);
		add(searchField, BorderLayout.SOUTH);
	}

	/**
	 * @return the log document
	 */
	public Document document() {
		return logTextArea.getDocument();
	}

	private JTextArea createLogTextArea(Document logDocument) {
		JTextArea textArea = textArea()
						.document(logDocument)
						.editable(false)
						.wrapStyleWord(true)
						.caretUpdatePolicy(DefaultCaret.NEVER_UPDATE)
						.build();
		Font font = textArea.getFont();
		textArea.setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));
		State lineWrapState = State.builder()
						.consumer(textArea::setLineWrap)
						.build();
		textArea.setComponentPopupMenu(menu()
						.controls(Controls.builder()
										.control(Control.builder()
														.command(this::saveLogToFile)
														.caption(MESSAGES.getString("save_to_file") + "..."))
										.separator()
										.control(Control.builder()
														.toggle(lineWrapState)
														.caption(MESSAGES.getString("line_wrap"))))
						.buildPopupMenu());

		return textArea;
	}

	private void saveLogToFile() throws IOException {
		Files.write(Dialogs.select()
						.files()
						.owner(this)
						.selectFileToSave(filename.get()).toPath(), logTextArea.getText().getBytes());
	}

	/**
	 * @param logDocument the log document
	 * @return a new {@link LogViewerPanel}
	 */
	public static LogViewerPanel logViewerPanel(Document logDocument) {
		return logViewerPanel(logDocument, () -> "log.txt");
	}

	/**
	 * @param logDocument the log document
	 * @param filename provides the default filename when saving the log to file
	 * @return a new {@link LogViewerPanel}
	 */
	public static LogViewerPanel logViewerPanel(Document logDocument, Supplier<String> filename) {
		return new LogViewerPanel(requireNonNull(logDocument), requireNonNull(filename));
	}
}
