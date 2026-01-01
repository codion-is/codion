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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultMenuBuilder extends AbstractComponentBuilder<JMenu, MenuBuilder> implements MenuBuilder {

	static final ControlsStep CONTROLS = new DefaultControlsStep();

	private final List<MenuListener> menuListeners = new ArrayList<>();
	private final List<PopupMenuListener> popupMenuListeners = new ArrayList<>();

	private final Controls controls;

	private Function<Action, JMenuItem> actionMenuItem = new DefaultActionMenuItem();
	private Function<Control, JMenuItem> controlMenuItem = new DefaultControlMenuItem();
	private Function<ToggleControl, JCheckBoxMenuItem> toggleControlMenuItem = new DefaultToggleControlMenuItem();

	DefaultMenuBuilder(Controls controls) {
		this.controls = controls;
	}

	@Override
	public MenuBuilder menuListener(MenuListener menuListener) {
		menuListeners.add(requireNonNull(menuListener));
		return this;
	}

	@Override
	public MenuBuilder popupMenuListener(PopupMenuListener popupMenuListener) {
		popupMenuListeners.add(requireNonNull(popupMenuListener));
		return this;
	}

	@Override
	public MenuBuilder actionMenuItem(Function<Action, JMenuItem> menuItem) {
		this.actionMenuItem = requireNonNull(menuItem);
		return this;
	}

	@Override
	public MenuBuilder controlMenuItem(Function<Control, JMenuItem> controlMenuItem) {
		this.controlMenuItem = requireNonNull(controlMenuItem);
		return this;
	}

	@Override
	public MenuBuilder toggleControlMenuItem(Function<ToggleControl, JCheckBoxMenuItem> toggleControlMenuItem) {
		this.toggleControlMenuItem = requireNonNull(toggleControlMenuItem);
		return this;
	}

	@Override
	public JPopupMenu buildPopupMenu() {
		JPopupMenu popupMenu = createComponent().getPopupMenu();
		popupMenuListeners.forEach(new AddPopupMenuListener(popupMenu));

		return popupMenu;
	}

	@Override
	public JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		controls.actions().stream()
						.filter(new IsControlsInstance())
						.map(new CastToControls())
						.filter(new IsNotEmptyControls())
						.forEach(new AddControlsToMenu(menuBar));

		return menuBar;
	}

	@Override
	protected JMenu createComponent() {
		JMenu menu = new JMenu(controls);
		menuListeners.forEach(new AddMenuListener(menu));
		new MenuControlHandler(menu, controls, actionMenuItem, controlMenuItem, toggleControlMenuItem);

		return menu;
	}

	private static final class DefaultActionMenuItem implements Function<Action, JMenuItem> {

		@Override
		public JMenuItem apply(Action action) {
			return MenuItemBuilder.builder().action(action).build();
		}
	}

	private static final class DefaultControlMenuItem implements Function<Control, JMenuItem> {

		@Override
		public JMenuItem apply(Control control) {
			return MenuItemBuilder.builder().control(control).build();
		}
	}

	private static final class DefaultToggleControlMenuItem implements Function<ToggleControl, JCheckBoxMenuItem> {

		@Override
		public JCheckBoxMenuItem apply(ToggleControl toggleControl) {
			return CheckBoxMenuItemBuilder.builder().toggle(toggleControl).build();
		}
	}

	private static final class DefaultControlsStep implements ControlsStep {

		@Override
		public MenuBuilder action(Action action) {
			return controls(Controls.builder()
							.action(requireNonNull(action))
							.build());
		}

		@Override
		public MenuBuilder control(Control control) {
			return controls(Controls.builder()
							.control(requireNonNull(control))
							.build());
		}

		@Override
		public MenuBuilder control(Supplier<? extends Control> control) {
			return controls(Controls.builder()
							.control(requireNonNull(control))
							.build());
		}

		@Override
		public MenuBuilder controls(Controls controls) {
			return new DefaultMenuBuilder(requireNonNull(controls));
		}

		@Override
		public MenuBuilder controls(Supplier<Controls> controls) {
			return new DefaultMenuBuilder(requireNonNull(controls).get());
		}
	}

	private static final class IsControlsInstance implements Predicate<Action> {

		@Override
		public boolean test(Action action) {
			return action instanceof Controls;
		}
	}

	private static final class CastToControls implements Function<Action, Controls> {

		@Override
		public Controls apply(Action action) {
			return (Controls) action;
		}
	}

	private static final class IsNotEmptyControls implements Predicate<Controls> {

		@Override
		public boolean test(Controls controls) {
			return controls.size() > 0;
		}
	}

	private static final class AddControlsToMenu implements Consumer<Controls> {

		private final JMenuBar menuBar;

		private AddControlsToMenu(JMenuBar menuBar) {
			this.menuBar = menuBar;
		}

		@Override
		public void accept(Controls controls) {
			menuBar.add(new DefaultMenuBuilder(controls).createComponent());
		}
	}

	private static final class AddPopupMenuListener implements Consumer<PopupMenuListener> {

		private final JPopupMenu popupMenu;

		private AddPopupMenuListener(JPopupMenu popupMenu) {
			this.popupMenu = popupMenu;
		}

		@Override
		public void accept(PopupMenuListener popupMenuListener) {
			popupMenu.addPopupMenuListener(popupMenuListener);
		}
	}

	private static final class AddMenuListener implements Consumer<MenuListener> {

		private final JMenu menu;

		private AddMenuListener(JMenu menu) {
			this.menu = menu;
		}

		@Override
		public void accept(MenuListener menuListener) {
			menu.addMenuListener(menuListener);
		}
	}

	private static final class MenuControlHandler extends ControlHandler {

		private final JMenu menu;
		private final Function<Action, JMenuItem> actionMenuItem;
		private final Function<Control, JMenuItem> controlMenuItem;
		private final Function<ToggleControl, JCheckBoxMenuItem> toggleControlMenuItem;

		private MenuControlHandler(JMenu menu, Controls controls,
															 Function<Action, JMenuItem> actionMenuItem,
															 Function<Control, JMenuItem> controlMenuItem,
															 Function<ToggleControl, JCheckBoxMenuItem> toggleControlMenuItem) {
			this.menu = menu;
			this.actionMenuItem = actionMenuItem;
			this.controlMenuItem = controlMenuItem;
			this.toggleControlMenuItem = toggleControlMenuItem;
			cleanupSeparators(new ArrayList<>(controls.actions())).forEach(this);
		}

		@Override
		void onSeparator() {
			menu.addSeparator();
		}

		@Override
		void onControl(Control control) {
			menu.add(controlMenuItem.apply(control));
		}

		@Override
		void onToggleControl(ToggleControl toggleControl) {
			menu.add(toggleControlMenuItem.apply(toggleControl));
		}

		@Override
		void onControls(Controls controls) {
			JMenu subMenu = new JMenu(controls);
			new MenuControlHandler(subMenu, controls, actionMenuItem, controlMenuItem, toggleControlMenuItem);
			this.menu.add(subMenu);
		}

		@Override
		void onAction(Action action) {
			menu.add(actionMenuItem.apply(action));
		}
	}
}
