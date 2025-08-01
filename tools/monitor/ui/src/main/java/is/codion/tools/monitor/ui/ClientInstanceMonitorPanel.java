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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.monitor.ui;

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.swing.common.ui.component.text.SearchHighlighter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.tools.monitor.model.ClientInstanceMonitor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = LocaleDateTimePattern.builder()
					.delimiterDash().yearFourDigits().hoursMinutesSeconds()
					.build().createFormatter();
	private static final DateTimeFormatter DATE_TIME_FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

	private final ClientInstanceMonitor model;
	private final JTextArea logTextArea;
	private final SearchHighlighter searchHighlighter;
	private final JTextField searchField;

	private final JTextField creationDateField = stringField()
					.editable(false)
					.build();

	/**
	 * Instantiates a new ClientInstanceMonitorPanel
	 * @param model the model
	 * @throws RemoteException in case of an exception
	 */
	public ClientInstanceMonitorPanel(ClientInstanceMonitor model) throws RemoteException {
		this.model = requireNonNull(model);
		this.logTextArea = createLogTextArea();
		this.searchHighlighter = SearchHighlighter.builder()
						.component(logTextArea)
						.build();
		this.searchField = searchHighlighter.createSearchField();
		KeyEvents.builder()
						.keyCode(KeyEvent.VK_F)
						.modifiers(InputEvent.CTRL_DOWN_MASK)
						.action(command(searchField::requestFocusInWindow))
						.enable(logTextArea);
		initializeUI();
		updateView();
	}

	public void updateView() throws RemoteException {
		creationDateField.setText(DATE_TIME_FORMATTER.format(model.remoteClient().creationTime()));
		model.refreshLog();
	}

	private void initializeUI() {
		JPanel creationDatePanel = flowLayoutPanel(FlowLayout.LEFT)
						.add(new JLabel("Creation time"))
						.add(creationDateField)
						.build();

		JPanel settingsPanel = flowLayoutPanel(FlowLayout.LEFT)
						.add(checkBox()
										.link(model.loggingEnabled())
										.text("Logging enabled")
										.build())
						.add(button()
										.control(command(this::updateView))
										.text("Refresh log")
										.build())
						.build();

		JPanel northPanel = borderLayoutPanel()
						.border(createTitledBorder("Connection info"))
						.centerComponent(creationDatePanel)
						.eastComponent(settingsPanel)
						.build();

		JPanel textLogPanel = borderLayoutPanel()
						.centerComponent(new JScrollPane(logTextArea))
						.southComponent(searchField)
						.build();

		JTabbedPane centerPane = tabbedPane()
						.tab("Text", textLogPanel)
						.tab("Tree", new JScrollPane(createLogTree()))
						.build();

		setLayout(borderLayout());
		add(northPanel, BorderLayout.NORTH);
		add(centerPane, BorderLayout.CENTER);
	}

	private JTree createLogTree() {
		JTree treeLog = new JTree(model.logTreeModel());
		treeLog.setRootVisible(false);

		return treeLog;
	}

	private JTextArea createLogTextArea() {
		JTextArea textArea = textArea()
						.document(model.logDocument())
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
														.command(() -> saveLogToFile(textArea))
														.caption("Save to file..."))
										.separator()
										.control(Control.builder()
														.toggle(lineWrapState)
														.caption("Line wrap")))
						.buildPopupMenu());

		return textArea;
	}

	private void saveLogToFile(JTextArea logArea) throws IOException {
		if (creationDateField.getText().isEmpty()) {
			throw new IllegalStateException("No client selected");
		}

		User user = model.remoteClient().user();
		LocalDateTime creationDate = LocalDateTime.parse(creationDateField.getText(), DATE_TIME_FORMATTER);
		String filename = user.username() + "@" + DATE_TIME_FILENAME_FORMATTER.format(creationDate) + ".log";

		Files.write(Dialogs.select()
						.files()
						.owner(this)
						.selectFileToSave(filename).toPath(), logArea.getText().getBytes());
	}
}
