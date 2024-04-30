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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import java.util.stream.Collectors;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.control;
import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_UP;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEmptyBorder;

final class ColumnSelectionPanel<C> extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(ColumnSelectionPanel.class, getBundle(ColumnSelectionPanel.class.getName()));

	private static final int COLUMNS_SELECTION_PANEL_HEIGHT = 250;
	private static final int COLUMN_SCROLL_BAR_UNIT_INCREMENT = 16;

	private final FilteredTableColumnModel<C> columnModel;
	private final Map<FilteredTableColumn<C>, State> visibleStates;
	private final List<JCheckBox> checkBoxes;

	ColumnSelectionPanel(FilteredTableColumnModel<C> columnModel) {
		super(new BorderLayout());
		this.columnModel = columnModel;
		this.visibleStates = createVisibleStates();
		this.checkBoxes = visibleStates.entrySet().stream()
						.map(entry -> checkBox(entry.getValue())
										.text(String.valueOf(entry.getKey().getHeaderValue()))
										.toolTipText(entry.getKey().toolTipText())
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
		columnModel.visible().forEach(tableColumn -> {
			if (!visibleStates.get(tableColumn).get()) {
				columnModel.visible(tableColumn.getIdentifier()).set(false);
			}
		});
		new ArrayList<>(columnModel.hidden()).forEach(tableColumn -> {
			if (visibleStates.get(tableColumn).get()) {
				columnModel.visible(tableColumn.getIdentifier()).set(true);
			}
		});
	}

	private Map<FilteredTableColumn<C>, State> createVisibleStates() {
		Map<FilteredTableColumn<C>, State> states = new LinkedHashMap<>();
		columnModel.columns().stream()
						.sorted(new FilteredTable.ColumnComparator())
						.forEach(column -> states.put(column, State.state(columnModel.visible(column.getIdentifier()).get())));

		return states;
	}

	private JPanel createNorthPanel(Insets insets) {
		JCheckBox selectAllBox = checkBox()
						.link(State.and(visibleStates.values()))
						.text(MESSAGES.getString("select_all"))
						.mnemonic(MESSAGES.getString("select_all_mnemonic").charAt(0))
						.build();
		JCheckBox selectNoneBox = checkBox()
						.link(State.and(visibleStates.values().stream()
										.map(StateObserver::not)
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
						.enable(selectAllBox, selectNoneBox);
		KeyEvents.builder(VK_DOWN)
						.condition(WHEN_FOCUSED)
						.action(control(new TransferFocusCommand(selectCheckBoxes, true)))
						.enable(selectAllBox, selectNoneBox);

		return gridLayoutPanel(2, 1)
						.addAll(selectAllBox, selectNoneBox)
						.border(createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right))
						.build();
	}

	private void selectAll() {
		visibleStates.values().forEach(state -> state.set(true));
	}

	private void selectNone() {
		visibleStates.values().forEach(state -> state.set(false));
	}

	private JScrollPane createCheckBoxPanel() {
		JPanel northPanel = gridLayoutPanel(0, 1)
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

		return borderLayoutPanel()
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
		public void execute() {
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
