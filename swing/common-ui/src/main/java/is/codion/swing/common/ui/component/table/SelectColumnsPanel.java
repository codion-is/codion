/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
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
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static javax.swing.BorderFactory.createEmptyBorder;

final class SelectColumnsPanel<C> extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(SelectColumnsPanel.class.getName());

  private static final int COLUMNS_SELECTION_PANEL_HEIGHT = 250;
  private static final int COLUMN_SCROLL_BAR_UNIT_INCREMENT = 16;

  private final FilteredTableColumnModel<C> columnModel;
  private final Map<TableColumn, State> states;
  private final List<JCheckBox> checkBoxes;

  SelectColumnsPanel(FilteredTableColumnModel<C> columnModel) {
    super(new BorderLayout());
    this.columnModel = columnModel;
    this.states = createStateMap();
    this.checkBoxes = states.entrySet().stream()
            .map(entry -> Components.checkBox(entry.getValue())
                    .caption(Objects.toString(entry.getKey().getHeaderValue()))
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
    columnModel.getVisibleColumns().forEach(tableColumn -> {
      if (!states.get(tableColumn).get()) {
        columnModel.setColumnVisible((C) tableColumn.getIdentifier(), false);
      }
    });
    new ArrayList<>(columnModel.getHiddenColumns()).forEach(tableColumn -> {
      if (states.get(tableColumn).get()) {
        columnModel.setColumnVisible((C) tableColumn.getIdentifier(), true);
      }
    });
  }

  private Map<TableColumn, State> createStateMap() {
    Map<TableColumn, State> stateMap = new LinkedHashMap<>();
    columnModel.getAllColumns().stream()
            .sorted(new FilteredTable.ColumnComparator())
            .forEach(column -> stateMap.put(column,
                    State.state(columnModel.isColumnVisible((C) column.getIdentifier()))));

    return stateMap;
  }

  private JPanel createNorthPanel(Insets insets) {
    JCheckBox selectAllButton = Components.checkBox()
            .linkedValueObserver(State.and(states.values()))
            .caption(MESSAGES.getString("select_all"))
            .mnemonic(MESSAGES.getString("select_all_mnemonic").charAt(0))
            .build();
    JCheckBox selectNoneButton = Components.checkBox()
            .linkedValueObserver(State.and(states.values().stream()
                    .map(StateObserver::getReversedObserver)
                    .collect(Collectors.toList())))
            .caption(MESSAGES.getString("select_none"))
            .mnemonic(MESSAGES.getString("select_none_mnemonic").charAt(0))
            .build();
    selectAllButton.addActionListener(new SelectAll(selectAllButton, selectNoneButton));
    selectNoneButton.addActionListener(new SelectNone(selectAllButton, selectNoneButton));

    return Components.panel(gridLayout(2, 1))
            .addAll(selectAllButton, selectNoneButton)
            .border(createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right))
            .build();
  }

  private void selectAll() {
    states.values().forEach(state -> state.set(true));
  }

  private void selectNone() {
    states.values().forEach(state -> state.set(false));
  }

  private JScrollPane createCheckBoxPanel() {
    JPanel northPanel = Components.panel(gridLayout(0, 1))
            .addAll(checkBoxes)
            .build();
    KeyEvents.Builder upEventBuilder = KeyEvents.builder(KeyEvent.VK_UP)
            .condition(JComponent.WHEN_FOCUSED)
            .action(control(new TransferFocusCommand(checkBoxes, false)));
    KeyEvents.Builder downEventBuilder = KeyEvents.builder(KeyEvent.VK_DOWN)
            .condition(JComponent.WHEN_FOCUSED)
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

    return Components.panel(borderLayout())
            .add(northPanel, BorderLayout.NORTH)
            .scrollPane()
            .preferredHeight(COLUMNS_SELECTION_PANEL_HEIGHT)
            .verticalUnitIncrement(COLUMN_SCROLL_BAR_UNIT_INCREMENT)
            .build();
  }

  private final class SelectAll implements ActionListener {

    private final JCheckBox selectAllButton;
    private final JCheckBox selectNoneButton;

    private SelectAll(JCheckBox selectAllButton, JCheckBox selectNoneButton) {
      this.selectAllButton = selectAllButton;
      this.selectNoneButton = selectNoneButton;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (selectAllButton.isSelected()) {
        selectAll();
      }
      else {
        selectNone();
        selectNoneButton.setSelected(true);
      }
    }
  }

  private final class SelectNone implements ActionListener {

    private final JCheckBox selectAllButton;
    private final JCheckBox selectNoneButton;

    private SelectNone(JCheckBox selectAllButton, JCheckBox selectNoneButton) {
      this.selectAllButton = selectAllButton;
      this.selectNoneButton = selectNoneButton;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (selectNoneButton.isSelected()) {
        selectNone();
      }
      else {
        selectAll();
        selectAllButton.setSelected(true);
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
    public void perform() throws Exception {
      checkBoxes.stream()
              .filter(Component::isFocusOwner)
              .findAny()
              .ifPresent(checkBox -> checkBoxes.get(next ?
                              getNextIndex(checkBoxes.indexOf(checkBox)) :
                              getPreviousIndex(checkBoxes.indexOf(checkBox)))
                      .requestFocusInWindow());
    }

    private int getNextIndex(int currentIndex) {
      return currentIndex == checkBoxes.size() - 1 ? 0 : currentIndex + 1;
    }

    private int getPreviousIndex(int currentIndex) {
      return currentIndex == 0 ? checkBoxes.size() - 1 : currentIndex - 1;
    }
  }
}
