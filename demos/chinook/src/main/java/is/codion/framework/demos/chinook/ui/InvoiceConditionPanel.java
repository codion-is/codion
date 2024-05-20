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
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.framework.demos.chinook.domain.Chinook.Invoice;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.DefaultForeignKeyConditionModel;
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
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState.ADVANCED;
import static is.codion.swing.common.ui.component.table.FilterColumnConditionPanel.filterColumnConditionPanel;
import static is.codion.swing.common.ui.component.table.FilterTableConditionPanel.filterTableConditionPanel;
import static is.codion.swing.common.ui.control.Control.control;
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
												FilterTableColumnModel<Attribute<?>> columnModel) {
		super(conditionModel);
		setLayout(new BorderLayout());
		this.columnModel = columnModel;
		this.simpleConditionPanel = new SimpleConditionPanel(conditionModel, invoiceDefinition);
		this.advancedConditionPanel = filterTableConditionPanel(conditionModel,
						createConditionPanels(new EntityConditionFieldFactory(invoiceDefinition)), columnModel);
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
		return state().isEqualTo(ADVANCED) ? super.selectableConditionPanels() : simpleConditionPanel.conditionPanels();
	}

	@Override
	public <T extends ColumnConditionPanel<Attribute<?>, ?>> T conditionPanel(Attribute<?> columnIdentifier) {
		if (state().isNotEqualTo(ADVANCED)) {
			return (T) simpleConditionPanel.conditionPanels().stream()
							.filter(panel -> panel.conditionModel().columnIdentifier().equals(columnIdentifier))
							.findFirst()
							.orElseThrow(IllegalArgumentException::new);
		}

		return (T) advancedConditionPanel.conditionPanels().stream()
						.filter(panel -> panel.conditionModel().columnIdentifier().equals(columnIdentifier))
						.findFirst()
						.orElseThrow(IllegalArgumentException::new);
	}

	@Override
	public Controls controls() {
		return advancedConditionPanel.controls();
	}

	protected void onStateChanged(ConditionState conditionState) {
		advancedConditionPanel.state().set(conditionState);
		removeAll();
		switch (conditionState) {
			case HIDDEN:
				break;
			case SIMPLE:
				add(simpleConditionPanel, BorderLayout.CENTER);
				simpleConditionPanel.customerConditionPanel.requestInputFocus();
				break;
			case ADVANCED:
				add(advancedConditionPanel, BorderLayout.CENTER);
				currentAttribut().ifPresent(attribute ->
								advancedConditionPanel.conditionPanel(attribute).requestInputFocus());
				break;
		}
		revalidate();
	}

	private Optional<Attribute<?>> currentAttribut() {
		if (simpleConditionPanel.customerConditionPanel.hasInputFocus()) {
			return Optional.of(Invoice.CUSTOMER_FK);
		}
		if (simpleConditionPanel.dateConditionPanel.hasInputFocus()) {
			return Optional.of(Invoice.DATE);
		}

		return Optional.empty();
	}

	private Collection<ColumnConditionPanel<Attribute<?>, ?>> createConditionPanels(FieldFactory<Attribute<?>> fieldFactory) {
		return conditionModel().conditionModels().values().stream()
						.filter(conditionModel -> columnModel.containsColumn(conditionModel.columnIdentifier()))
						.filter(conditionModel -> fieldFactory.supportsType(conditionModel.columnClass()))
						.map(conditionModel -> createPanel(conditionModel, fieldFactory))
						.collect(toList());
	}

	private FilterColumnConditionPanel<Attribute<?>, ?> createPanel(ColumnConditionModel<Attribute<?>, ?> conditionModel,
																																	FieldFactory<Attribute<?>> fieldFactory) {
		return filterColumnConditionPanel(conditionModel,
						Objects.toString(columnModel.column(conditionModel.columnIdentifier()).getHeaderValue()), fieldFactory);
	}

	private static final class SimpleConditionPanel extends JPanel {

		private final CustomerConditionPanel customerConditionPanel;
		private final DateConditionPanel dateConditionPanel;

		private SimpleConditionPanel(TableConditionModel<Attribute<?>> conditionModel,
																 EntityDefinition invoiceDefinition) {
			super(new BorderLayout());
			setBorder(createEmptyBorder(5, 5, 5, 5));
			customerConditionPanel = new CustomerConditionPanel(conditionModel.conditionModel(Invoice.CUSTOMER_FK), invoiceDefinition);
			dateConditionPanel = new DateConditionPanel(conditionModel.conditionModel(Invoice.DATE), invoiceDefinition);
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

		private static final class CustomerConditionPanel extends ColumnConditionPanel<Attribute<?>, Entity> {

			private final EntitySearchField searchField;

			private CustomerConditionPanel(ColumnConditionModel<Attribute<?>, Entity> conditionModel, EntityDefinition invoiceDefinition) {
				super(conditionModel, invoiceDefinition.attributes().definition(conditionModel.columnIdentifier()).caption());
				setLayout(new BorderLayout());
				setBorder(createTitledBorder(createEmptyBorder(), caption()));
				DefaultForeignKeyConditionModel foreignKeyConditionModel = (DefaultForeignKeyConditionModel) conditionModel;
				foreignKeyConditionModel.equalValue().addConsumer(customer ->
								foreignKeyConditionModel.inValues().value().set(customer));
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

			private final ComponentValue<Integer, NumberField<Integer>> yearValue = integerField()
							.initialValue(LocalDate.now().getYear())
							.listener(this::updateCondition)
							.focusable(false)
							.horizontalAlignment(SwingConstants.CENTER)
							.buildValue();
			private final ComponentValue<Integer, NumberField<Integer>> monthValue = integerField()
							.columns(2)
							.valueRange(1, 12)
							.listener(this::updateCondition)
							.rethrowValidationException(false)
							.horizontalAlignment(SwingConstants.CENTER)
							.keyEvent(KeyEvents.builder(KeyEvent.VK_UP)
											.action(control(this::incrementMonth)))
							.keyEvent(KeyEvents.builder(KeyEvent.VK_DOWN)
											.action(control(this::decrementMonth)))
							.keyEvent(KeyEvents.builder(KeyEvent.VK_UP)
											.modifiers(InputEvent.CTRL_DOWN_MASK)
											.action(control(this::incrementYear)))
							.keyEvent(KeyEvents.builder(KeyEvent.VK_DOWN)
											.modifiers(InputEvent.CTRL_DOWN_MASK)
											.action(control(this::decrementYear)))
							.buildValue();

			private DateConditionPanel(ColumnConditionModel<Attribute<?>, LocalDate> conditionModel, EntityDefinition invoiceDefinition) {
				super(conditionModel, invoiceDefinition.attributes().definition(conditionModel.columnIdentifier()).caption());
				setLayout(new BorderLayout());
				conditionModel.operator().set(Operator.BETWEEN);
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
																BUNDLE.getString("month"), CENTER, DEFAULT_POSITION))
												.build())
								.build(), BorderLayout.CENTER);
			}

			private static Integer incrementMonth(Integer month) {
				return month == null ? 1 :
								month == 12 ? 1 : month + 1;
			}

			private static Integer decrementMonth(Integer month) {
				return month == null ? 12 :
								month == 1 ? 12 : month - 1;
			}

			private void incrementYear() {
				yearValue.map(year -> year + 1);
			}

			private void decrementYear() {
				yearValue.map(year -> year - 1);
			}

			private void incrementMonth() {
				monthValue.map(DateConditionPanel::incrementMonth);
				if (monthValue.isEqualTo(1)) {
					incrementYear();
				}
			}

			private void decrementMonth() {
				monthValue.map(DateConditionPanel::decrementMonth);
				if (monthValue.isEqualTo(12)) {
					decrementYear();
				}
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
				int month = monthValue.optional().orElse(1);

				return LocalDate.of(year, month, 1);
			}

			private LocalDate upperBound() {
				int year = yearValue.optional().orElse(LocalDate.now().getYear());
				int month = monthValue.optional().orElse(12);
				YearMonth yearMonth = YearMonth.of(year, month);

				return LocalDate.of(year, month, yearMonth.lengthOfMonth());
			}

			private boolean hasInputFocus() {
				return yearValue.component().hasFocus() || monthValue.component().hasFocus();
			}
		}
	}
}
