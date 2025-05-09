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
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.Event;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.key.KeyEvents;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.event.KeyEvent;
import java.util.Collections;

import static is.codion.swing.common.ui.control.Control.command;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DialogsTest {

	@Test
	void componentDialog() {
		Dialogs.componentDialog(new JLabel())
						.owner(new JLabel())
						.title("title")
						.icon(Logos.logoTransparent())
						.modal(false)
						.resizable(false)
						.enterAction(command(() -> {}))
						.onOpened(e -> {})
						.onClosed(e -> {})
						.closeObserver(Event.event())
						.confirmCloseListener(state -> {})
						.disposeOnEscape(false)
						.keyEvent(KeyEvents.builder(KeyEvent.VK_ESCAPE)
										.action(Control.command(() -> {})))
						.build();
	}

	@Test
	void progressWorkerDialog() {
		Dialogs.progressWorkerDialog(() -> {})
						.owner(new JLabel())
						.title("title")
						.icon(Logos.logoTransparent())
						.northPanel(new JPanel())
						.westPanel(new JPanel())
						.onException("Fail")
						.onResult("Success")
						.stringPainted(true)
						.indeterminate(false)
						.build();
	}

	@Test
	void exceptionDialog() {
		Dialogs.exceptionDialog()
						.owner(new JLabel())
						.title("title")
						.icon(Logos.logoTransparent())
						.message("message");
	}

	@Test
	void fileSelectionDialog() {
		Dialogs.fileSelectionDialog()
						.owner(new JLabel())
						.title("title")
						.startDirectory(System.getProperty("user.home"))
						.confirmOverwrite(true);
	}

	@Test
	void loginDialog() {
		Dialogs.loginDialog()
						.owner(new JLabel())
						.title("title")
						.icon(Logos.logoTransparent())
						.validator(user -> {})
						.southComponent(new JLabel())
						.defaultUser(User.user("scott"));
	}

	@Test
	void okCancelDialog() {
		JLabel label = new JLabel();
		Runnable runnable = () -> {};
		Dialogs.okCancelDialog(label)
						.owner(label)
						.title("title")
						.modal(false)
						.icon(Logos.logoTransparent())
						.onOk(runnable)
						.onCancel(runnable)
						.build();

		Control.Command command = () -> {};
		ObservableState state = State.state().observable();

		assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
						.onOk(runnable)
						.okAction(command(command)));
		assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
						.onCancel(runnable)
						.cancelAction(command(command)));

		assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
						.okAction(command(command))
						.onOk(runnable));
		assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
						.cancelAction(command(command))
						.onCancel(runnable));

		assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
						.okAction(command(command))
						.okEnabled(state));
		assertThrows(IllegalStateException.class, () -> Dialogs.okCancelDialog(label)
						.cancelAction(command(command))
						.cancelEnabled(state));
	}

	@Test
	void progressDialog() {
		Dialogs.progressDialog()
						.owner(new JLabel())
						.title("title")
						.indeterminate(true)
						.stringPainted(true)
						.northPanel(new JPanel())
						.westPanel(new JPanel())
						.control(Control.builder().command(() -> {}))
						.build();
	}

	@Test
	void selectionDialog() {
		Dialogs.listSelectionDialog(Collections.singletonList("hello"))
						.owner(new JLabel())
						.title("title")
						.allowEmptySelection(true)
						.defaultSelection("hello");
		Dialogs.comboBoxSelectionDialog(Collections.singletonList("hello"))
						.owner(new JLabel())
						.title("title")
						.allowEmptySelection(true)
						.defaultSelection("hello");
	}

	@Test
	void selectionDialogNoItems() {
		assertThrows(IllegalArgumentException.class, () -> Dialogs.listSelectionDialog(Collections.emptyList()));
		assertThrows(IllegalArgumentException.class, () -> Dialogs.comboBoxSelectionDialog(Collections.emptyList()));
	}

	@Test
	void selectionDialogNonExistingDefaultSelection() {
		assertThrows(IllegalArgumentException.class, () -> Dialogs.listSelectionDialog(Collections.singletonList("helloist"))
						.defaultSelection("hello"));
		assertThrows(IllegalArgumentException.class, () -> Dialogs.comboBoxSelectionDialog(Collections.singletonList("helloist"))
						.defaultSelection("hello"));
	}
}
