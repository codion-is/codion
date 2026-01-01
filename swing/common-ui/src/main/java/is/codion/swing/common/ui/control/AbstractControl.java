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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
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
 * An abstract Control implementation, implementing everything except actionPerformed().
 */
abstract class AbstractControl extends AbstractAction implements Control {

	private static final String ENABLED = "enabled";

	static final String FONT = "Font";
	static final String BACKGROUND = "Background";
	static final String FOREGROUND = "Foreground";

	private final ObservableState enabledObservable;

	// Keep this in a field since it's added as a weak listener
	private final Enabler enabler = new Enabler();
	private final boolean initialized;

	AbstractControl(AbstractControlBuilder<?, ?> builder) {
		super((String) builder.values.get(NAME));
		initialized = true;
		enabledObservable = builder.enabled == null ? State.state(true).observable() : builder.enabled;
		enabledObservable.addWeakConsumer(enabler);
		super.setEnabled(enabledObservable.is());
		builder.values.forEach(super::putValue);
	}

	@Override
	public final String toString() {
		return caption().orElse(super.toString());
	}

	@Override
	public final void setEnabled(boolean newValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void putValue(String key, @Nullable Object newValue) {
		if (initialized) {
			throw new UnsupportedOperationException();
		}
		super.putValue(key, newValue);
	}

	@Override
	public final @Nullable Object getValue(String key) {
		if (ENABLED.equals(key)) {
			return enabledObservable.is();
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
	public final Optional<String> caption() {
		Object value = getValue(NAME);

		return value == null ? Optional.empty() : Optional.of(String.valueOf(value));
	}

	@Override
	public final ObservableState enabled() {
		return enabledObservable;
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
			if (SwingUtilities.isEventDispatchThread()) {
				AbstractControl.super.setEnabled(enabled);
			}
			else {
				SwingUtilities.invokeLater(() -> AbstractControl.super.setEnabled(enabled));
			}
		}
	}

	abstract static class AbstractControlBuilder<C extends Control, B extends ControlBuilder<C, B>> implements ControlBuilder<C, B> {

		private final Map<String, @Nullable Object> values = new HashMap<>();

		private @Nullable ObservableState enabled;

		@Override
		public final B caption(@Nullable String caption) {
			values.put(NAME, caption);
			return self();
		}

		@Override
		public final B enabled(@Nullable ObservableState enabled) {
			this.enabled = enabled;
			return self();
		}

		@Override
		public final B mnemonic(int mnemonic) {
			values.put(MNEMONIC_KEY, mnemonic);
			return self();
		}

		@Override
		public final B icon(@Nullable ControlIcon icon) {
			smallIcon(icon == null ? null : icon.small());
			largeIcon(icon == null ? null : icon.large());
			return self();
		}

		@Override
		public final B smallIcon(@Nullable Icon smallIcon) {
			values.put(SMALL_ICON, smallIcon);
			return self();
		}

		@Override
		public final B largeIcon(@Nullable Icon largeIcon) {
			values.put(LARGE_ICON_KEY, largeIcon);
			return self();
		}

		@Override
		public final B description(@Nullable String description) {
			values.put(SHORT_DESCRIPTION, description);
			return self();
		}

		@Override
		public final B foreground(@Nullable Color foreground) {
			values.put(FOREGROUND, foreground);
			return self();
		}

		@Override
		public final B background(@Nullable Color background) {
			values.put(BACKGROUND, background);
			return self();
		}

		@Override
		public final B font(@Nullable Font font) {
			values.put(FONT, font);
			return self();
		}

		@Override
		public final B keyStroke(@Nullable KeyStroke keyStroke) {
			values.put(ACCELERATOR_KEY, keyStroke);
			return self();
		}

		@Override
		public final B value(String key, @Nullable Object value) {
			requireNonNull(key);
			if (ENABLED.equals(key)) {
				throw new IllegalArgumentException("Can not set the enabled property of a Control");
			}
			values.put(key, value);
			return self();
		}

		protected final B self() {
			return (B) this;
		}
	}
}
