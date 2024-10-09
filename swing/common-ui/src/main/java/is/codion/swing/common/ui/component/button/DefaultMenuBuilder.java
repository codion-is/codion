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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.Controls.ControlsBuilder;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

final class DefaultMenuBuilder extends AbstractComponentBuilder<Void, JMenu, MenuBuilder> implements MenuBuilder {

	private final ControlsBuilder controlsBuilder;

	private final List<MenuListener> menuListeners = new ArrayList<>();
	private final List<PopupMenuListener> popupMenuListeners = new ArrayList<>();
	private MenuItemBuilder<?, ?> menuItemBuilder = MenuItemBuilder.builder();
	private ToggleMenuItemBuilder<?, ?> toggleMenuItemBuilder = CheckBoxMenuItemBuilder.builder();

	DefaultMenuBuilder(Controls controls) {
		this.controlsBuilder = controls == null ? Controls.builder() : controls.copy();
	}

	@Override
	public MenuBuilder action(Action action) {
		controlsBuilder.action(requireNonNull(action));
		return this;
	}

	@Override
	public MenuBuilder control(Control control) {
		return action(requireNonNull(control));
	}

	@Override
	public MenuBuilder controls(Controls controls) {
		this.controlsBuilder.control(requireNonNull(controls));
		return this;
	}

	@Override
	public MenuBuilder separator() {
		this.controlsBuilder.separator();
		return this;
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
	public MenuBuilder menuItemBuilder(MenuItemBuilder<?, ?> menuItemBuilder) {
		this.menuItemBuilder = requireNonNull(menuItemBuilder);
		return this;
	}

	@Override
	public MenuBuilder toggleMenuItemBuilder(ToggleMenuItemBuilder<?, ?> toggleMenuItemBuilder) {
		this.toggleMenuItemBuilder = requireNonNull(toggleMenuItemBuilder);
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
		controlsBuilder.build().actions().stream()
						.filter(new IsControlsInstance())
						.map(new CastToControls())
						.filter(new IsNotEmptyControls())
						.forEach(new AddControlsToMenu(menuBar));

		return menuBar;
	}

	@Override
	protected JMenu createComponent() {
		Controls controls = controlsBuilder.build();
		JMenu menu = new JMenu(controls);
		menuListeners.forEach(new AddMenuListener(menu));
		new MenuControlHandler(menu, controls, menuItemBuilder, toggleMenuItemBuilder);

		return menu;
	}

	@Override
	protected ComponentValue<Void, JMenu> createComponentValue(JMenu component) {
		return new MenuComponentValue(component);
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
			return controls.notEmpty();
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
		private final MenuItemBuilder<?, ?> menuItemBuilder;
		private final ToggleMenuItemBuilder<?, ?> toggleMenuItemBuilder;

		private MenuControlHandler(JMenu menu, Controls controls,
															 MenuItemBuilder<?, ?> menuItemBuilder,
															 ToggleMenuItemBuilder<?, ?> toggleMenuItemBuilder) {
			this.menu = menu;
			this.menuItemBuilder = menuItemBuilder;
			this.toggleMenuItemBuilder = toggleMenuItemBuilder;
			cleanupSeparators(new ArrayList<>(controls.actions())).forEach(this);
		}

		@Override
		void onSeparator() {
			menu.addSeparator();
		}

		@Override
		void onControl(Control control) {
			menu.add(menuItemBuilder.control(control).build());
		}

		@Override
		void onToggleControl(ToggleControl toggleControl) {
			menu.add(toggleMenuItemBuilder.toggleControl(toggleControl).build());
		}

		@Override
		void onControls(Controls controls) {
			JMenu subMenu = new JMenu(controls);
			new MenuControlHandler(subMenu, controls, menuItemBuilder, toggleMenuItemBuilder);
			this.menu.add(subMenu);
		}

		@Override
		void onAction(Action action) {
			menu.add(action);
		}
	}

	private static final class MenuComponentValue extends AbstractComponentValue<Void, JMenu> {

		private MenuComponentValue(JMenu component) {
			super(component);
		}

		@Override
		protected Void getComponentValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setComponentValue(Void value) {
			throw new UnsupportedOperationException();
		}
	}
}
