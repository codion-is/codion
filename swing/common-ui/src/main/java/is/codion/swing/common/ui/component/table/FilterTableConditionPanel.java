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
package is.codion.swing.common.ui.component.table;

import is.codion.common.i18n.Messages;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.state.State;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static is.codion.swing.common.ui.component.table.FilterTableColumnComponentPanel.filterTableColumnComponentPanel;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

/**
 * A default filter table condition panel.
 * @param <C> the column identifier type
 * @see #filterTableConditionPanel(TableConditionModel, List, FilterTableColumnModel)
 */
public final class FilterTableConditionPanel<C> extends JPanel implements TableConditionPanel<C> {

	private final TableConditionModel<C> conditionModel;
	private final Collection<? extends ColumnConditionPanel<? extends C, ?>> conditionPanels;
	private final FilterTableColumnComponentPanel<C> componentPanel;
	private final State advanced = State.builder()
					.consumer(this::onAdvancedChanged)
					.build();

	private FilterTableConditionPanel(TableConditionModel<? extends C> conditionModel,
																		Collection<ColumnConditionPanel<? extends C, ?>> conditionPanels,
																		FilterTableColumnModel<C> columnModel) {
		this.conditionModel = (TableConditionModel<C>) requireNonNull(conditionModel);
		this.conditionPanels = unmodifiableList(new ArrayList<>(requireNonNull(conditionPanels)));
		Map<C, JComponent> collect = conditionPanels.stream()
						.collect(toMap(panel -> panel.conditionModel()
										.columnIdentifier(), JComponent.class::cast));
		this.componentPanel = filterTableColumnComponentPanel(requireNonNull(columnModel), collect);
		setLayout(new BorderLayout());
		add(componentPanel, BorderLayout.CENTER);
	}

	@Override
	public TableConditionModel<C> conditionModel() {
		return conditionModel;
	}

	@Override
	public Collection<? extends ColumnConditionPanel<C, ?>> conditionPanels() {
		return (Collection<? extends ColumnConditionPanel<C, ?>>) conditionPanels;
	}

	@Override
	public <T extends ColumnConditionPanel<C, ?>> Optional<T> conditionPanel(C columnIdentifier) {
		return (Optional<T>) conditionPanels.stream()
						.filter(panel -> panel.conditionModel().columnIdentifier().equals(columnIdentifier))
						.findFirst();
	}

	@Override
	public State advanced() {
		return advanced;
	}

	@Override
	public Controls controls() {
		return Controls.builder()
						.control(ToggleControl.builder(advanced)
										.name(Messages.advanced()))
						.control(Control.builder(this::clearConditions)
										.name(Messages.clear()))
						.build();
	}

	/**
	 * @param <C> the column identifier type
	 * @param conditionModel the condition model
	 * @param conditionPanels the condition panels
	 * @param columnModel the column model
	 * @return a new {@link FilterTableConditionPanel}
	 */
	public static <C> FilterTableConditionPanel<? extends C> filterTableConditionPanel(TableConditionModel<C> conditionModel,
																																										 Collection<ColumnConditionPanel<? extends C, ?>> conditionPanels,
																																										 FilterTableColumnModel<C> columnModel) {
		return new FilterTableConditionPanel<>(conditionModel, conditionPanels, columnModel);
	}

	private void clearConditions() {
		conditionPanels.stream()
						.map(ColumnConditionPanel::conditionModel)
						.forEach(ColumnConditionModel::clear);
	}

	private void onAdvancedChanged(boolean advancedView) {
		conditionPanels.forEach(panel -> panel.advanced().set(advancedView));
	}
}
