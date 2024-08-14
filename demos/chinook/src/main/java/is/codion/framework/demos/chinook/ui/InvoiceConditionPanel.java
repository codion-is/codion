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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.Operator;
import is.codion.common.event.EventObserver;
import is.codion.common.item.Item;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState;
import is.codion.swing.common.ui.component.table.FilterColumnConditionPanel;
import is.codion.swing.common.ui.component.table.FilterColumnConditionPanel.FieldFactory;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.FilterTableConditionPanel;
import is.codion.swing.common.ui.component.table.TableConditionPanel;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.ui.EntityConditionFieldFactory;
import is.codion.swing.framework.ui.component.EntitySearchField;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState.ADVANCED;
import static is.codion.swing.common.ui.component.table.FilterColumnConditionPanel.filterColumnConditionPanel;
import static is.codion.swing.common.ui.component.table.FilterTableConditionPanel.filterTableConditionPanel;
import static is.codion.swing.common.ui.control.Control.commandControl;
import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.border.TitledBorder.CENTER;
import static javax.swing.border.TitledBorder.DEFAULT_POSITION;

final class InvoiceConditionPanel extends TableConditionPanel<Attribute<?>> {

	private static final ResourceBundle BUNDLE = getBundle(InvoiceConditionPanel.class.getName());

	private final FilterTableColumnModel<Attribute<?>> columnModel;
	private final FilterTableConditionPanel<Attribute<?>> advancedConditionPanel;
	private final SimpleConditionPanel simpleConditionPanel;

	InvoiceConditionPanel(EntityDefinition invoiceDefinition,
												TableConditionModel<Attribute<?>> conditionModel,
												FilterTableColumnModel<Attribute<?>> columnModel,
												Runnable onDateChanged) {
		super(conditionModel);
		setLayout(new BorderLayout());
		this.columnModel = columnModel;
		this.simpleConditionPanel = new SimpleConditionPanel(conditionModel, invoiceDefinition, onDateChanged);
		this.advancedConditionPanel = filterTableConditionPanel(conditionModel,
						createConditionPanels(new EntityConditionFieldFactory(invoiceDefinition)), columnModel);
		state().link(advancedConditionPanel.state());
	}

	@Override
	public Collection<ColumnConditionPanel<Attribute<?>, ?>> conditionPanels() {
		Collection<ColumnConditionPanel<Attribute<?>, ?>> conditionPanels =
						new ArrayList<>(advancedConditionPanel.conditionPanels());
		conditionPanels.addAll(simpleConditionPanel.conditionPanels());

		return conditionPanels;
	}

	@Override
	public Collection<ColumnConditionPanel<Attribute<?>, ?>> selectableConditionPanels() {
		return state().isEqualTo(ADVANCED) ? advancedConditionPanel.selectableConditionPanels() : simpleConditionPanel.conditionPanels();
	}

	@Override
	public <T extends ColumnConditionPanel<Attribute<?>, ?>> T conditionPanel(Attribute<?> attribute) {
		if (state().isNotEqualTo(ADVANCED)) {
			return (T) simpleConditionPanel.conditionPanels().stream()
							.filter(panel -> panel.conditionModel().identifier().equals(attribute))
							.findFirst()
							.orElseThrow(IllegalArgumentException::new);
		}

		return (T) advancedConditionPanel.conditionPanels().stream()
						.filter(panel -> panel.conditionModel().identifier().equals(attribute))
						.findFirst()
						.orElseThrow(IllegalArgumentException::new);
	}

	@Override
	public Controls controls() {
		return advancedConditionPanel.controls();
	}

	@Override
	public Optional<EventObserver<?>> initializedEvent() {
		return advancedConditionPanel.initializedEvent();
	}

	@Override
	protected void onStateChanged(ConditionState conditionState) {
		removeAll();
		switch (conditionState) {
			case SIMPLE:
				add(simpleConditionPanel, BorderLayout.CENTER);
				simpleConditionPanel.activate();
				break;
			case ADVANCED:
				add(advancedConditionPanel, BorderLayout.CENTER);
				if (simpleConditionPanel.customerConditionPanel.hasInputFocus()) {
					advancedConditionPanel.conditionPanel(Invoice.CUSTOMER_FK).requestInputFocus();
				}
				else if (simpleConditionPanel.dateConditionPanel.hasInputFocus()) {
					advancedConditionPanel.conditionPanel(Invoice.DATE).requestInputFocus();
				}
				break;
			default:
				break;
		}
		revalidate();
	}

	private Collection<ColumnConditionPanel<Attribute<?>, ?>> createConditionPanels(FieldFactory<Attribute<?>> fieldFactory) {
		return conditionModel().conditionModels().values().stream()
						.filter(conditionModel -> columnModel.containsColumn(conditionModel.identifier()))
						.filter(conditionModel -> fieldFactory.supportsType(conditionModel.columnClass()))
						.map(conditionModel -> createPanel(conditionModel, fieldFactory))
						.collect(toList());
	}

	private FilterColumnConditionPanel<Attribute<?>, ?> createPanel(ColumnConditionModel<Attribute<?>, ?> conditionModel,
																																	FieldFactory<Attribute<?>> fieldFactory) {
		return filterColumnConditionPanel(conditionModel,
						Objects.toString(columnModel.column(conditionModel.identifier()).getHeaderValue()), fieldFactory);
	}

	private static final class SimpleConditionPanel extends JPanel {

		private final CustomerConditionPanel customerConditionPanel;
		private final DateConditionPanel dateConditionPanel;

		private SimpleConditionPanel(TableConditionModel<Attribute<?>> conditionModel,
																 EntityDefinition invoiceDefinition, Runnable onDateChanged) {
			super(new BorderLayout());
			setBorder(createEmptyBorder(5, 5, 5, 5));
			customerConditionPanel = new CustomerConditionPanel(conditionModel.conditionModel(Invoice.CUSTOMER_FK), invoiceDefinition);
			dateConditionPanel = new DateConditionPanel(conditionModel.conditionModel(Invoice.DATE), invoiceDefinition);
			dateConditionPanel.yearValue.addListener(onDateChanged);
			dateConditionPanel.monthValue.addListener(onDateChanged);
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

		private Collection<ColumnConditionPanel<Attribute<?>, ?>> conditionPanels() {
			return asList(customerConditionPanel, dateConditionPanel);
		}

		private void activate() {
			customerConditionPanel.conditionModel().operator().set(Operator.IN);
			dateConditionPanel.conditionModel().operator().set(Operator.BETWEEN);
			customerConditionPanel.requestInputFocus();
		}

		private static final class CustomerConditionPanel extends ColumnConditionPanel<Attribute<?>, Entity> {

			private final EntitySearchField searchField;

			private CustomerConditionPanel(ColumnConditionModel<Attribute<?>, Entity> conditionModel, EntityDefinition invoiceDefinition) {
				super(conditionModel, invoiceDefinition.attributes().definition(conditionModel.identifier()).caption());
				setLayout(new BorderLayout());
				setBorder(createTitledBorder(createEmptyBorder(), caption()));
				ForeignKeyConditionModel foreignKeyConditionModel = (ForeignKeyConditionModel) conditionModel;
				foreignKeyConditionModel.inValues().value().link(foreignKeyConditionModel.equalValue());
				searchField = EntitySearchField.builder(foreignKeyConditionModel.inSearchModel())
								.columns(25)
								.build();
				add(searchField, BorderLayout.CENTER);
			}

			@Override
			public Collection<JComponent> components() {
				return singletonList(searchField);
			}

			@Override
			public void requestInputFocus() {
				searchField.requestFocusInWindow();
			}

			@Override
			protected void onStateChanged(ConditionState state) {}

			private boolean hasInputFocus() {
				return searchField.hasFocus();
			}
		}

		private static final class DateConditionPanel extends ColumnConditionPanel<Attribute<?>, LocalDate> {

			private final ComponentValue<Integer, NumberField<Integer>> yearValue = Components.integerField()
							.initialValue(LocalDate.now().getYear())
							.listener(this::updateCondition)
							.focusable(false)
							.buildValue();
			private final ComponentValue<Month, JSpinner> monthValue = Components.<Month>itemSpinner(new MonthSpinnerModel())
							.listener(this::updateCondition)
							.editable(false)
							.columns(3)
							.keyEvent(KeyEvents.builder(KeyEvent.VK_UP)
											.modifiers(InputEvent.CTRL_DOWN_MASK)
											.action(commandControl(this::incrementYear)))
							.keyEvent(KeyEvents.builder(KeyEvent.VK_DOWN)
											.modifiers(InputEvent.CTRL_DOWN_MASK)
											.action(commandControl(this::decrementYear)))
							.buildValue();

			private DateConditionPanel(ColumnConditionModel<Attribute<?>, LocalDate> conditionModel, EntityDefinition invoiceDefinition) {
				super(conditionModel, invoiceDefinition.attributes().definition(conditionModel.identifier()).caption());
				setLayout(new BorderLayout());
				conditionModel().operator().set(Operator.BETWEEN);
				updateCondition();
				initializeUI();
			}

			@Override
			protected void onStateChanged(ConditionState state) {}

			private void initializeUI() {
				setLayout(new BorderLayout());
				add(gridLayoutPanel(1, 2)
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
				return asList(yearValue.component(), monthValue.component());
			}

			@Override
			public void requestInputFocus() {
				monthValue.component().requestFocusInWindow();
			}

			private void updateCondition() {
				conditionModel().setLowerBound(lowerBound());
				conditionModel().setUpperBound(upperBound());
			}

			private LocalDate lowerBound() {
				int year = yearValue.optional().orElse(LocalDate.now().getYear());
				Month month = monthValue.optional().orElse(JANUARY);

				return LocalDate.of(year, month, 1);
			}

			private LocalDate upperBound() {
				int year = yearValue.optional().orElse(LocalDate.now().getYear());
				Month month = monthValue.optional().orElse(DECEMBER);
				YearMonth yearMonth = YearMonth.of(year, month);

				return LocalDate.of(year, month, yearMonth.lengthOfMonth());
			}

			private boolean hasInputFocus() {
				return yearValue.component().hasFocus() || monthValue.component().hasFocus();
			}

			private static final class MonthSpinnerModel extends SpinnerListModel {

				private MonthSpinnerModel() {
					super(createMonthsList());
				}

				private static List<Item<Month>> createMonthsList() {
					List<Item<Month>> months = Arrays.stream(Month.values())
									.map(month -> Item.item(month, month.getDisplayName(TextStyle.SHORT, Locale.getDefault())))
									.collect(toList());
					months.add(0, Item.item(null, ""));
					Collections.reverse(months);

					return months;
				}
			}
		}
	}
}
