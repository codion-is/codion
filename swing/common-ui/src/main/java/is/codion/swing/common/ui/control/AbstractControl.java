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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * An abstrct Control implementation, implementing everything except actionPerformed().
 */
abstract class AbstractControl extends AbstractAction implements Control {

	private static final Consumer<Exception> DEFAULT_EXCEPTION_HANDLER = new DefaultExceptionHandler();
	private static final String ENABLED = "enabled";

	static final String FONT = "Font";
	static final String BACKGROUND = "Background";
	static final String FOREGROUND = "Foreground";

	private final StateObserver enabledObserver;

	// Keep this in a field since it's added as a weak listener
	private final Enabler enabler = new Enabler();
	private final boolean initialized;

	AbstractControl(AbstractControlBuilder<?, ?> builder) {
		super((String) builder.values.get(NAME));
		this.initialized = true;
		this.enabledObserver = builder.enabled == null ? State.state(true) : builder.enabled;
		this.enabledObserver.addWeakConsumer(enabler);
		super.setEnabled(this.enabledObserver.get());
		builder.values.forEach(super::putValue);
	}

	@Override
	public final String toString() {
		return name().orElse(super.toString());
	}

	@Override
	public final void setEnabled(boolean newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void putValue(String key, Object newValue) {
		if (initialized) {
			throw new UnsupportedOperationException();
		}
		super.putValue(key, newValue);
	}

	@Override
	public final Object getValue(String key) {
		if (ENABLED.equals(key)) {
			return enabledObserver.get();
		}

		return super.getValue(key);
	}

	@Override
	public final Collection<String> keys() {
		return Arrays.stream(getKeys())
						.filter(String.class::isInstance)
						.map(String.class::cast)
						.collect(toList());
	}

	@Override
	public final Optional<String> description() {
		return Optional.ofNullable((String) getValue(SHORT_DESCRIPTION));
	}

	@Override
	public final Optional<String> name() {
		Object value = getValue(NAME);

		return value == null ? Optional.empty() : Optional.of(String.valueOf(value));
	}

	@Override
	public final StateObserver enabled() {
		return enabledObserver;
	}

	@Override
	public final OptionalInt mnemonic() {
		Integer mnemonic = (Integer) getValue(MNEMONIC_KEY);

		return mnemonic == null || mnemonic.equals(0) ? OptionalInt.empty() : OptionalInt.of(mnemonic);
	}

	@Override
	public final Optional<KeyStroke> keyStroke() {
		return Optional.ofNullable((KeyStroke) getValue(ACCELERATOR_KEY));
	}

	@Override
	public final Optional<Icon> smallIcon() {
		return Optional.ofNullable((Icon) getValue(SMALL_ICON));
	}

	@Override
	public final Optional<Icon> largeIcon() {
		return Optional.ofNullable((Icon) getValue(LARGE_ICON_KEY));
	}

	@Override
	public final Optional<Color> background() {
		return Optional.ofNullable((Color) getValue(BACKGROUND));
	}

	@Override
	public final Optional<Color> foreground() {
		return Optional.ofNullable((Color) getValue(FOREGROUND));
	}

	@Override
	public final Optional<Font> font() {
		return Optional.ofNullable((Font) getValue(FONT));
	}

	private final class Enabler implements Consumer<Boolean> {

		@Override
		public void accept(Boolean enabled) {
			AbstractControl.super.setEnabled(enabled);
		}
	}

	abstract static class AbstractControlBuilder<C extends Control, B extends Builder<C, B>> implements Builder<C, B> {

		private final Map<String, Object> values = new HashMap<>();

		private StateObserver enabled;
		protected Consumer<Exception> onException = DEFAULT_EXCEPTION_HANDLER;

		@Override
		public final B name(String name) {
			values.put(NAME, name);
			return self();
		}

		@Override
		public final B enabled(StateObserver enabled) {
			this.enabled = enabled;
			return self();
		}

		@Override
		public final B mnemonic(int mnemonic) {
			values.put(MNEMONIC_KEY, mnemonic);
			return self();
		}

		@Override
		public final B smallIcon(Icon smallIcon) {
			values.put(SMALL_ICON, smallIcon);
			return self();
		}

		@Override
		public final B largeIcon(Icon largeIcon) {
			values.put(LARGE_ICON_KEY, largeIcon);
			return self();
		}

		@Override
		public final B description(String description) {
			values.put(SHORT_DESCRIPTION, description);
			return self();
		}

		@Override
		public final B foreground(Color foreground) {
			values.put(FOREGROUND, foreground);
			return self();
		}

		@Override
		public final B background(Color background) {
			values.put(BACKGROUND, background);
			return self();
		}

		@Override
		public final B font(Font font) {
			values.put(FONT, font);
			return self();
		}

		@Override
		public final B keyStroke(KeyStroke keyStroke) {
			values.put(ACCELERATOR_KEY, keyStroke);
			return self();
		}

		@Override
		public final B value(String key, Object value) {
			if (ENABLED.equals(key)) {
				throw new IllegalArgumentException("Can not set the enabled property of a Control");
			}
			values.put(key, value);
			return self();
		}

		@Override
		public final B onException(Consumer<Exception> onException) {
			this.onException = requireNonNull(onException);
			return self();
		}

		protected final B self() {
			return (B) this;
		}
	}

	private static final class DefaultExceptionHandler implements Consumer<Exception> {

		@Override
		public void accept(Exception exception) {
			if (exception instanceof CancelException) {
				return; // Operation cancelled
			}
			if (exception instanceof RuntimeException) {
				throw (RuntimeException) exception;
			}

			throw new RuntimeException(exception);
		}
	}
}
