/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_UP;
import static javax.swing.BorderFactory.createEmptyBorder;

final class ColumnSelectionPanel<C> extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ColumnSelectionPanel.class.getName());

  private static final int COLUMNS_SELECTION_PANEL_HEIGHT = 250;
  private static final int COLUMN_SCROLL_BAR_UNIT_INCREMENT = 16;

  private final FilteredTableColumnModel<C> columnModel;
  private final Map<TableColumn, State> columnVisibleStates;
  private final List<JCheckBox> checkBoxes;

  ColumnSelectionPanel(FilteredTableColumnModel<C> columnModel) {
    super(new BorderLayout());
    this.columnModel = columnModel;
    this.columnVisibleStates = createColumnVisibleStates();
    this.checkBoxes = columnVisibleStates.entrySet().stream()
            .map(entry -> Components.checkBox(entry.getValue())
                    .text(Objects.toString(entry.getKey().getHeaderValue()))
                    .build())
            .collect(Collectors.toList());
    JScrollPane checkBoxPanel = createCheckBoxPanel();
    add(createNorthPanel(checkBoxPanel.getBorder().getBorderInsets(checkBoxPanel)), BorderLayout.NORTH);
    add(checkBoxPanel, BorderLayout.CENTER);
  }

  void requestColumnPanelFocus() {
    if (!checkBoxes.isEmpty()) {
      checkBoxes.get(0).requestFocusInWindow();
    }
  }

  void applyChanges() {
    columnModel.visibleColumns().forEach(tableColumn -> {
      if (!columnVisibleStates.get(tableColumn).get()) {
        columnModel.setColumnVisible(tableColumn.getIdentifier(), false);
      }
    });
    new ArrayList<>(columnModel.hiddenColumns()).forEach(tableColumn -> {
      if (columnVisibleStates.get(tableColumn).get()) {
        columnModel.setColumnVisible(tableColumn.getIdentifier(), true);
      }
    });
  }

  private Map<TableColumn, State> createColumnVisibleStates() {
    Map<TableColumn, State> stateMap = new LinkedHashMap<>();
    columnModel.columns().stream()
            .sorted(new FilteredTable.ColumnComparator())
            .forEach(column -> stateMap.put(column,
                    State.state(columnModel.isColumnVisible(column.getIdentifier()))));

    return stateMap;
  }

  private JPanel createNorthPanel(Insets insets) {
    JCheckBox selectAllBox = Components.checkBox()
            .linkedValueObserver(State.and(columnVisibleStates.values()))
            .text(MESSAGES.getString("select_all"))
            .mnemonic(MESSAGES.getString("select_all_mnemonic").charAt(0))
            .build();
    JCheckBox selectNoneBox = Components.checkBox()
            .linkedValueObserver(State.and(columnVisibleStates.values().stream()
                    .map(StateObserver::reversedObserver)
                    .collect(Collectors.toList())))
            .text(MESSAGES.getString("select_none"))
            .mnemonic(MESSAGES.getString("select_none_mnemonic").charAt(0))
            .build();
    selectAllBox.addActionListener(new SelectAll(selectAllBox, selectNoneBox));
    selectNoneBox.addActionListener(new SelectNone(selectAllBox, selectNoneBox));

    List<JCheckBox> selectCheckBoxes = Arrays.asList(selectAllBox, selectNoneBox);
    KeyEvents.builder(VK_UP)
            .condition(WHEN_FOCUSED)
            .action(control(new TransferFocusCommand(selectCheckBoxes, false)))
            .enable(selectAllBox)
            .enable(selectNoneBox);
    KeyEvents.builder(VK_DOWN)
            .condition(WHEN_FOCUSED)
            .action(control(new TransferFocusCommand(selectCheckBoxes, true)))
            .enable(selectAllBox)
            .enable(selectNoneBox);

    return Components.panel(gridLayout(2, 1))
            .addAll(selectAllBox, selectNoneBox)
            .border(createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right))
            .build();
  }

  private void selectAll() {
    columnVisibleStates.values().forEach(state -> state.set(true));
  }

  private void selectNone() {
    columnVisibleStates.values().forEach(state -> state.set(false));
  }

  private JScrollPane createCheckBoxPanel() {
    JPanel northPanel = Components.panel(gridLayout(0, 1))
            .addAll(checkBoxes)
            .build();
    KeyEvents.Builder upEventBuilder = KeyEvents.builder(VK_UP)
            .condition(WHEN_FOCUSED)
            .action(control(new TransferFocusCommand(checkBoxes, false)));
    KeyEvents.Builder downEventBuilder = KeyEvents.builder(VK_DOWN)
            .condition(WHEN_FOCUSED)
            .action(control(new TransferFocusCommand(checkBoxes, true)));
    checkBoxes.forEach(checkBox -> {
      upEventBuilder.enable(checkBox);
      downEventBuilder.enable(checkBox);
      checkBox.addFocusListener(new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
          northPanel.scrollRectToVisible(checkBox.getBounds());
        }
      });
    });

    return Components.borderLayoutPanel()
            .northComponent(northPanel)
            .scrollPane()
            .preferredHeight(COLUMNS_SELECTION_PANEL_HEIGHT)
            .verticalUnitIncrement(COLUMN_SCROLL_BAR_UNIT_INCREMENT)
            .build();
  }

  private final class SelectAll implements ActionListener {

    private final JCheckBox selectAllBox;
    private final JCheckBox selectNoneBox;

    private SelectAll(JCheckBox selectAllBox, JCheckBox selectNoneBox) {
      this.selectAllBox = selectAllBox;
      this.selectNoneBox = selectNoneBox;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (selectAllBox.isSelected()) {
        selectAll();
      }
      else {
        selectNone();
        selectNoneBox.setSelected(true);
      }
    }
  }

  private final class SelectNone implements ActionListener {

    private final JCheckBox selectAllBox;
    private final JCheckBox selectNoneBox;

    private SelectNone(JCheckBox selectAllBox, JCheckBox selectNoneBox) {
      this.selectAllBox = selectAllBox;
      this.selectNoneBox = selectNoneBox;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (selectNoneBox.isSelected()) {
        selectNone();
      }
      else {
        selectAll();
        selectAllBox.setSelected(true);
      }
    }
  }

  private static final class TransferFocusCommand implements Control.Command {

    private final List<JCheckBox> checkBoxes;
    private final boolean next;

    private TransferFocusCommand(List<JCheckBox> checkBoxes, boolean next) {
      this.next = next;
      this.checkBoxes = checkBoxes;
    }

    @Override
    public void perform() {
      checkBoxes.stream()
              .filter(Component::isFocusOwner)
              .findAny()
              .ifPresent(checkBox -> checkBoxes.get(next ?
                              nextIndex(checkBoxes.indexOf(checkBox)) :
                              previousIndex(checkBoxes.indexOf(checkBox)))
                      .requestFocusInWindow());
    }

    private int nextIndex(int currentIndex) {
      return currentIndex == checkBoxes.size() - 1 ? 0 : currentIndex + 1;
    }

    private int previousIndex(int currentIndex) {
      return currentIndex == 0 ? checkBoxes.size() - 1 : currentIndex - 1;
    }
  }
}
