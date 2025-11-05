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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.state.State;
import is.codion.common.utilities.property.PropertyStore;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.label.LabelBuilder;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.scrollpane.ScrollPaneBuilder;
import is.codion.swing.common.ui.component.text.TextAreaBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.ResourceBundle.getBundle;

/**
 * A Panel for displaying exception information.
 */
final class ExceptionPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(ExceptionPanel.class, getBundle(ExceptionPanel.class.getName()));

	private static final int MESSAGE_AREA_WIDTH = 500;
	private static final int SCROLL_PANE_WIDTH = 500;
	private static final int SCROLL_PANE_HEIGHT = 200;
	private static final int TAB_SIZE = 4;

	private final State showDetailState = State.builder()
					.consumer(this::showDetails)
					.build();
	private final JCheckBox detailsCheckBox = CheckBoxBuilder.builder()
					.link(showDetailState)
					.text(MESSAGES.getString("details"))
					.toolTipText(MESSAGES.getString("show_details"))
					.build();
	private final JTextArea errorMessageArea = TextAreaBuilder.builder()
					.rowsColumns(3, 20)
					.editable(false)
					.lineWrap(true)
					.wrapStyleWord(true)
					.build();
	private final JTextArea stackTraceArea = TextAreaBuilder.builder()
					.editable(false)
					.tabSize(TAB_SIZE)
					.build();
	private final JScrollPane stackTraceScrollPane = ScrollPaneBuilder.builder()
					.view(stackTraceArea)
					.visible(false)
					.preferredSize(new Dimension(SCROLL_PANE_WIDTH, SCROLL_PANE_HEIGHT))
					.build();
	private final JButton printButton = ButtonBuilder.builder()
					.control(Control.builder()
									.command(stackTraceArea::print)
									.caption(Messages.print())
									.description(MESSAGES.getString("print_error_report"))
									.mnemonic(MESSAGES.getString("print_error_report_mnemonic").charAt(0)))
					.visible(false)
					.build();
	private final JButton saveButton = ButtonBuilder.builder()
					.control(Control.builder()
									.command(this::saveDetails)
									.caption(MESSAGES.getString("save"))
									.description(MESSAGES.getString("save_error_log"))
									.mnemonic(MESSAGES.getString("save_mnemonic").charAt(0)))
					.visible(false)
					.build();
	private final JButton closeButton = ButtonBuilder.builder()
					.control(Control.builder()
									.command(this::closeDialog)
									.caption(MESSAGES.getString("close"))
									.description(MESSAGES.getString("close_dialog")))
					.build();
	private final JButton copyButton = ButtonBuilder.builder()
					.control(Control.builder()
									.command(() -> Utilities.setClipboard(stackTraceArea.getText()))
									.caption(Messages.copy())
									.description(MESSAGES.getString("copy_to_clipboard"))
									.mnemonic(MESSAGES.getString("copy_mnemonic").charAt(0)))
					.visible(false)
					.build();

	ExceptionPanel(Throwable throwable, String message, boolean systemProperties) {
		setThrowable(throwable, message == null ? throwable.getClass().getSimpleName() : message, systemProperties);
		setLayout(borderLayout());
		add(createMainPanel(), BorderLayout.CENTER);
		bindEvents();
	}

	JButton closeButton() {
		return closeButton;
	}

	JCheckBox detailsCheckBox() {
		return detailsCheckBox;
	}

	private JPanel createMainPanel() {
		return BorderLayoutPanelBuilder.builder()
						.border(emptyBorder())
						.north(BorderLayoutPanelBuilder.builder()
										.west(LabelBuilder.builder()
														.icon(UIManager.getIcon("OptionPane.errorIcon")))
										.center(ScrollPaneBuilder.builder()
														.view(errorMessageArea)
														.preferredWidth(MESSAGE_AREA_WIDTH)))
						.center(stackTraceScrollPane)
						.south(createButtonPanel())
						.build();
	}

	private JPanel createButtonPanel() {
		return BorderLayoutPanelBuilder.builder()
						.west(PanelBuilder.builder()
										.flowLayout(FlowLayout.LEFT)
										.add(detailsCheckBox))
						.center(PanelBuilder.builder()
										.flowLayout(FlowLayout.RIGHT)
										.addAll(copyButton, printButton, saveButton, closeButton))
						.build();
	}

	private void bindEvents() {
		KeyEvents.builder()
						.keyCode(VK_ESCAPE)
						.condition(WHEN_IN_FOCUSED_WINDOW)
						.action(command(this::closeDialog))
						.enable(this);
	}

	private void showDetails(boolean showDetails) {
		copyButton.setVisible(showDetails);
		printButton.setVisible(showDetails);
		saveButton.setVisible(showDetails);
		stackTraceScrollPane.setVisible(showDetails);
		parentDialog().ifPresent(dialog -> {
			dialog.pack();
			dialog.setLocationRelativeTo(dialog.getOwner());
		});
	}

	private void closeDialog() {
		parentDialog().ifPresent(JDialog::dispose);
	}

	private Optional<JDialog> parentDialog() {
		return Optional.ofNullable(Utilities.parentDialog(this));
	}

	private void saveDetails() throws IOException {
		Files.write(new DefaultFileSelectionDialogBuilder()
										.owner(stackTraceArea)
										.selectFileToSave("error.txt")
										.toPath(),
						Arrays.asList(stackTraceArea.getText().split("\\r?\\n")));
	}

	private void setThrowable(Throwable exception, String message, boolean systemProperties) {
		errorMessageArea.setText(message);
		errorMessageArea.setCaretPosition(0);
		stackTraceArea.setText(stackTraceAndProperties(exception, systemProperties));
		stackTraceArea.setCaretPosition(0);
	}

	private static String stackTraceAndProperties(Throwable exception, boolean systemProperties) {
		StringWriter stringWriter = new StringWriter();
		exception.printStackTrace(new PrintWriter(stringWriter));
		StringBuilder builder = new StringBuilder(stringWriter.toString());
		if (systemProperties) {
			builder.append("\n");
			builder.append("--------------------------------------------Properties--------------------------------------------\n\n");
			builder.append(PropertyStore.systemProperties());
		}

		return builder.toString();
	}
}