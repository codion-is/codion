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
package is.codion.demos.chinook.ui;

import is.codion.common.Operator;
import is.codion.common.item.Item;
import is.codion.common.model.condition.ConditionModel;
import is.codion.demos.chinook.domain.api.Chinook.Invoice;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ConditionPanel;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.FilterTableConditionPanel;
import is.codion.swing.common.ui.component.table.TableConditionPanel;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.component.EntitySearchField;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.ADVANCED;
import static is.codion.swing.common.ui.component.table.FilterTableConditionPanel.filterTableConditionPanel;
import static is.codion.swing.common.ui.control.Control.command;
import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.border.TitledBorder.CENTER;
import static javax.swing.border.TitledBorder.DEFAULT_POSITION;

final class InvoiceConditionPanel extends TableConditionPanel<Attribute<?>> {

	private static final ResourceBundle BUNDLE = getBundle(InvoiceConditionPanel.class.getName());

	private final FilterTableConditionPanel<Attribute<?>> advancedConditionPanel;
	private final SimpleConditionPanel simpleConditionPanel;

	InvoiceConditionPanel(SwingEntityTableModel tableModel,
												Map<Attribute<?>, ConditionPanel<?>> conditionPanels,
												FilterTableColumnModel<Attribute<?>> columnModel,
												Consumer<TableConditionPanel<Attribute<?>>> onPanelInitialized) {
		super(tableModel.queryModel().condition().conditionModel(),
						attribute -> columnModel.column(attribute).getHeaderValue().toString());
		setLayout(new BorderLayout());
		tableModel.queryModel().condition().persist().add(Invoice.DATE);
		this.simpleConditionPanel = new SimpleConditionPanel(tableModel);
		this.advancedConditionPanel = filterTableConditionPanel(tableModel.queryModel().condition().conditionModel(),
						conditionPanels, columnModel, onPanelInitialized);
		view().link(advancedConditionPanel.view());
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(advancedConditionPanel);
	}

	@Override
	public Map<Attribute<?>, ConditionPanel<?>> panels() {
		Map<Attribute<?>, ConditionPanel<?>> conditionPanels =
						new HashMap<>(advancedConditionPanel.panels());
		conditionPanels.putAll(simpleConditionPanel.panels());

		return conditionPanels;
	}

	@Override
	public Map<Attribute<?>, ConditionPanel<?>> selectable() {
		return view().isEqualTo(ADVANCED) ? advancedConditionPanel.selectable() : simpleConditionPanel.panels();
	}

	@Override
	public ConditionPanel<?> panel(Attribute<?> attribute) {
		if (view().isNotEqualTo(ADVANCED)) {
			return simpleConditionPanel.panel(attribute);
		}

		return advancedConditionPanel.panel(attribute);
	}

	@Override
	public Controls controls() {
		return advancedConditionPanel.controls();
	}

	@Override
	protected void onViewChanged(ConditionView conditionView) {
		removeAll();
		switch (conditionView) {
			case SIMPLE:
				add(simpleConditionPanel, BorderLayout.CENTER);
				simpleConditionPanel.activate();
				break;
			case ADVANCED:
				add(advancedConditionPanel, BorderLayout.CENTER);
				if (simpleConditionPanel.customerConditionPanel.isFocused()) {
					advancedConditionPanel.panel(Invoice.CUSTOMER_FK).requestInputFocus();
				}
				else if (simpleConditionPanel.dateConditionPanel.isFocused()) {
					advancedConditionPanel.panel(Invoice.DATE).requestInputFocus();
				}
				break;
			default:
				break;
		}
		revalidate();
	}

	private static final class SimpleConditionPanel extends JPanel {

		private final Map<Attribute<?>, ConditionPanel<?>> conditionPanels = new HashMap<>();
		private final CustomerConditionPanel customerConditionPanel;
		private final DateConditionPanel dateConditionPanel;

		private SimpleConditionPanel(SwingEntityTableModel tableModel) {
			super(new BorderLayout());
			setBorder(createEmptyBorder(5, 5, 5, 5));
			EntityTableConditionModel entityConditionModel = tableModel.queryModel().condition();
			ForeignKeyConditionModel customerConditionModel = entityConditionModel.get(Invoice.CUSTOMER_FK);
			customerConditionPanel = new CustomerConditionPanel(customerConditionModel, tableModel.entityDefinition());
			dateConditionPanel = new DateConditionPanel(entityConditionModel.get(Invoice.DATE));
			dateConditionPanel.yearValue.addListener(tableModel.items()::refresh);
			dateConditionPanel.monthValue.addListener(tableModel.items()::refresh);
			conditionPanels.put(Invoice.CUSTOMER_FK, customerConditionPanel);
			conditionPanels.put(Invoice.DATE, dateConditionPanel);
			initializeUI();
		}

		private void initializeUI() {
			add(borderLayoutPanel()
							.westComponent(borderLayoutPanel()
											.westComponent(customerConditionPanel)
											.centerComponent(dateConditionPanel)
											.build())
							.build(), BorderLayout.CENTER);
		}

		private Map<Attribute<?>, ConditionPanel<?>> panels() {
			return conditionPanels;
		}

		private ConditionPanel<?> panel(Attribute<?> attribute) {
			requireNonNull(attribute);
			ConditionPanel<?> conditionPanel = panels().get(attribute);
			if (conditionPanel == null) {
				throw new IllegalStateException("No condition panel available for " + attribute);
			}

			return conditionPanel;
		}

		private void activate() {
			customerConditionPanel.model().operator().set(Operator.IN);
			dateConditionPanel.model().operator().set(Operator.BETWEEN);
			customerConditionPanel.requestInputFocus();
		}

		private static final class CustomerConditionPanel extends ConditionPanel<Entity> {

			private final EntitySearchField searchField;

			private CustomerConditionPanel(ForeignKeyConditionModel conditionModel, EntityDefinition definition) {
				super(conditionModel);
				setLayout(new BorderLayout());
				setBorder(createTitledBorder(createEmptyBorder(), definition.attributes().definition(Invoice.CUSTOMER_FK).caption()));
				conditionModel.operands().in().value().link(conditionModel.operands().equal());
				searchField = EntitySearchField.builder()
								.model(conditionModel.inSearchModel())
								.multiSelection()
								.columns(25)
								.build();
				add(searchField, BorderLayout.CENTER);
			}

			@Override
			public Collection<JComponent> components() {
				return List.of(searchField);
			}

			@Override
			public void requestInputFocus() {
				searchField.requestFocusInWindow();
			}

			@Override
			protected void onViewChanged(ConditionView conditionView) {}

			private boolean isFocused() {
				return searchField.hasFocus();
			}
		}

		private static final class DateConditionPanel extends ConditionPanel<LocalDate> {

			private final ComponentValue<Integer, NumberField<Integer>> yearValue = Components.integerField()
							.value(LocalDate.now().getYear())
							.listener(this::updateCondition)
							.focusable(false)
							.columns(4)
							.horizontalAlignment(SwingConstants.CENTER)
							.buildValue();
			private final ComponentValue<Month, JSpinner> monthValue = Components.<Month>itemSpinner(new MonthSpinnerModel())
							.listener(this::updateCondition)
							.editable(false)
							.columns(3)
							.horizontalAlignment(SwingConstants.LEFT)
							.keyEvent(KeyEvents.builder(KeyEvent.VK_UP)
											.modifiers(InputEvent.CTRL_DOWN_MASK)
											.action(command(this::incrementYear)))
							.keyEvent(KeyEvents.builder(KeyEvent.VK_DOWN)
											.modifiers(InputEvent.CTRL_DOWN_MASK)
											.action(command(this::decrementYear)))
							.buildValue();

			private DateConditionPanel(ConditionModel<LocalDate> conditionModel) {
				super(conditionModel);
				model().operator().set(Operator.BETWEEN);
				updateCondition();
				initializeUI();
			}

			@Override
			protected void onViewChanged(ConditionView conditionView) {}

			private void initializeUI() {
				setLayout(new BorderLayout());
				add(flexibleGridLayoutPanel(1, 2)
								.add(borderLayoutPanel()
												.centerComponent(yearValue.component())
												.border(createTitledBorder(createEmptyBorder(),
																BUNDLE.getString("year"), CENTER, DEFAULT_POSITION))
												.build())
								.add(borderLayoutPanel()
												.centerComponent(monthValue.component())
												.border(createTitledBorder(createEmptyBorder(),
																BUNDLE.getString("month")))
												.build())
								.build(), BorderLayout.CENTER);
			}

			private void incrementYear() {
				yearValue.map(year -> year + 1);
			}

			private void decrementYear() {
				yearValue.map(year -> year - 1);
			}

			@Override
			public Collection<JComponent> components() {
				return List.of(yearValue.component(), monthValue.component());
			}

			@Override
			public void requestInputFocus() {
				monthValue.component().requestFocusInWindow();
			}

			private void updateCondition() {
				model().operands().lower().set(lower());
				model().operands().upper().set(upper());
			}

			private LocalDate lower() {
				int year = yearValue.optional().orElse(LocalDate.now().getYear());
				Month month = monthValue.optional().orElse(JANUARY);

				return LocalDate.of(year, month, 1);
			}

			private LocalDate upper() {
				int year = yearValue.optional().orElse(LocalDate.now().getYear());
				Month month = monthValue.optional().orElse(DECEMBER);
				YearMonth yearMonth = YearMonth.of(year, month);

				return LocalDate.of(year, month, yearMonth.lengthOfMonth());
			}

			private boolean isFocused() {
				return yearValue.component().hasFocus() || monthValue.component().hasFocus();
			}

			private static final class MonthSpinnerModel extends SpinnerListModel {

				private MonthSpinnerModel() {
					super(createMonthsList());
				}

				private static List<Item<Month>> createMonthsList() {
					return Stream.concat(Stream.of(Item.<Month>item(null, "")), Arrays.stream(Month.values())
													.map(month -> Item.item(month, month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))))
									.toList();
				}
			}
		}
	}
}
