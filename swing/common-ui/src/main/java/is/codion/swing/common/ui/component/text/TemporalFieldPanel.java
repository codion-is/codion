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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.state.ObservableState;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;
import is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Optional;

import static is.codion.swing.common.ui.component.Components.panel;
import static java.util.Objects.requireNonNull;

/**
 * A panel for a TemporalField with button for displaying a calendar
 * @param <T> the Temporal type supplied by this panel
 * @see #supports(Class)
 */
public final class TemporalFieldPanel<T extends Temporal> extends JPanel {

	private final TemporalField<T> temporalField;
	private final JButton button;

	TemporalFieldPanel(DefaultBuilder<T> builder) {
		temporalField = requireNonNull(builder.createTemporalField());
		button = createButton(builder);
		initializeUI();
	}

	/**
	 * @return the temporal input field
	 */
	public TemporalField<T> temporalField() {
		return temporalField;
	}

	/**
	 * @return the calendar button
	 */
	public JButton calendarButton() {
		return button;
	}

	/**
	 * @return the Temporal value currently being displayed, an empty Optional in case of an incomplete/unparseable date
	 */
	public Optional<T> optional() {
		return temporalField.optional();
	}

	/**
	 * @return the Temporal value currently being displayed, null in case of an incomplete/unparseable date
	 */
	public @Nullable T get() {
		return temporalField.get();
	}

	/**
	 * Sets the date in the input field, clears the field if {@code date} is null.
	 * @param temporal the temporal value to set
	 */
	public void set(@Nullable Temporal temporal) {
		temporalField.set(temporal);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		temporalField.setEnabled(enabled);
	}

	@Override
	public void setToolTipText(@Nullable String text) {
		temporalField.setToolTipText(text);
	}

	/**
	 * {@link TemporalFieldPanel} supports {@link LocalDate} and {@link LocalDateTime}.
	 * @param temporalClass the temporal type
	 * @param <T> the temporal type
	 * @return true if {@link TemporalFieldPanel} supports the given type
	 */
	public static <T extends Temporal> boolean supports(Class<T> temporalClass) {
		return CalendarPanel.supportedTypes().contains(requireNonNull(temporalClass));
	}

	/**
	 * @return a {@link Builder.TemporalClassStep}
	 */
	public static Builder.TemporalClassStep builder() {
		return DefaultBuilder.TEMPORAL_CLASS;
	}

	/**
	 * Builds a {@link TemporalFieldPanel}
	 * @param <T> the temporal type
	 */
	public interface Builder<T extends Temporal> extends ComponentValueBuilder<TemporalFieldPanel<T>, T, Builder<T>> {

		/**
		 * Provides a {@link TemporalFieldPanel.Builder}
		 */
		interface TemporalClassStep {

			/**
			 * @param <T> the value type
			 * @param temporalClass the temporal class
			 * @return a builder for a temporal panel
			 */
			<T extends Temporal> Builder<T> temporalClass(Class<T> temporalClass);
		}

		/**
		 * @param dateTimePattern the date time pattern
		 * @return this builder instance
		 */
		Builder<T> dateTimePattern(String dateTimePattern);

		/**
		 * @param selectAllOnFocusGained if true the component will select contents on focus gained
		 * @return this builder instance
		 */
		Builder<T> selectAllOnFocusGained(boolean selectAllOnFocusGained);

		/**
		 * @param columns the number of colums in the temporal field
		 * @return this builder instance
		 */
		Builder<T> columns(int columns);

		/**
		 * @param updateOn specifies when the underlying value should be updated
		 * @return this builder instance
		 */
		Builder<T> updateOn(UpdateOn updateOn);

		/**
		 * Default false.
		 * @param buttonFocusable true if the calendar button should be focusable
		 * @return this builder instance
		 */
		Builder<T> buttonFocusable(boolean buttonFocusable);

		/**
		 * @param calendarIcon the calendar icon
		 * @return this builder instance
		 */
		Builder<T> calendarIcon(ImageIcon calendarIcon);
	}

	private void initializeUI() {
		setLayout(new BorderLayout());
		add(temporalField, BorderLayout.CENTER);
		add(panel()
						.layout(new GridLayout(1, 1, 0, 0))
						.add(button)
						.build(), BorderLayout.EAST);
		addFocusListener(new InputFocusAdapter(temporalField));
	}

	private static final class InputFocusAdapter extends FocusAdapter {

		private final JFormattedTextField inputField;

		private InputFocusAdapter(JFormattedTextField inputField) {
			this.inputField = inputField;
		}

		@Override
		public void focusGained(FocusEvent e) {
			inputField.requestFocusInWindow();
		}
	}

	private JButton createButton(DefaultBuilder<T> builder) {
		return Components.button()
						.control(temporalField.calendarControl().orElseThrow(() ->
										new IllegalArgumentException("TemporalField does not support a calendar for: " +
														temporalField.temporalClass())))
						.focusable(builder.buttonFocusable)
						.preferredSize(new Dimension(temporalField.getPreferredSize().height, temporalField.getPreferredSize().height))
						.build();
	}

	private static final class DefaultTemporalClassStep implements Builder.TemporalClassStep {

		@Override
		public <T extends Temporal> Builder<T> temporalClass(Class<T> temporalClass) {
			return new DefaultBuilder<>(temporalClass);
		}
	}

	private static final class DefaultBuilder<T extends Temporal>
					extends AbstractComponentValueBuilder<TemporalFieldPanel<T>, T, Builder<T>>
					implements Builder<T> {

		private static final Builder.TemporalClassStep TEMPORAL_CLASS = new DefaultTemporalClassStep();

		private final TemporalField.Builder<T> temporalFieldBuilder;

		private boolean buttonFocusable;

		private DefaultBuilder(Class<T> valueClass) {
			if (!supports(valueClass)) {
				throw new IllegalArgumentException("Unsupported temporal type: " + valueClass);
			}
			temporalFieldBuilder = TemporalField.builder().temporalClass(valueClass);
		}

		@Override
		public Builder<T> dateTimePattern(String dateTimePattern) {
			temporalFieldBuilder.dateTimePattern(dateTimePattern);
			return this;
		}

		@Override
		public Builder<T> selectAllOnFocusGained(boolean selectAllOnFocusGained) {
			temporalFieldBuilder.selectAllOnFocusGained(selectAllOnFocusGained);
			return this;
		}

		@Override
		public Builder<T> columns(int columns) {
			temporalFieldBuilder.columns(columns);
			return this;
		}

		@Override
		public Builder<T> updateOn(UpdateOn updateOn) {
			temporalFieldBuilder.updateOn(updateOn);
			return this;
		}

		@Override
		public Builder<T> buttonFocusable(boolean buttonFocusable) {
			this.buttonFocusable = buttonFocusable;
			return this;
		}

		@Override
		public Builder<T> calendarIcon(ImageIcon calendarIcon) {
			temporalFieldBuilder.calendarIcon(calendarIcon);
			return this;
		}

		@Override
		protected TemporalFieldPanel<T> createComponent() {
			return new TemporalFieldPanel<>(this);
		}

		@Override
		protected ComponentValue<TemporalFieldPanel<T>, T> createComponentValue(TemporalFieldPanel<T> component) {
			return new TemporalFieldPanelValue<>(component);
		}

		@Override
		protected void enableTransferFocusOnEnter(TemporalFieldPanel<T> component, TransferFocusOnEnter transferFocusOnEnter) {
			transferFocusOnEnter.enable(component.temporalField);
			transferFocusOnEnter.enable(component.button);
		}

		@Override
		protected void enableValidIndicator(ValidIndicatorFactory validIndicatorFactory,
																				TemporalFieldPanel<T> component, ObservableState valid) {
			validIndicatorFactory.enable(component.temporalField, valid);
		}

		private TemporalField<T> createTemporalField() {
			return temporalFieldBuilder.build();
		}
	}

	private static final class TemporalFieldPanelValue<T extends Temporal> extends AbstractComponentValue<TemporalFieldPanel<T>, T> {

		private TemporalFieldPanelValue(TemporalFieldPanel<T> inputPanel) {
			super(inputPanel);
			inputPanel.temporalField().observable().addListener(new NotifyListeners());
		}

		@Override
		protected @Nullable T getComponentValue() {
			return component().get();
		}

		@Override
		protected void setComponentValue(@Nullable T value) {
			component().set(value);
		}

		private final class NotifyListeners implements Runnable {

			@Override
			public void run() {
				notifyObserver();
			}
		}
	}
}
