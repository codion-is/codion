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
package is.codion.manual.swing.common.ui;

import is.codion.common.event.Event;
import is.codion.common.i18n.Messages;
import is.codion.common.state.State;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.JButton;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Window;
import java.io.File;
import java.util.Collection;
import java.util.List;

final class DialogsDemo {

	private void dialog(Window window) {
		// tag::dialog[]
		Event<?> close = Event.event();

		JButton closeButton = Components.button()
						.control(Control.builder()
										.command(close::run)
										.caption("Close"))
						.build();

		Dialogs.builder()
						.component(closeButton)
						.owner(window)
						.title("Dialog")
						.disposeOnEscape(false)
						.closeObserver(close)
						.show();
		// end::dialog[]
	}

	private void selectItems(Window window) {
		// tag::comboBox[]
		Dialogs.select()
						.comboBox(List.of("One", "Two", "Three"))
						.owner(window)
						.title("Select a number")
						.select()
						.ifPresent(System.out::println);
		// end::comboBox[]

		// tag::listSingle[]
		Dialogs.select()
						.list(List.of("One", "Two", "Three"))
						.owner(window)
						.title("Select a number")
						.select()
						.single()
						.ifPresent(System.out::println);
		// end::listSingle[]

		// tag::listMultiple[]
		Collection<String> selected = Dialogs.select()
						.list(List.of("One", "Two", "Three", "Four"))
						.owner(window)
						.title("Select numbers")
						.select()
						.multiple();
		// end::listMultiple[]
	}

	private void selectFiles(Window window) {
		// tag::file[]
		File file = Dialogs.select()
						.files()
						.owner(window)
						.title("Select a file")
						.selectFile();
		// end::file[]

		// tag::files[]
		Collection<File> files = Dialogs.select()
						.files()
						.owner(window)
						.title("Select files")
						.filter(new FileNameExtensionFilter("PDF files", "pdf"))
						.selectFiles();
		// end::files[]

		// tag::fileToSave[]
		File fileToSave = Dialogs.select()
						.files()
						.owner(window)
						.title("Select file to save")
						.confirmOverwrite(false)
						.selectFileToSave("default-filename.txt");
		// end::fileToSave[]

		// tag::directory[]
		File directory = Dialogs.select()
						.files()
						.owner(window)
						.title("Select a directory")
						.selectDirectory();
		// end::directory[]

		// tag::directories[]
		Collection<File> directories = Dialogs.select()
						.files()
						.owner(window)
						.title("Select directories")
						.selectDirectories();
		// end::directories[]

		// tag::fileOrDirectory[]
		File fileOrDirectory = Dialogs.select()
						.files()
						.owner(window)
						.title("Select file or directory")
						.selectFileOrDirectory();
		// end::fileOrDirectory[]

		// tag::filesOrDirectories[]
		Collection<File> filesOrDirectories = Dialogs.select()
						.files()
						.owner(window)
						.title("Select files and/or directories")
						.selectFilesOrDirectories();
		// end::filesOrDirectories[]
	}

	private void okCancel(Window window) {
		// tag::okCancel[]
		Dialogs.okCancel()
						.owner(window)
						.title("Title")
						.onOk(this::onOk)
						.onCancel(this::onCancel)
						.show();
		// end::okCancel[]
	}

	private void action(Window window) {
		// tag::action[]
		Dialogs.action()
						.owner(window)
						.title("Title")
						.defaultAction(Control.builder()
										.command(this::onOk)
										.caption(Messages.ok())
										.build())
						.escapeAction(Control.builder()
										.command(this::onCancel)
										.caption(Messages.cancel())
										.build())
						.show();
		// end::action[]
	}

	private void input(Window window) {
		// tag::input[]
		ComponentValue<NumberField<Integer>, Integer> component =
						Components.integerField()
										.value(42)
										.buildValue();

		Integer input = Dialogs.input()
						.component(component)
						.owner(window)
						.title("Input")
						.valid(State.present(component))
						.show();
		// end::input[]
	}

	private void exception(Window window, Exception exception) {
		// tag::exception[]
		Dialogs.exception()
						.owner(window)
						.title("Exception")
						.unwrap(List.of(RuntimeException.class))
						// Don't include system properties
						.systemProperties(false)
						.show(exception);
		// end::exception[]
	}

	private void calendar(Window window) {
		// tag::calendar[]
		Dialogs.calendar()
						.owner(window)
						.title("Calendar")
						.selectLocalDate()
						.ifPresent(System.out::println);
		// end::calendar[]
	}

	private void progress(Window window) {
		// tag::progress[]
		Dialogs.progressWorker()
						.task(this::performTask)
						.owner(window)
						.title("Performing task")
						.onResult(this::handleResult)
						.onException(this::handleException)
						.execute();
		// end::progress[]
	}

	private void handleException(Exception exception) {}

	private void handleResult() {}

	private void performTask() {}

	private void onOk() {}

	private void onCancel() {}
}