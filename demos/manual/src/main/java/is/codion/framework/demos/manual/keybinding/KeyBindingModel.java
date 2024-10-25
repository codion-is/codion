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
package is.codion.framework.demos.manual.keybinding;

import is.codion.common.item.Item;
import is.codion.framework.demos.manual.keybinding.KeyBindingModel.KeyBindingColumns.Id;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class KeyBindingModel {

	private static final Collection<String> EXCLUDED_COMPONENTS = asList("PopupMenuSeparator", "ToolBarSeparator", "DesktopIcon");

	private static final String PACKAGE = "javax.swing.";
	private static final String PRESSED = "pressed ";
	private static final String RELEASED = "released ";

	private final FilterTableModel<KeyBinding, Id> tableModel;
	private final FilterComboBoxModel<String> componentComboBoxModel;

	KeyBindingModel(FilterComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelComboBoxModel) {
		this.componentComboBoxModel = FilterComboBoxModel.builder(new ComponentItems(lookAndFeelComboBoxModel)).build();
		this.componentComboBoxModel.refresh();
		this.tableModel = FilterTableModel.builder(new KeyBindingColumns())
						.supplier(new KeyBindingItems())
						.build();
		bindEvents(lookAndFeelComboBoxModel);
	}

	FilterComboBoxModel<String> componentComboBoxModel() {
		return componentComboBoxModel;
	}

	FilterTableModel<KeyBinding, Id> tableModel() {
		return tableModel;
	}

	private void bindEvents(FilterComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelComboBoxModel) {
		componentComboBoxModel.refresher().success().addListener(tableModel::refresh);
		componentComboBoxModel.selection().item().addListener(tableModel::refresh);
		lookAndFeelComboBoxModel.selection().item().addListener(componentComboBoxModel::refresh);
	}

	private static String className(String componentName) {
		if (componentName.equals("JTableHeader")) {
			return PACKAGE + "table." + componentName;
		}

		return PACKAGE + componentName;
	}

	static final class KeyBinding {

		private final String action;
		private final String whenFocused;
		private final String whenInFocusedWindow;
		private final String whenAncestor;

		private KeyBinding(String action, String whenFocused, String whenInFocusedWindow, String whenAncestor) {
			this.action = action;
			this.whenFocused = whenFocused;
			this.whenInFocusedWindow = whenInFocusedWindow;
			this.whenAncestor = whenAncestor;
		}

		private static KeyBinding create(Object actionKey, JComponent component) {
			return new KeyBinding(actionKey.toString(),
							keyStrokes(actionKey, component.getInputMap(JComponent.WHEN_FOCUSED)),
							keyStrokes(actionKey, component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)),
							keyStrokes(actionKey, component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)));
		}

		private static String keyStrokes(Object actionKey, InputMap inputMap) {
			KeyStroke[] allKeys = inputMap.allKeys();
			if (allKeys == null) {
				return "";
			}

			return Arrays.stream(allKeys)
							.filter(keyStroke -> inputMap.get(keyStroke).equals(actionKey))
							.map(Objects::toString)
							.map(KeyBinding::movePressedReleased)
							.collect(joining(", "));
		}

		private static String movePressedReleased(String keyStroke) {
			if (keyStroke.contains(PRESSED)) {
				return keyStroke.replace(PRESSED, "") + " pressed";
			}
			if (keyStroke.contains(RELEASED)) {
				return keyStroke.replace(RELEASED, "") + " released";
			}

			return keyStroke;
		}
	}

	private final class KeyBindingItems implements Supplier<Collection<KeyBinding>> {

		@Override
		public Collection<KeyBinding> get() {
			String componentName = componentComboBoxModel.getSelectedItem();
			if (componentName == null) {
				return List.of();
			}
			String componentClassName = className(componentName);
			try {
				JComponent component = (JComponent) Class.forName(componentClassName).getDeclaredConstructor().newInstance();
				ActionMap actionMap = component.getActionMap();
				Object[] allKeys = actionMap.allKeys();
				if (allKeys == null) {
					return List.of();
				}

				return Arrays.stream(allKeys)
								.sorted(comparing(Objects::toString))
								.map(actionKey -> KeyBinding.create(actionKey, component))
								.collect(toList());
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static final class KeyBindingColumns implements Columns<KeyBinding, Id> {

		public enum Id {
			ACTION_COLUMN,
			WHEN_FOCUSED_COLUMN,
			WHEN_IN_FOCUSED_WINDOW_COLUMN,
			WHEN_ANCESTOR_COLUMN
		}

		private static final List<Id> IDENTIFIERS = List.of(Id.values());

		@Override
		public List<Id> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(Id identifier) {
			return String.class;
		}

		@Override
		public Object value(KeyBinding keyBinding, Id identifier) {
			return switch (identifier) {
				case ACTION_COLUMN -> keyBinding.action;
				case WHEN_FOCUSED_COLUMN -> keyBinding.whenFocused;
				case WHEN_IN_FOCUSED_WINDOW_COLUMN -> keyBinding.whenInFocusedWindow;
				case WHEN_ANCESTOR_COLUMN -> keyBinding.whenAncestor;
			};
		}
	}

	private static final class ComponentItems implements Supplier<Collection<String>> {

		private final FilterComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelComboBoxModel;

		private ComponentItems(FilterComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelComboBoxModel) {
			this.lookAndFeelComboBoxModel = lookAndFeelComboBoxModel;
		}

		@Override
		public Collection<String> get() {
			Item<LookAndFeelProvider> selectedItem = lookAndFeelComboBoxModel.getSelectedItem();
			if (selectedItem == null) {
				return List.of();
			}

			LookAndFeelProvider lookAndFeelProvider = selectedItem.value();
			try {
				return lookAndFeelProvider.lookAndFeel().getDefaults().keySet().stream()
								.map(Object::toString)
								.map(ComponentItems::componentName)
								.flatMap(Optional::stream)
								.sorted()
								.collect(toList());
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private static Optional<String> componentName(String key) {
			if (key.endsWith("UI") && key.indexOf(".") == -1) {
				String componentName = key.substring(0, key.length() - 2);
				if (!EXCLUDED_COMPONENTS.contains(componentName)) {
					return Optional.of("J" + componentName);
				}
			}

			return Optional.empty();
		}
	}
}
