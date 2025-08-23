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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.observer.Observable;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static is.codion.swing.common.ui.Utilities.disposeParentWindow;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;

final class DefaultInputDialogBuilder<T> implements InputDialogBuilder<T> {

	static final ComponentStep COMPONENT = new DefaultComponentStep();

	private final JPanel basePanel = new JPanel(Layouts.borderLayout());
	private final ComponentValue<?, T> componentValue;
	private final OkCancelDialogBuilder okCancelDialogBuilder = new DefaultOkCancelDialogBuilder()
					.component(basePanel);

	private @Nullable String caption;

	DefaultInputDialogBuilder(ComponentValue<?, T> componentValue) {
		this.componentValue = requireNonNull(componentValue);
		this.basePanel.add(componentValue.component(), BorderLayout.CENTER);
		int gap = Layouts.GAP.getOrThrow();
		this.basePanel.setBorder(createEmptyBorder(gap, gap, 0, gap));
	}

	@Override
	public InputDialogBuilder<T> owner(@Nullable Window owner) {
		okCancelDialogBuilder.owner(owner);
		return this;
	}

	@Override
	public InputDialogBuilder<T> owner(@Nullable Component owner) {
		okCancelDialogBuilder.owner(owner);
		return this;
	}

	@Override
	public InputDialogBuilder<T> locationRelativeTo(@Nullable Component component) {
		okCancelDialogBuilder.locationRelativeTo(component);
		return this;
	}

	@Override
	public InputDialogBuilder<T> location(@Nullable Point location) {
		okCancelDialogBuilder.location(location);
		return this;
	}

	@Override
	public InputDialogBuilder<T> title(@Nullable Observable<String> title) {
		okCancelDialogBuilder.title(title);
		return this;
	}

	@Override
	public InputDialogBuilder<T> icon(@Nullable ImageIcon icon) {
		okCancelDialogBuilder.icon(icon);
		return this;
	}

	@Override
	public InputDialogBuilder<T> title(@Nullable String title) {
		okCancelDialogBuilder.title(title);
		return this;
	}

	@Override
	public InputDialogBuilder<T> caption(@Nullable String caption) {
		this.caption = caption;
		return this;
	}

	@Override
	public InputDialogBuilder<T> valid(ObservableState valid) {
		okCancelDialogBuilder.okEnabled(valid);
		return this;
	}

	@Override
	public InputDialogBuilder<T> validator(Predicate<@Nullable T> validator) {
		return valid(createInputValidObserver(requireNonNull(validator)));
	}

	@Override
	public InputDialogBuilder<T> keyEvent(KeyEvents.Builder keyEventBuilder) {
		okCancelDialogBuilder.keyEvent(keyEventBuilder);
		return this;
	}

	@Override
	public InputDialogBuilder<T> onBuild(Consumer<JDialog> onBuild) {
		okCancelDialogBuilder.onBuild(onBuild);
		return this;
	}

	@Override
	public void show(Predicate<@Nullable T> closeDialog) {
		requireNonNull(closeDialog);
		if (caption != null) {
			basePanel.add(new JLabel(caption), BorderLayout.NORTH);
		}
		okCancelDialogBuilder.onOk(() -> {
			if (closeDialog.test(componentValue.get())) {
				disposeParentWindow(componentValue.component());
			}
		}).show();
	}

	@Override
	public @Nullable T show() {
		State okPressed = State.state();
		if (caption != null) {
			basePanel.add(new JLabel(caption), BorderLayout.NORTH);
		}
		okCancelDialogBuilder.onOk(new OnOk(componentValue, okPressed)).show();
		if (okPressed.is()) {
			return componentValue.get();
		}

		throw new CancelException();
	}

	private ObservableState createInputValidObserver(Predicate<@Nullable T> inputValidator) {
		State validInputState = State.state(inputValidator.test(componentValue.get()));
		componentValue.addListener(new InputValidStateListener<>(validInputState, inputValidator, componentValue));

		return validInputState;
	}

	private static final class DefaultComponentStep implements ComponentStep {

		@Override
		public <T> InputDialogBuilder<T> component(ComponentValueBuilder<?, T, ?> componentBuilder) {
			return new DefaultInputDialogBuilder<>(requireNonNull(componentBuilder).buildValue());
		}

		@Override
		public <T> InputDialogBuilder<T> component(ComponentValue<?, T> componentValue) {
			return new DefaultInputDialogBuilder<>(componentValue);
		}
	}

	private static final class OnOk implements Runnable {

		private final ComponentValue<?, ?> componentValue;
		private final State okPressed;

		private OnOk(ComponentValue<?, ?> componentValue, State okPressed) {
			this.componentValue = componentValue;
			this.okPressed = okPressed;
		}

		@Override
		public void run() {
			disposeParentWindow(componentValue.component());
			okPressed.set(true);
		}
	}

	private static final class InputValidStateListener<T> implements Runnable {

		private final State validInputState;
		private final Predicate<@Nullable T> validInputPredicate;
		private final Value<T> componentValue;

		private InputValidStateListener(State validInputState, Predicate<@Nullable T> validInputPredicate, Value<T> componentValue) {
			this.validInputState = validInputState;
			this.validInputPredicate = validInputPredicate;
			this.componentValue = componentValue;
		}

		@Override
		public void run() {
			validInputState.set(validInputPredicate.test(componentValue.get()));
		}
	}
}
