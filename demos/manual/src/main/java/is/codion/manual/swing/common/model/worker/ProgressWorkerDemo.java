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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.model.worker;

import is.codion.common.model.CancelException;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressResultTaskHandler;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTaskHandler;
import is.codion.swing.common.model.worker.ProgressWorker.ResultTaskHandler;
import is.codion.swing.common.model.worker.ProgressWorker.TaskHandler;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static is.codion.swing.common.ui.component.Components.*;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JOptionPane.showMessageDialog;

final class ProgressWorkerDemo {

	private static final Logger LOG = Logger.getLogger("none");

	private static JFrame applicationFrame;

	private ProgressWorkerDemo() {}

	private static void task() {
		// tag::taskWorker[]
		// A non-progress aware task, producing no result
		ProgressWorker.Task task = () -> {
			// Perform the task
		};

		ProgressWorker.builder()
						.task(task)
						.onException(exception ->
										Dialogs.exception()
														.owner(applicationFrame)
														.show(exception))
						.execute();
		// end::taskWorker[]
	}

	private static void taskHandler() {
		// tag::taskHandler[]
		// TaskHandler encapsulates the task and its handlers in a single class.
		// Handler interface methods are called first, followed by
		// any handlers added via the builder, in the order they were added.
		// This enables a layered approach where the handler interface
		// handles model-level concerns (logging, state updates) while
		// builder handlers handle UI-level concerns (displaying dialogs).
		ProgressWorker.TaskHandler task = new TaskHandler() {

			@Override
			public void execute() throws Exception {
				// Perform the task
			}

			// Called first on exception: log the error (model-level)
			@Override
			public void onException(Exception exception) {
				LOG.log(Level.WARNING, exception.getMessage());
			}
		};

		ProgressWorker.builder()
						.task(task)
						// Called after the handler's onException: display the error (UI-level)
						.onException(exception -> Dialogs.exception()
										.owner(applicationFrame)
										.show(exception))
						.execute();
		// end::taskHandler[]
	}

	private static void resultTask() {
		// tag::resultTaskWorker[]
		// A non-progress aware task, producing a result
		ProgressWorker.ResultTask<String> task = () -> {
			// Perform the task
			return "Result";
		};

		ProgressWorker.builder()
						.task(task)
						.onResult(result ->
										showMessageDialog(applicationFrame, result))
						.onException(exception ->
										Dialogs.exception()
														.owner(applicationFrame)
														.show(exception))
						.execute();
		// end::resultTaskWorker[]
	}

	private static void resultTaskHandler() {
		// tag::resultTaskHandler[]
		// ResultTaskHandler encapsulates a result-producing task and its handlers.
		// The handler's onResult and onException are called first (model-level),
		// then the builder's handlers are called after (UI-level).
		ResultTaskHandler<String> task = new ResultTaskHandler<String>() {

			@Override
			public String execute() throws Exception {
				// Perform the task
				return "Result";
			}

			// Called first on success: log the result (model-level)
			@Override
			public void onResult(String result) {
				LOG.log(Level.INFO, result);
			}

			// Called first on exception: log the error (model-level)
			@Override
			public void onException(Exception exception) {
				LOG.log(Level.WARNING, exception.getMessage());
			}
		};

		ProgressWorker.builder()
						.task(task)
						// Called after the handler's onResult: display the result (UI-level)
						.onResult(result -> showMessageDialog(applicationFrame, result))
						// Called after the handler's onException: display the error (UI-level)
						.onException(exception -> Dialogs.exception()
										.owner(applicationFrame)
										.show(exception))
						.execute();
		// end::resultTaskHandler[]
	}

	private static void progressTask() {
		// tag::progressTaskWorker[]
		// A progress aware task, producing no result
		ProgressWorker.ProgressTask<String> task = progressReporter -> {
			// Perform the task
			progressReporter.report(42);
			progressReporter.publish("Message");
		};

		ProgressWorker.builder()
						.task(task)
						.onProgress(progress ->
										System.out.println("Progress: " + progress))
						.onPublish(message ->
										showMessageDialog(applicationFrame, message))
						.onException(exception ->
										Dialogs.exception()
														.owner(applicationFrame)
														.show(exception))
						.execute();
		// end::progressTaskWorker[]
	}

	private static void progressTaskHandler() {
		// tag::progressTaskHandler[]
		// ProgressTaskHandler encapsulates a progress-aware task and its handlers.
		// The handler's methods are called first (model-level),
		// then the builder's handlers are called after (UI-level).
		ProgressTaskHandler<String> task = new ProgressTaskHandler<String>() {

			@Override
			public void execute(ProgressReporter<String> progressReporter) throws Exception {
				// Perform the task
				for (int i = 0; i < maximum(); i++) {
					progressReporter.report(i);
					progressReporter.publish("Message " + i);
				}
			}

			@Override
			public void onProgress(int progress) {
				System.out.println("Progress: " + progress);
			}

			@Override
			public void onPublish(List<String> message) {
				displayMessage(message);
			}

			// Called first on exception: log the error (model-level)
			@Override
			public void onException(Exception exception) {
				LOG.log(Level.WARNING, exception.getMessage());
			}
		};

		ProgressWorker.builder()
						.task(task)
						// Called after the handler's onException: display the error (UI-level)
						.onException(exception -> Dialogs.exception()
										.owner(applicationFrame)
										.show(exception))
						.execute();
		// end::progressTaskHandler[]
	}

	private static void displayMessage(List<String> message) {}

	// tag::layeredModel[]
	// A model class that exposes a task for importing data.
	// The handler interface methods handle model-level concerns:
	// updating internal state on success and logging errors.
	static final class DataImportModel {

		private static final Logger LOG = Logger.getLogger(DataImportModel.class.getName());

		private final List<String> importedItems = new ArrayList<>();

		// Returns a task that can be further configured by the UI layer
		ImportTask importTask(List<String> items) {
			return new ImportTask(items);
		}

		List<String> importedItems() {
			return importedItems;
		}

		final class ImportTask implements ResultTaskHandler<Integer> {

			private final List<String> items;

			private ImportTask(List<String> items) {
				this.items = items;
			}

			@Override
			public Integer execute() throws Exception {
				// Perform the import
				int count = 0;
				for (String item : items) {
					importItem(item);
					count++;
				}

				return count;
			}

			// Model-level: update internal state after a successful import
			@Override
			public void onResult(Integer count) {
				importedItems.addAll(items);
			}

			// Model-level: log the error
			@Override
			public void onException(Exception exception) {
				LOG.log(Level.SEVERE, "Import failed", exception);
			}

			private void importItem(String item) throws Exception {
				// Import the item
			}
		}
	}
	// end::layeredModel[]

	// tag::layeredUI[]
	// The UI layer creates the task from the model and adds
	// its own handlers for user-facing feedback.
	// These run after the model-level handlers.
	static final class DataImportPanel extends JPanel {

		private final DataImportModel model;

		DataImportPanel(DataImportModel model) {
			this.model = model;
		}

		void performImport(List<String> items) {
			ProgressWorker.builder()
							.task(model.importTask(items))
							// UI-level: display the result to the user
							.onResult(count ->
											showMessageDialog(this, count + " items imported"))
							// UI-level: display the error to the user
							.onException(exception ->
											Dialogs.exception()
															.owner(this)
															.show(exception))
							.execute();
		}
	}
	// end::layeredUI[]

	private static void progressResultTask() {
		// tag::progressResultWorker[]
		// A reusable, cancellable task, producing a result.
		// Displays a progress bar in a dialog while running.
		var task = new DemoProgressResultTask();

		ProgressWorker.builder()
						.task(task.prepare(142))
						.execute();
		// end::progressResultWorker[]
	}

	// tag::progressResultTask1[]
	static final class DemoProgressResultTask implements ProgressResultTaskHandler<Integer, String> {

		private final JProgressBar progressBar = progressBar()
						.indeterminate(false)
						.stringPainted(true)
						.string("")
						.build();
		// Indicates whether the task has been cancelled
		private final AtomicBoolean cancelled = new AtomicBoolean();
		// A Control for setting the cancelled state
		private final Control cancel = Control.builder()
						.command(() -> cancelled.set(true))
						.caption("Cancel")
						.mnemonic('C')
						.build();
		// A panel containing the progress bar and cancel button
		private final JPanel progressPanel = borderLayoutPanel()
						.center(progressBar)
						.east(button()
										.control(cancel))
						.build();
		// The dialog displaying the progress panel
		private final JDialog dialog = Dialogs.builder()
						.component(progressPanel)
						.owner(applicationFrame)
						// Trigger the cancel control with the Escape key
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_ESCAPE)
										.action(cancel))
						// Prevent the dialog from closing on Escape
						.disposeOnEscape(false)
						.build();

		private int taskSize;

		@Override
		public Integer execute(ProgressReporter<String> progressReporter) throws Exception {
			List<Integer> result = new ArrayList<>();
			for (int i = 0; i < taskSize; i++) {
				Thread.sleep(50);
				if (cancelled.get()) {
					throw new CancelException();
				}
				result.add(i);
				reportProgress(progressReporter, i);
			}

			return result.stream()
							.mapToInt(Integer::intValue)
							.sum();
		}

		@Override
		public int maximum() {
			return taskSize;
		}

		@Override
		public void onStarted() {
			dialog.setVisible(true);
		}

		@Override
		public void onProgress(int progress) {
			progressBar.setValue(progress);
		}

		@Override
		public void onPublish(List<String> strings) {
			progressBar.setString(strings.get(0));
		}

		@Override
		public void onDone() {
			dialog.setVisible(false);
			// end::progressResultTask1[]
			// DEMO ONLY CODE: So that the demo JVM exits
			// when the demo is done, otherwise not required
			dialog.dispose();
			// tag::progressResultTask2[]
		}

		@Override
		public void onCancelled() {
			showMessageDialog(applicationFrame, "Cancelled");
		}

		@Override
		public void onException(Exception exception) {
			Dialogs.exception()
							.owner(applicationFrame)
							.show(exception);
		}

		@Override
		public void onResult(Integer result) {
			showMessageDialog(applicationFrame, "Result : " + result);
		}

		// Makes this task reusable by resetting the internal state
		private DemoProgressResultTask prepare(int taskSize) {
			this.taskSize = taskSize;
			progressBar.getModel().setMaximum(taskSize);
			cancelled.set(false);

			return this;
		}

		private void reportProgress(ProgressReporter<String> reporter, int progress) {
			reporter.report(progress);
			if (progress < taskSize * 0.5) {
				reporter.publish("Going strong");
			}
			else if (progress > taskSize * 0.5 && progress < taskSize * 0.85) {
				reporter.publish("Half way there");
			}
			else if (progress > taskSize * 0.85) {
				reporter.publish("Almost done");
			}
		}
	}
	// end::progressResultTask2[]

	public static void main(String[] args) {
		progressResultTask();
	}
}
