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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.ui;

import is.codion.common.model.worker.ProgressWorker;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.plugin.flatlaf.themes.FlatLookAndFeelThemes;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.frame.Frames;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static javax.swing.BorderFactory.createEtchedBorder;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

/**
 * A complete application based on the common UI module only, no entities, no database.
 */
public final class GreeterDemo {

	// tag::model[]
	// The model, observable values and states, no Swing in sight
	static final class GreeterModel {

		Value<String> name = Value.nullable();
		Value<Integer> exclamations = Value.nonNull(1);
		State shout = State.state();
		Value<String> greeting = Value.nullable();
		State working = State.state();

		// derived, active while a name is present and no greeting is in the works
		ObservableState ready = State.and(State.present(name), working.not());

		// a task to perform in a background thread
		String createGreeting() throws InterruptedException {
			Thread.sleep(500);// greeting is hard work
			String result = "Hello " + name.get() + "!".repeat(exclamations.getOrThrow());

			return shout.is() ? result.toUpperCase() : result;
		}
	}
	// end::model[]

	// tag::view[]
	// The view, plain Swing components, built and bound to the model
	// is.codion.swing.common.ui.component.Components.* is statically imported
	static JPanel createPanel(GreeterModel model) {
		// An action, enabled only while the model is ready,
		// performing its work off the Event Dispatch Thread
		Control greet = Control.builder()
						.command(() -> ProgressWorker.builder()
										.task(model::createGreeting)
										.onWorking(model.working::set)
										.onResult(model.greeting::set)
										.onException(Dialogs.exception()::show)
										.execute())
						.caption("Greet")
						.mnemonic('G')
						.enabled(model.ready)
						.build();

		JPanel form = form()
						.add(stringField()
										.link(model.name)
										.label("Name")
										.columns(15))
						.add(integerSpinner()
										.link(model.exclamations)
										.label("Exclamations")
										.range(0, 5))
						.add(checkBox()
										.link(model.shout)
										.text("Shout"))
						.build();

		return borderLayoutPanel()
						.center(form)
						.south(gridLayoutPanel(2, 1)
										.add(label()
														.text(model.greeting)
														.horizontalAlignment(CENTER)
														.border(createEtchedBorder()))
										.add(button()
														.control(greet)))
						.border(emptyBorder())
						.build();
	}
	// end::view[]

	// tag::main[]
	public static void main(String[] args) {
		FlatLookAndFeelThemes.addAll();
		SwingUtilities.invokeLater(GreeterDemo::start);
	}

	private static void start() {
		LookAndFeelProvider.findLookAndFeel(FlatDarculaLaf.class)
						.ifPresent(LookAndFeelEnabler::enable);

		Frames.builder()
						.component(createPanel(new GreeterModel()))
						.title("Greeter")
						.defaultCloseOperation(EXIT_ON_CLOSE)
						.centerFrame(true)
						.show();
	}
	// end::main[]
}
