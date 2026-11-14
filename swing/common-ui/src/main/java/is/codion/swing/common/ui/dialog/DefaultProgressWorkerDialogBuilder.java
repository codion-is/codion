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
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.model.worker.ProgressWorker;
import is.codion.common.model.worker.ProgressWorker.ProgressResultTask;
import is.codion.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.common.model.worker.ProgressWorker.ResultTask;
import is.codion.common.model.worker.ProgressWorker.Task;
import is.codion.swing.common.model.action.DelayedAction;
import is.codion.swing.common.ui.control.Control;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.RootPaneContainer;
import javax.swing.border.Border;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.swing.common.model.action.DelayedAction.delayedAction;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static javax.swing.JOptionPane.INFORMATION_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;

final class DefaultProgressWorkerDialogBuilder<T extends @Nullable Object, V> extends AbstractDialogBuilder<ProgressWorkerDialogBuilder<T, V>>
				implements ProgressWorkerDialogBuilder<T, V> {

	static final ProgressWorkerDialogBuilder.BuilderFactory BUILDER_FACTORY = new DefaultBuilderFactory();

	private final ProgressWorker.Builder<T, V> progressWorkerBuilder;
	private final ProgressDialog.Builder progressDialogBuilder = new ProgressDialog.DefaultBuilder();

	private final Consumer<Exception> displayExceptionInDialog = new DisplayExceptionInDialog();
	private final List<Consumer<Exception>> onException = new ArrayList<>(singleton(displayExceptionInDialog));
	private int showDelay = SHOW_DELAY.getOrThrow();
	private int hideDelay = HIDE_DELAY.getOrThrow();

	DefaultProgressWorkerDialogBuilder(Task task) {
		this.progressWorkerBuilder = (ProgressWorker.Builder<T, V>) ProgressWorker.builder().task(task);
	}

	DefaultProgressWorkerDialogBuilder(ResultTask<T> task) {
		this.progressWorkerBuilder = (ProgressWorker.Builder<T, V>) ProgressWorker.builder().task(task);
	}

	DefaultProgressWorkerDialogBuilder(ProgressTask<V> task) {
		this.progressWorkerBuilder = (ProgressWorker.Builder<T, V>) ProgressWorker.builder().task(task);
		this.progressDialogBuilder.maximum(task.maximum());
	}

	DefaultProgressWorkerDialogBuilder(ProgressResultTask<T, V> task) {
		this.progressWorkerBuilder = ProgressWorker.builder().task(task);
		this.progressDialogBuilder.maximum(task.maximum());
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> indeterminate(boolean indeterminate) {
		progressDialogBuilder.indeterminate(indeterminate);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> maximum(int maximum) {
		progressDialogBuilder.maximum(maximum);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> stringPainted(boolean stringPainted) {
		progressDialogBuilder.stringPainted(stringPainted);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> border(@Nullable Border border) {
		progressDialogBuilder.border(border);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> northComponent(@Nullable JComponent northComponent) {
		progressDialogBuilder.northComponent(northComponent);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> westComponent(@Nullable JComponent westComponent) {
		progressDialogBuilder.westComponent(westComponent);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> eastComponent(@Nullable JComponent eastComponent) {
		progressDialogBuilder.eastComponent(eastComponent);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> control(Supplier<? extends Control> control) {
		progressDialogBuilder.control(control);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> control(Control control) {
		progressDialogBuilder.control(control);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> progressBarSize(@Nullable Dimension progressBarSize) {
		progressDialogBuilder.progressBarSize(progressBarSize);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> delay(int show, int hide) {
		if (show < 0) {
			throw new IllegalArgumentException("show delay must be non-negative");
		}
		if (hide < 0) {
			throw new IllegalArgumentException("hide delay must be non-negative");
		}
		this.showDelay = show;
		this.hideDelay = hide;
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onWorking(Consumer<Boolean> onWorking) {
		this.progressWorkerBuilder.onWorking(onWorking);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onPublish(Consumer<List<V>> onPublish) {
		this.progressWorkerBuilder.onPublish(onPublish);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onSuccess(Runnable onSuccess) {
		this.progressWorkerBuilder.onSuccess(onSuccess);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onResult(Consumer<? super T> onResult) {
		this.progressWorkerBuilder.onResult(onResult);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onSuccess(String title, String message) {
		requireNonNull(title);
		requireNonNull(message);
		this.progressWorkerBuilder.onSuccess(() -> showMessageDialog(owner, message, title, INFORMATION_MESSAGE));
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onException(Consumer<Exception> onException) {
		this.onException.add(requireNonNull(onException));
		// Make sure the default exception handler is removed, whenever a custom one is specified
		this.onException.remove(displayExceptionInDialog);
		return this;
	}

	@Override
	public ProgressWorkerDialogBuilder<T, V> onException(String exceptionTitle) {
		return onException(new DisplayExceptionInDialog(requireNonNull(exceptionTitle)));
	}

	@Override
	public ProgressWorker<T, V> execute() {
		ProgressWorker<T, V> worker = build();
		worker.execute();

		return worker;
	}

	@Override
	public ProgressWorker<T, V> build() {
		onBuildConsumers.forEach(progressDialogBuilder::onBuild);
		onException.forEach(progressWorkerBuilder::onException);

		ProgressDialog progressDialog = progressDialogBuilder
						.owner(owner)
						.locationRelativeTo(locationRelativeTo)
						.title(title)
						.icon(icon)
						.build();

		DisplayDialog dialog = new DisplayDialog(progressDialog, owner == null ? null : new InputBlocker(owner));

		return progressWorkerBuilder
						.onStarted(dialog::show)
						.onProgress(progressDialog::setProgress)
						.onPublish(chunks -> progressDialog.setMessage(message(chunks)))
						.onDone(dialog::hide)
						.build();
	}

	/**
	 * Shows the dialog once {@link #SHOW_DELAY} has passed, so a task that finishes before then never
	 * shows one, and keeps it up for {@link #HIDE_DELAY} once it has, so one that appears does not
	 * flash past. Both run on the dispatch thread, {@link ProgressWorker} calling this from
	 * {@code onStarted} and {@code onDone}.
	 * <p>A dialog with an owner, being modal, blocks input from the start, see {@link InputBlocker}.
	 */
	private final class DisplayDialog {

		private final ProgressDialog progressDialog;
		private final @Nullable InputBlocker inputBlocker;

		private @Nullable DelayedAction show;
		private long shownAt;//when the dialog went up, zero until it does, which is what tells hide() it never did

		private DisplayDialog(ProgressDialog progressDialog, @Nullable InputBlocker inputBlocker) {
			this.progressDialog = progressDialog;
			this.inputBlocker = inputBlocker;
		}

		private void show() {
			if (inputBlocker != null) {
				inputBlocker.block();
			}
			show = delayedAction(() -> {
				//the modality takes over, setVisible() blocking the windows before any further event is dispatched
				unblock();
				//before, a modal dialog only returning from setVisible() once hidden
				shownAt = currentTimeMillis();
				progressDialog.setVisible(true);
			}, showDelay);
		}

		private void hide() {
			cancel();
			//onDone, called before onResult and onException, so a dialog they show receives input
			unblock();
			//measured from when it went up rather than from when it was asked for, so the dialog is shown the full hideDelay
			long remaining = shownAt == 0 ? 0 : hideDelay - (currentTimeMillis() - shownAt);
			delayedAction(() -> {
				progressDialog.setVisible(false);
				progressDialog.dispose();
			}, (int) Math.max(0, remaining));
		}

		private void cancel() {
			if (show != null) {
				show.cancel();
				show = null;
			}
		}

		private void unblock() {
			if (inputBlocker != null) {
				inputBlocker.unblock();
			}
		}
	}

	/**
	 * Blocks input from when the task starts until its dialog shows, the modality of the dialog then taking over, or
	 * until the task finishes, so that a modal dialog is modal from the start, the delay governing only when it shows.
	 * Input before then would otherwise reach the owner, a key typed into a form whose content the task then replaces,
	 * or a click moving it on to another row. Keys are discarded application wide, as the modality of the dialog does.
	 * The mouse is blocked within the owner, by a transparent component covering its layered pane, menu bar included,
	 * a component of its own rather than the glass pane, which two blockers overlapping would restore out of order.
	 */
	static final class InputBlocker implements KeyEventDispatcher {

		private static final Integer BLOCKING_LAYER = JLayeredPane.DRAG_LAYER + 1;

		private final Window owner;
		private final JComponent mouseBlocker = new MouseBlocker();

		private boolean blocking = false;

		InputBlocker(Window owner) {
			this.owner = owner;
		}

		void block() {
			if (!blocking) {
				blocking = true;
				KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
				if (owner instanceof RootPaneContainer) {
					JLayeredPane layeredPane = ((RootPaneContainer) owner).getLayeredPane();
					mouseBlocker.setBounds(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
					layeredPane.add(mouseBlocker, BLOCKING_LAYER);
				}
			}
		}

		void unblock() {
			if (blocking) {
				blocking = false;
				KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
				Container parent = mouseBlocker.getParent();
				if (parent != null) {
					parent.remove(mouseBlocker);
				}
			}
		}

		boolean blocking() {
			return blocking;
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent event) {
			//discarded
			return true;
		}

		/**
		 * Receives the mouse events within its bounds, having listeners, and does nothing with them
		 */
		private static final class MouseBlocker extends JComponent {

			private MouseBlocker() {
				MouseAdapter discard = new MouseAdapter() {};
				addMouseListener(discard);
				addMouseMotionListener(discard);
				addMouseWheelListener(discard);
			}
		}
	}

	private static class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public ProgressWorkerDialogBuilder<?, ?> task(Task task) {
			return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task));
		}

		@Override
		public <T extends @Nullable Object> ProgressWorkerDialogBuilder<T, ?> task(ResultTask<T> task) {
			return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task));
		}

		@Override
		public <V> ProgressWorkerDialogBuilder<?, V> task(ProgressTask<V> task) {
			return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task)).indeterminate(false);
		}

		@Override
		public <T extends @Nullable Object, V> ProgressWorkerDialogBuilder<T, V> task(ProgressResultTask<T, V> task) {
			return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task)).indeterminate(false);
		}
	}

	private @Nullable String message(List<V> chunks) {
		return chunks.isEmpty() ? null : Objects.toString(chunks.get(chunks.size() - 1));
	}

	private class DisplayExceptionInDialog implements Consumer<Exception> {

		private final String dialogTitle;

		private DisplayExceptionInDialog() {
			this(Messages.error());
		}

		private DisplayExceptionInDialog(String dialogTitle) {
			this.dialogTitle = dialogTitle;
		}

		@Override
		public void accept(Exception exception) {
			new DefaultExceptionDialogBuilder()
							.owner(owner)
							.title(dialogTitle)
							.show(exception);
		}
	}
}
