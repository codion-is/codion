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
package is.codion.manual.keybinding;

import is.codion.common.item.Item;
import is.codion.manual.keybinding.KeyBindingModel.KeyBindingColumns.ColumnId;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

final class KeyBindingModel {

	private static final Set<String> EXCLUDED_COMPONENTS = Set.of("PopupMenuSeparator", "ToolBarSeparator", "DesktopIcon");

	private static final String PACKAGE = "javax.swing.";
	private static final String PRESSED = "pressed ";
	private static final String RELEASED = "released ";

	private final FilterComboBoxModel<String> componentModel;
	private final FilterTableModel<KeyBindingRow, ColumnId> tableModel;

	KeyBindingModel(FilterComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelModel) {
		this.componentModel = FilterComboBoxModel.builder(new ComponentItems(lookAndFeelModel)).build();
		this.componentModel.items().refresh();
		this.tableModel = FilterTableModel.builder(new KeyBindingColumns())
						.supplier(new KeyBindingItems())
						.build();
		bindEvents(lookAndFeelModel);
	}

	FilterComboBoxModel<String> componentModel() {
		return componentModel;
	}

	FilterTableModel<KeyBindingRow, ColumnId> tableModel() {
		return tableModel;
	}

	private void bindEvents(FilterComboBoxModel<?> lookAndFeelModel) {
		// Refresh the component combo box when a look and feel is selected
		lookAndFeelModel.selection().item().addListener(componentModel.items()::refresh);
		// Refresh the table model when the component combo box has been refreshed
		componentModel.items().refresher().success().addListener(tableModel.items()::refresh);
		// And when a component is selected
		componentModel.selection().item().addListener(tableModel.items()::refresh);
	}

	record KeyBindingRow(String action, String whenFocused, String whenInFocusedWindow, String whenAncestor) {

		Object value(ColumnId columnId) {
			return switch (columnId) {
				case ACTION -> action;
				case WHEN_FOCUSED -> whenFocused;
				case WHEN_IN_FOCUSED_WINDOW -> whenInFocusedWindow;
				case WHEN_ANCESTOR -> whenAncestor;
			};
		}
	}

	static final class KeyBindingColumns implements TableColumns<KeyBindingRow, ColumnId> {

		enum ColumnId {
			ACTION,
			WHEN_FOCUSED,
			WHEN_IN_FOCUSED_WINDOW,
			WHEN_ANCESTOR
		}

		private static final List<ColumnId> IDENTIFIERS = List.of(ColumnId.values());

		@Override
		public List<ColumnId> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(ColumnId columnId) {
			return String.class;
		}

		@Override
		public Object value(KeyBindingRow row, ColumnId columnId) {
			return row.value(columnId);
		}
	}

	// Provides the items when populating the component combo box model
	private static final class ComponentItems implements Supplier<Collection<String>> {

		private final FilterComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelModel;

		private ComponentItems(FilterComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelModel) {
			this.lookAndFeelModel = lookAndFeelModel;
		}

		@Override
		public Collection<String> get() {
			return lookAndFeelModel.selection().item().optional()
							.map(Item::value)
							.map(LookAndFeelProvider::lookAndFeel)
							.map(LookAndFeel::getDefaults)
							.map(Hashtable::keySet)
							.map(Collection::stream)
							.map(keys -> keys
											.map(Object::toString)
											.map(ComponentItems::componentName)
											.flatMap(Optional::stream)
											.sorted()
											.toList())
							.orElse(List.of());
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

	// Provides the rows when populating the key binding table model
	private final class KeyBindingItems implements Supplier<Collection<KeyBindingRow>> {

		@Override
		public Collection<KeyBindingRow> get() {
			return componentModel.selection().item().optional()
							.map(KeyBindingItems::componentClassName)
							.map(KeyBindingItems::keyBindings)
							.orElse(List.of());
		}

		private static String componentClassName(String componentName) {
			if (componentName.equals("JTableHeader")) {
				return PACKAGE + "table." + componentName;
			}

			return PACKAGE + componentName;
		}

		private static List<KeyBindingRow> keyBindings(String componentClassName) {
			try {
				JComponent component = (JComponent) Class.forName(componentClassName).getDeclaredConstructor().newInstance();
				ActionMap actionMap = component.getActionMap();
				Object[] allKeys = actionMap.allKeys();
				if (allKeys == null) {
					return List.of();
				}

				return Arrays.stream(allKeys)
								.sorted(comparing(Objects::toString))
								.map(actionKey -> row(actionKey, component))
								.toList();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private static KeyBindingRow row(Object actionKey, JComponent component) {
			return new KeyBindingRow(actionKey.toString(),
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
							.map(KeyBindingItems::movePressedReleased)
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
}
