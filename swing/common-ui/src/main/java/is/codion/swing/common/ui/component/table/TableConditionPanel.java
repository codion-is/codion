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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Text;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static is.codion.common.item.Item.item;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;

/**
 * An abstract base class for a UI component based on a {@link TableConditionModel}.
 * @param <C> the type used to identify the table columns
 */
public abstract class TableConditionPanel<C> extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(TableConditionPanel.class, getBundle(TableConditionPanel.class.getName()));

	private final TableConditionModel<C> tableConditionModel;
	private final Function<C, String> captions;
	private final Value<ConditionView> view = Value.builder()
					.nonNull(HIDDEN)
					.consumer(this::onViewChanged)
					.build();
	private final State hiddenView = State.state(true);
	private final State simpleView = State.state();
	private final State advancedView = State.state();

	/**
	 * Instantiates a new {@link TableConditionPanel}
	 * @param tableConditionModel the {@link TableConditionModel}
	 * @param captions provides captions based on the column identifiers used when presenting conditions for selection
	 * @see #select(JComponent)
	 */
	protected TableConditionPanel(TableConditionModel<C> tableConditionModel, Function<C, String> captions) {
		this.tableConditionModel = requireNonNull(tableConditionModel);
		this.captions = requireNonNull(captions);
		configureStates();
	}

	/**
	 * @return the {@link Value} controlling the {@link ConditionView}
	 */
	public final Value<ConditionView> view() {
		return view;
	}

	/**
	 * @return the condition panels mapped to their respective identifier
	 */
	public abstract Map<C, ConditionPanel<?>> panels();

	/**
	 * By default, this returns all condition panels, override to customize.
	 * @return the selectable condition panels
	 * @see #panels()
	 * @see #select(JComponent)
	 */
	public Map<C, ConditionPanel<?>> selectable() {
		return panels();
	}

	/**
	 * @param identifier the identifier for which to retrieve the {@link ConditionPanel}
	 * @return the {@link ConditionPanel} associated with the given identifier
	 * @throws IllegalArgumentException in case no panel is available for the given identifier
	 */
	public ConditionPanel<?> panel(C identifier) {
		requireNonNull(identifier);
		ConditionPanel<?> conditionPanel = panels().get(identifier);
		if (conditionPanel == null) {
			throw new IllegalArgumentException("No condition panel available for " + identifier);
		}

		return conditionPanel;
	}

	/**
	 * @return the controls provided by this condition panel, for example clearing the condition and changing the condition view
	 */
	public Controls controls() {
		return Controls.builder()
						.control(Control.builder()
										.toggle(hiddenView)
										.caption(MESSAGES.getString("hidden")))
						.control(Control.builder()
										.toggle(simpleView)
										.caption(MESSAGES.getString("simple")))
						.control(Control.builder()
										.toggle(advancedView)
										.caption(MESSAGES.getString("advanced")))
						.separator()
						.control(Control.builder()
										.command(this::clear)
										.caption(Messages.clear()))
						.build();
	}

	/**
	 * Selects one of the selectable condition panels to receive the input focus.
	 * If only one panel is selectable, that one receives the input focus automatically.
	 * If multiple condition panels are selectable, a selection dialog is presented.
	 * @param dialogOwner the selection dialog owner
	 * @see #selectable()
	 */
	public final void select(JComponent dialogOwner) {
		List<Item<? extends ConditionPanel<?>>> panelItems = selectable().entrySet().stream()
						.map(entry -> item(entry.getValue(), captions.apply(entry.getKey())))
						.sorted(Text.collator())
						.collect(toList());
		if (panelItems.size() == 1) {
			view().map(conditionView -> conditionView == HIDDEN ? SIMPLE : conditionView);
			panelItems.get(0).value().requestInputFocus();
		}
		else if (!panelItems.isEmpty()) {
			Dialogs.selectionDialog(panelItems)
							.owner(dialogOwner)
							.title(MESSAGES.getString("select_condition"))
							.selectSingle()
							.map(Item::value)
							.ifPresent(conditionPanel -> {
								view().map(conditionView -> conditionView == HIDDEN ? SIMPLE : conditionView);
								conditionPanel.requestInputFocus();
							});
		}
	}

	/**
	 * Called each time the condition view changes, override to update this panel according to the state
	 * @param conditionView the new condition view
	 */
	protected void onViewChanged(ConditionView conditionView) {}

	private void configureStates() {
		State.group(hiddenView, simpleView, advancedView);
		hiddenView.addConsumer(new StateConsumer(HIDDEN));
		simpleView.addConsumer(new StateConsumer(SIMPLE));
		advancedView.addConsumer(new StateConsumer(ADVANCED));
		view.addConsumer(conditionView -> {
			hiddenView.set(conditionView == HIDDEN);
			simpleView.set(conditionView == SIMPLE);
			advancedView.set(conditionView == ADVANCED);
		});
	}

	private void clear() {
		tableConditionModel.clear();
	}

	/**
	 * @param <C> the type identifying the table columns
	 */
	public interface Factory<C> {

		/**
		 * @param tableConditionModel the condition model
		 * @param conditionPanels the condition panels
		 * @param columnModel the column model
		 * @param onPanelInitialized called when the panel has been initialized
		 * @return a new {@link TableConditionPanel}
		 */
		TableConditionPanel<C> create(TableConditionModel<C> tableConditionModel,
																	Map<C, ConditionPanel<?>> conditionPanels,
																	FilterTableColumnModel<C> columnModel,
																	Consumer<TableConditionPanel<C>> onPanelInitialized);
	}

	private final class StateConsumer implements Consumer<Boolean> {

		private final ConditionView view;

		private StateConsumer(ConditionView view) {
			this.view = view;
		}

		@Override
		public void accept(Boolean enabled) {
			if (enabled) {
				TableConditionPanel.this.view.set(view);
			}
		}
	}
}
