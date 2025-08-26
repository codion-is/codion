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

import is.codion.common.i18n.Messages;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.text.DocumentAdapter;
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
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Supplier;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

/**
 * A simple log viewer with search functionality.
 * @see #logViewer()
 * @see #logViewer(Supplier)
 */
public final class LogViewer extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(CalendarPanel.class, getBundle(LogViewer.class.getName()));

	private final JTextArea logArea;
	private final Supplier<String> filename;
	private final JTextField searchField;

	private LogViewer(Supplier<String> filename) {
		super(borderLayout());
		this.logArea = createLogArea();
		this.filename = filename;
		this.searchField = SearchHighlighter.builder()
						.component(logArea)
						.build()
						.createSearchField();
		KeyEvents.builder()
						.keyCode(KeyEvent.VK_F)
						.modifiers(InputEvent.CTRL_DOWN_MASK)
						.action(command(searchField::requestFocusInWindow))
						.enable(logArea);
		logArea.getDocument().addDocumentListener((DocumentAdapter) e ->
						logArea.setCaretPosition(e.getDocument().getLength()));
		add(new JScrollPane(logArea), BorderLayout.CENTER);
		add(borderLayoutPanel()
						.center(searchField)
						.east(button()
										.control(Control.builder()
														.command(() -> logArea.setText(""))
														.caption(Messages.clear())
														.mnemonic(Messages.clearMnemonic())))
						.build(), BorderLayout.SOUTH);
	}

	/**
	 * Clears the log
	 */
	public void clear() {
		logArea.setText("");
	}

	/**
	 * @param string the string to append
	 */
	public void append(String string) {
		try {
			document().insertString(document().getLength(), string, null);
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	private JTextArea createLogArea() {
		JTextArea textArea = textArea()
						.document(new DefaultStyledDocument())
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
														.command(this::saveToFile)
														.caption(MESSAGES.getString("save_to_file") + "..."))
										.separator()
										.control(Control.builder()
														.toggle(lineWrapState)
														.caption(MESSAGES.getString("line_wrap"))))
						.buildPopupMenu());

		return textArea;
	}

	private void saveToFile() throws IOException {
		Files.write(Dialogs.select()
						.files()
						.owner(this)
						.selectFileToSave(filename.get()).toPath(), logArea.getText().getBytes());
	}

	private Document document() {
		return logArea.getDocument();
	}

	/**
	 * @return a new {@link LogViewer}
	 */
	public static LogViewer logViewer() {
		return logViewer(() -> "log.txt");
	}

	/**
	 * @param defaultSaveLogFilename provides the default filename when saving the log to file
	 * @return a new {@link LogViewer}
	 */
	public static LogViewer logViewer(Supplier<String> defaultSaveLogFilename) {
		return new LogViewer(requireNonNull(defaultSaveLogFilename));
	}
}
