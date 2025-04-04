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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.component.button.ButtonPanelBuilder;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static is.codion.swing.common.ui.dialog.DefaultComponentDialogBuilder.createDialog;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

class DefaultActionDialogBuilder<B extends ActionDialogBuilder<B>> extends AbstractDialogBuilder<B> implements ActionDialogBuilder<B> {

	private final List<Action> actions = new ArrayList<>();
	private final JComponent component;
	private final Collection<Consumer<JDialog>> onShownConsumers = new ArrayList<>(1);

	private Action defaultAction;
	private Action escapeAction;
	private Border buttonPanelBorder = BorderFactory.createEmptyBorder(10, 10, 5, 10);
	private int buttonPanelConstraints = FlowLayout.TRAILING;
	private boolean modal = true;
	private boolean resizable = true;
	private Dimension size;

	DefaultActionDialogBuilder(JComponent component) {
		this.component = requireNonNull(component);
	}

	@Override
	public B action(Action action) {
		actions.add(requireNonNull(action));
		return self();
	}

	@Override
	public B defaultAction(Action defaultAction) {
		this.defaultAction = requireNonNull(defaultAction);
		if (!actions.contains(defaultAction)) {
			actions.add(defaultAction);
		}
		return self();
	}

	@Override
	public B escapeAction(Action escapeAction) {
		this.escapeAction = requireNonNull(escapeAction);
		if (!actions.contains(escapeAction)) {
			actions.add(escapeAction);
		}
		return self();
	}

	@Override
	public final B modal(boolean modal) {
		this.modal = modal;
		return self();
	}

	@Override
	public final B resizable(boolean resizable) {
		this.resizable = resizable;
		return self();
	}

	@Override
	public final B size(Dimension size) {
		this.size = size;
		return self();
	}

	@Override
	public final B buttonPanelConstraints(int buttonPanelConstraints) {
		this.buttonPanelConstraints = buttonPanelConstraints;
		return self();
	}

	@Override
	public final B buttonPanelBorder(Border buttonPanelBorder) {
		this.buttonPanelBorder = requireNonNull(buttonPanelBorder);
		return self();
	}

	@Override
	public final B onShown(Consumer<JDialog> onShown) {
		onShownConsumers.add(requireNonNull(onShown));
		return self();
	}

	@Override
	public final JDialog show() {
		JDialog dialog = build();
		dialog.setVisible(true);

		return dialog;
	}

	@Override
	public JDialog build() {
		if (actions.isEmpty()) {
			throw new IllegalStateException("No actions have been specified");
		}

		JPanel buttonPanel = ButtonPanelBuilder.builder(Controls.builder()
										.actions(actions))
						.build();
		JPanel panel = BorderLayoutPanelBuilder.builder(new BorderLayout())
						.centerComponent(component)
						.southComponent(PanelBuilder.builder(Layouts.flowLayout(buttonPanelConstraints))
										.add(buttonPanel)
										.border(buttonPanelBorder)
										.build())
						.build();

		JDialog dialog = createDialog(owner, title, icon, panel, size, locationRelativeTo,
						location, modal, resizable, onShownConsumers, keyEventBuilders);
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		if (defaultAction != null) {
			Arrays.stream(buttonPanel.getComponents())
							.filter(new IsButton())
							.map(new CastToButton())
							.filter(new IsButtonAction(defaultAction))
							.findFirst()
							.ifPresent(new SetDefaultButton(dialog));
		}
		if (escapeAction != null) {
			dialog.addWindowListener(new EscapeOnWindowClosingListener(escapeAction));
			KeyEvents.builder(VK_ESCAPE)
							.condition(WHEN_IN_FOCUSED_WINDOW)
							.action(escapeAction)
							.enable(dialog.getRootPane());
		}
		onBuildConsumers.forEach(consumer -> consumer.accept(dialog));

		return dialog;
	}

	protected final List<Action> actions() {
		return actions;
	}

	protected final JComponent component() {
		return component;
	}

	private static final class EscapeOnWindowClosingListener extends WindowAdapter {

		private final Action escapeAction;

		private EscapeOnWindowClosingListener(Action escapeAction) {
			this.escapeAction = escapeAction;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			escapeAction.actionPerformed(null);
		}
	}

	private static final class IsButton implements Predicate<Component> {

		@Override
		public boolean test(Component component) {
			return component instanceof JButton;
		}
	}

	private static final class CastToButton implements Function<Component, JButton> {

		@Override
		public JButton apply(Component component) {
			return (JButton) component;
		}
	}

	private static final class IsButtonAction implements Predicate<JButton> {

		private final Action defaultAction;

		private IsButtonAction(Action defaultAction) {
			this.defaultAction = defaultAction;
		}

		@Override
		public boolean test(JButton button) {
			return button.getAction() == defaultAction;
		}
	}

	private static final class SetDefaultButton implements Consumer<JButton> {

		private final JDialog dialog;

		private SetDefaultButton(JDialog dialog) {
			this.dialog = dialog;
		}

		@Override
		public void accept(JButton button) {
			dialog.getRootPane().setDefaultButton(button);
		}
	}
}
