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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.label;

import is.codion.common.observer.Observable;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;

final class DefaultLabelBuilder<T> extends AbstractComponentValueBuilder<T, JLabel, LabelBuilder<T>> implements LabelBuilder<T> {

	private @Nullable String text;
	private @Nullable Icon icon;
	private int horizontalAlignment = HORIZONTAL_ALIGNMENT.getOrThrow();
	private @Nullable Integer displayedMnemonic;
	private @Nullable Character displayedMnemonicChar;
	private int iconTextGap = -1;
	private @Nullable JComponent component;

	DefaultLabelBuilder() {}

	@Override
	public LabelBuilder<T> text(@Nullable String text) {
		this.text = text;
		return this;
	}

	@Override
	public LabelBuilder<T> text(Observable<String> text) {
		link((Value<T>) text);
		return this;
	}

	@Override
	public LabelBuilder<T> horizontalAlignment(int horizontalAlignment) {
		this.horizontalAlignment = horizontalAlignment;
		return this;
	}

	@Override
	public LabelBuilder<T> displayedMnemonic(int displayedMnemonic) {
		this.displayedMnemonic = displayedMnemonic;
		return this;
	}

	@Override
	public LabelBuilder<T> displayedMnemonic(char displayedMnemonic) {
		this.displayedMnemonicChar = displayedMnemonic;
		return this;
	}

	@Override
	public LabelBuilder<T> labelFor(@Nullable JComponent component) {
		this.component = component;
		return this;
	}

	@Override
	public LabelBuilder<T> icon(@Nullable Icon icon) {
		this.icon = icon;
		return this;
	}

	@Override
	public LabelBuilder<T> iconTextGap(int iconTextGap) {
		this.iconTextGap = iconTextGap;
		return this;
	}

	@Override
	protected JLabel createComponent() {
		JLabel label = new JLabel(text, icon, horizontalAlignment);
		if (displayedMnemonic != null) {
			label.setDisplayedMnemonic(displayedMnemonic);
		}
		if (displayedMnemonicChar != null) {
			label.setDisplayedMnemonic(displayedMnemonicChar);
		}
		if (component != null) {
			label.setLabelFor(component);
		}
		if (iconTextGap >= 0) {
			label.setIconTextGap(iconTextGap);
		}

		return label;
	}

	@Override
	protected ComponentValue<T, JLabel> createComponentValue(JLabel component) {
		return new LabelComponentValue<>(component);
	}
}
