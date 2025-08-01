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

import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

final class DefaultComponentDialogBuilder extends AbstractDialogBuilder<ComponentDialogBuilder> implements ComponentDialogBuilder {

	private final Collection<Consumer<JDialog>> onShownConsumers = new ArrayList<>(1);
	private final Collection<Consumer<WindowEvent>> onOpenedConsumers = new ArrayList<>(1);
	private final Collection<Consumer<WindowEvent>> onClosedConsumers = new ArrayList<>(1);

	private @Nullable JComponent component;
	private boolean modal = true;
	private boolean resizable = true;
	private @Nullable Dimension size;
	private @Nullable Action enterAction;
	private @Nullable Observer<?> closeObserver;
	private @Nullable Consumer<State> confirmCloseListener;
	private boolean disposeOnEscape = true;

	@Override
	public ComponentDialogBuilder component(@Nullable JComponent component) {
		this.component = component;
		return this;
	}

	@Override
	public ComponentDialogBuilder modal(boolean modal) {
		this.modal = modal;
		return this;
	}

	@Override
	public ComponentDialogBuilder resizable(boolean resizable) {
		this.resizable = resizable;
		return this;
	}

	@Override
	public ComponentDialogBuilder size(@Nullable Dimension size) {
		this.size = size;
		return this;
	}

	@Override
	public ComponentDialogBuilder enterAction(Action enterAction) {
		this.enterAction = requireNonNull(enterAction);
		return this;
	}

	@Override
	public ComponentDialogBuilder closeObserver(@Nullable Observer<?> closeObserver) {
		this.closeObserver = closeObserver;
		return this;
	}

	@Override
	public ComponentDialogBuilder confirmCloseListener(@Nullable Consumer<State> confirmCloseListener) {
		this.confirmCloseListener = confirmCloseListener;
		return this;
	}

	@Override
	public ComponentDialogBuilder disposeOnEscape(boolean disposeOnEscape) {
		this.disposeOnEscape = disposeOnEscape;
		return this;
	}

	@Override
	public ComponentDialogBuilder onShown(Consumer<JDialog> onShown) {
		onShownConsumers.add(requireNonNull(onShown));
		return this;
	}

	@Override
	public ComponentDialogBuilder onOpened(Consumer<WindowEvent> onOpened) {
		onOpenedConsumers.add(requireNonNull(onOpened));
		return this;
	}

	@Override
	public ComponentDialogBuilder onClosed(Consumer<WindowEvent> onClosed) {
		onClosedConsumers.add(requireNonNull(onClosed));
		return this;
	}

	@Override
	public JDialog show() {
		JDialog dialog = build();
		dialog.setVisible(true);

		return dialog;
	}

	@Override
	public JDialog build() {
		JDialog dialog = createDialog(owner, title, icon, component, size, locationRelativeTo,
						location, modal, resizable, onShownConsumers, keyEventBuilders);
		if (enterAction != null) {
			KeyEvents.builder()
							.keyCode(VK_ENTER)
							.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.action(enterAction)
							.enable(dialog.getRootPane());
		}

		Action disposeAction = new DisposeDialogAction(new DialogSupplier(dialog), confirmCloseListener);
		dialog.addWindowListener(new DialogListener(disposeAction, onClosedConsumers, onOpenedConsumers));
		if (closeObserver == null) {
			dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			if (disposeOnEscape) {
				KeyEvents.builder()
								.keyCode(VK_ESCAPE)
								.condition(WHEN_IN_FOCUSED_WINDOW)
								.action(new DisposeDialogOnEscapeAction(dialog, confirmCloseListener))
								.enable(dialog.getRootPane());
			}
		}
		else {
			closeObserver.addListener(new CloseListener(disposeAction));
		}
		onBuildConsumers.forEach(consumer -> consumer.accept(dialog));

		return dialog;
	}

	static JDialog createDialog(@Nullable Window owner, @Nullable Observable<String> title, @Nullable ImageIcon icon,
															@Nullable JComponent component, @Nullable Dimension size, @Nullable Component locationRelativeTo,
															@Nullable Point location, boolean modal, boolean resizable,
															Collection<Consumer<JDialog>> onShownConsumers,
															List<KeyEvents.Builder> keyEventBuilders) {
		JDialog dialog = new JDialog(owner);
		if (title != null) {
			dialog.setTitle(title.get());
			title.addConsumer(new SetDialogTitle(dialog));
		}
		if (icon != null) {
			dialog.setIconImage(icon.getImage());
		}
		if (component != null) {
			dialog.setLayout(Layouts.borderLayout());
			dialog.add(component, BorderLayout.CENTER);
		}
		if (size != null) {
			dialog.setSize(size);
		}
		else {
			dialog.pack();
		}
		if (location != null) {
			dialog.setLocation(location);
		}
		else if (locationRelativeTo != null) {
			dialog.setLocationRelativeTo(locationRelativeTo);
		}
		else {
			dialog.setLocationRelativeTo(owner);
		}
		dialog.setModal(modal);
		dialog.setResizable(resizable);
		keyEventBuilders.forEach(new EnableKeyEvent(dialog));
		if (!onShownConsumers.isEmpty()) {
			dialog.addComponentListener(new OnShownAdapter(dialog, onShownConsumers));
		}

		return dialog;
	}

	private static final class EnableKeyEvent implements Consumer<KeyEvents.Builder> {

		private final JDialog dialog;

		private EnableKeyEvent(JDialog dialog) {
			this.dialog = dialog;
		}

		@Override
		public void accept(KeyEvents.Builder builder) {
			builder.enable(dialog.getRootPane());
		}
	}

	private static final class DialogSupplier implements Supplier<JDialog> {

		private final JDialog dialog;

		private DialogSupplier(JDialog dialog) {
			this.dialog = dialog;
		}

		@Override
		public JDialog get() {
			return dialog;
		}
	}

	private static final class DialogListener extends WindowAdapter {

		private final Action disposeAction;
		private final Collection<Consumer<WindowEvent>> onOpenedConsumers;
		private final Collection<Consumer<WindowEvent>> onClosedConsumers;

		private DialogListener(Action disposeAction,
													 Collection<Consumer<WindowEvent>> onClosedConsumers,
													 Collection<Consumer<WindowEvent>> onOpenedConsumers) {
			this.disposeAction = disposeAction;
			this.onClosedConsumers = onClosedConsumers;
			this.onOpenedConsumers = onOpenedConsumers;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			disposeAction.actionPerformed(null);
		}

		@Override
		public void windowClosed(WindowEvent e) {
			onClosedConsumers.forEach(consumer -> consumer.accept(e));
		}

		@Override
		public void windowOpened(WindowEvent e) {
			onOpenedConsumers.forEach(consumer -> consumer.accept(e));
		}
	}

	private static final class CloseListener implements Runnable {

		private final Action disposeAction;

		private CloseListener(Action disposeAction) {
			this.disposeAction = disposeAction;
		}

		@Override
		public void run() {
			disposeAction.actionPerformed(null);
		}
	}

	private static final class SetDialogTitle implements Consumer<String> {

		private final JDialog dialog;

		private SetDialogTitle(JDialog dialog) {
			this.dialog = dialog;
		}

		@Override
		public void accept(String title) {
			dialog.setTitle(title);
		}
	}

	private static final class OnShownAdapter extends ComponentAdapter {

		private final JDialog dialog;
		private final Collection<Consumer<JDialog>> onShownConsumers;

		private OnShownAdapter(JDialog dialog, Collection<Consumer<JDialog>> onShownConsumers) {
			this.dialog = dialog;
			this.onShownConsumers = onShownConsumers;
		}

		@Override
		public void componentShown(ComponentEvent e) {
			onShownConsumers.forEach(onShown -> onShown.accept(dialog));
		}
	}
}
