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
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;
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
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class KeyBindingModel {

  private static final Collection<String> EXCLUDED_COMPONENTS = asList("PopupMenuSeparator", "ToolBarSeparator", "DesktopIcon");

  private static final String PACKAGE = "javax.swing.";
  private static final String PRESSED = "pressed ";
  private static final String RELEASED = "released ";

  private static final int ACTION_COLUMN_INDEX = 0;
  private static final int WHEN_FOCUSED_COLUMN_INDEX = 1;
  private static final int WHEN_IN_FOCUSED_WINDOW_COLUMN_INDEX = 2;
  private static final int WHEN_ANCESTOR_COLUMN_INDEX = 3;

  private final FilteredTableModel<KeyBinding, Integer> tableModel;
  private final FilteredComboBoxModel<String> componentComboBoxModel;

  KeyBindingModel(FilteredComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelComboBoxModel) {
    this.componentComboBoxModel = createComponentComboBoxModel(lookAndFeelComboBoxModel);
    this.componentComboBoxModel.refresh();
    this.tableModel = FilteredTableModel.builder(new KeyBindingColumnFactory(), new KeyBindingValueProvider())
            .itemSupplier(new KeyBindingItemSupplier())
            .build();
    bindEvents(lookAndFeelComboBoxModel);
  }

  FilteredComboBoxModel<String> componentComboBoxModel() {
    return componentComboBoxModel;
  }

  FilteredTableModel<KeyBinding, Integer> tableModel() {
    return tableModel;
  }

  private void bindEvents(FilteredComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelComboBoxModel) {
    componentComboBoxModel.refresher().addRefreshListener(tableModel::refresh);
    componentComboBoxModel.addSelectionListener(component -> tableModel.refresh());
    lookAndFeelComboBoxModel.addSelectionListener(lookAndFeelProvider -> componentComboBoxModel.refresh());
  }

  private static String className(String componentName) {
    if (componentName.equals("JTableHeader")) {
      return PACKAGE + "table." + componentName;
    }

    return PACKAGE + componentName;
  }

  private static final class KeyBindingColumnFactory implements FilteredTableModel.ColumnFactory<Integer> {
    @Override
    public List<FilteredTableColumn<Integer>> createColumns() {
      FilteredTableColumn<Integer> action = FilteredTableColumn.builder(ACTION_COLUMN_INDEX)
              .headerValue("Action")
              .columnClass(String.class)
              .build();
      FilteredTableColumn<Integer> whenFocused = FilteredTableColumn.builder(WHEN_FOCUSED_COLUMN_INDEX)
              .headerValue("When Focused")
              .columnClass(String.class)
              .build();
      FilteredTableColumn<Integer> whenInFocusedWindow = FilteredTableColumn.builder(WHEN_IN_FOCUSED_WINDOW_COLUMN_INDEX)
              .headerValue("When in Focused Window")
              .columnClass(String.class)
              .build();
      FilteredTableColumn<Integer> whenAncestor = FilteredTableColumn.builder(WHEN_ANCESTOR_COLUMN_INDEX)
              .headerValue("When Ancestor")
              .columnClass(String.class)
              .build();

      return asList(action, whenFocused, whenInFocusedWindow, whenAncestor);
    }
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

  private final class KeyBindingItemSupplier implements Supplier<Collection<KeyBinding>> {

    @Override
    public Collection<KeyBinding> get() {
      String componentName = componentComboBoxModel.getSelectedItem();
      if (componentName == null) {
        return emptyList();
      }
      String componentClassName = className(componentName);
      try {
        JComponent component = (JComponent) Class.forName(componentClassName).getDeclaredConstructor().newInstance();
        ActionMap actionMap = component.getActionMap();
        Object[] allKeys = actionMap.allKeys();
        if (allKeys == null) {
          return emptyList();
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

  private static final class KeyBindingValueProvider implements ColumnValueProvider<KeyBinding, Integer> {

    @Override
    public Object value(KeyBinding keyBinding, Integer columnIdentifier) {
      switch (columnIdentifier) {
        case ACTION_COLUMN_INDEX:
          return keyBinding.action;
        case WHEN_FOCUSED_COLUMN_INDEX:
          return keyBinding.whenFocused;
        case WHEN_IN_FOCUSED_WINDOW_COLUMN_INDEX:
          return keyBinding.whenInFocusedWindow;
        case WHEN_ANCESTOR_COLUMN_INDEX:
          return keyBinding.whenAncestor;
        default:
          throw new IllegalArgumentException("Unknown identifier: " + columnIdentifier);
      }
    }
  }

  private static FilteredComboBoxModel<String> createComponentComboBoxModel(
          FilteredComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelComboBoxModel) {
    FilteredComboBoxModel<String> comboBoxModel = new FilteredComboBoxModel<>();
    comboBoxModel.refresher().itemSupplier().set(new ComponentItemSupplier(lookAndFeelComboBoxModel));

    return comboBoxModel;
  }

  private static final class ComponentItemSupplier implements Supplier<Collection<String>> {

    private final FilteredComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelComboBoxModel;

    private ComponentItemSupplier(FilteredComboBoxModel<Item<LookAndFeelProvider>> lookAndFeelComboBoxModel) {
      this.lookAndFeelComboBoxModel = lookAndFeelComboBoxModel;
    }

    @Override
    public Collection<String> get() {
      Item<LookAndFeelProvider> selectedItem = lookAndFeelComboBoxModel.getSelectedItem();
      if (selectedItem == null) {
        return emptyList();
      }

      LookAndFeelProvider lookAndFeelProvider = selectedItem.get();
      try {
        return lookAndFeelProvider.lookAndFeel().getDefaults().keySet().stream()
                .map(Object::toString)
                .map(ComponentItemSupplier::componentName)
                .filter(Optional::isPresent)
                .map(Optional::get)
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
