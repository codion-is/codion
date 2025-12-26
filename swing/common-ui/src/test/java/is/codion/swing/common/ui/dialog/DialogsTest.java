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

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.user.User;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.icon.SVGIcon;
import is.codion.swing.common.ui.icon.SVGIconsTest;
import is.codion.swing.common.ui.key.KeyEvents;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Collections;

import static is.codion.swing.common.ui.control.Control.command;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DialogsTest {

	private static final SVGIcon ICON = SVGIcon.svgIcon(SVGIconsTest.class.getResource("alert.svg"), 10, Color.BLACK);

	@Test
	void dialog() {
		Dialogs.builder()
						.component(new JLabel())
						.owner(new JLabel())
						.title("title")
						.icon(ICON)
						.modal(false)
						.resizable(false)
						.enterAction(command(() -> {}))
						.onOpened(e -> {})
						.onClosed(e -> {})
						.closeObserver(Event.event())
						.confirmCloseListener(state -> {})
						.disposeOnEscape(false)
						.keyEvent(KeyEvents.builder()
										.keyCode(KeyEvent.VK_ESCAPE)
										.action(Control.command(() -> {})))
						.build();
	}

	@Test
	void progressWorker() {
		Dialogs.progressWorker()
						.task(() -> {})
						.owner(new JLabel())
						.title("title")
						.icon(ICON)
						.northComponent(new JPanel())
						.westComponent(new JPanel())
						.onException("Fail")
						.onResult("Success", "Success")
						.stringPainted(true)
						.indeterminate(false)
						.build();
	}

	@Test
	void exception() {
		Dialogs.exception()
						.owner(new JLabel())
						.title("title")
						.icon(ICON)
						.message("message");
	}

	@Test
	void fileSelection() {
		Dialogs.select()
						.files()
						.owner(new JLabel())
						.title("title")
						.startDirectory(System.getProperty("user.home"))
						.confirmOverwrite(true);
	}

	@Test
	void login() {
		Dialogs.login()
						.owner(new JLabel())
						.title("title")
						.icon(ICON)
						.validator(user -> {})
						.southComponent(new JLabel())
						.defaultUser(User.user("scott"));
	}

	@Test
	void okCancel() {
		JLabel label = new JLabel();
		Runnable runnable = () -> {};
		Dialogs.okCancel()
						.component(label)
						.owner(label)
						.title("title")
						.modal(false)
						.icon(ICON)
						.onOk(runnable)
						.onCancel(runnable)
						.build();

		Control.Command command = () -> {};
		ObservableState state = State.state().observable();

		assertThrows(IllegalStateException.class, () -> Dialogs.okCancel()
						.onOk(runnable)
						.okAction(command(command)));
		assertThrows(IllegalStateException.class, () -> Dialogs.okCancel()
						.onCancel(runnable)
						.cancelAction(command(command)));

		assertThrows(IllegalStateException.class, () -> Dialogs.okCancel()
						.okAction(command(command))
						.onOk(runnable));
		assertThrows(IllegalStateException.class, () -> Dialogs.okCancel()
						.cancelAction(command(command))
						.onCancel(runnable));

		assertThrows(IllegalStateException.class, () -> Dialogs.okCancel()
						.okAction(command(command))
						.okEnabled(state));
		assertThrows(IllegalStateException.class, () -> Dialogs.okCancel()
						.cancelAction(command(command))
						.cancelEnabled(state));
	}

	@Test
	void progress() {
		Dialogs.progress()
						.owner(new JLabel())
						.title("title")
						.indeterminate(true)
						.stringPainted(true)
						.northComponent(new JPanel())
						.westComponent(new JPanel())
						.control(Control.builder().command(() -> {}))
						.build();
	}

	@Test
	void selectionDialog() {
		Dialogs.select()
						.list(Collections.singletonList("hello"))
						.owner(new JLabel())
						.title("title")
						.allowEmptySelection(true)
						.defaultSelection("hello");
		Dialogs.select()
						.comboBox(Collections.singletonList("hello"))
						.owner(new JLabel())
						.title("title")
						.allowEmptySelection(true)
						.defaultSelection("hello");
	}

	@Test
	void selectionDialogNoItems() {
		assertThrows(IllegalArgumentException.class, () -> Dialogs.select().list(Collections.emptyList()));
		assertThrows(IllegalArgumentException.class, () -> Dialogs.select().comboBox(Collections.emptyList()));
	}

	@Test
	void selectionDialogNonExistingDefaultSelection() {
		assertThrows(IllegalArgumentException.class, () -> Dialogs.select().list(Collections.singletonList("helloist"))
						.defaultSelection("hello"));
		assertThrows(IllegalArgumentException.class, () -> Dialogs.select().comboBox(Collections.singletonList("helloist"))
						.defaultSelection("hello"));
	}
}
