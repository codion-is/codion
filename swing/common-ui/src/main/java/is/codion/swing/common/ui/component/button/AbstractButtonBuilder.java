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
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.builder.AbstractComponentValueBuilder;
import is.codion.swing.common.ui.control.Control;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

abstract class AbstractButtonBuilder<C extends AbstractButton, T, B extends ButtonBuilder<C, T, B>>
				extends AbstractComponentValueBuilder<C, T, B> implements ButtonBuilder<C, T, B> {

	private static final String FONT = "Font";
	private static final String BACKGROUND = "Background";
	private static final String FOREGROUND = "Foreground";

	private final List<ActionListener> actionListeners = new ArrayList<>();

	private @Nullable String text;
	private @Nullable Integer mnemonic;
	private @Nullable Boolean includeText;
	private @Nullable Integer horizontalAlignment;
	private @Nullable Integer verticalAlignment;
	private @Nullable Integer horizontalTextPosition;
	private @Nullable Integer verticalTextPosition;
	private @Nullable Boolean borderPainted;
	private @Nullable Boolean contentAreaFilled;
	private @Nullable Boolean focusPainted;
	private @Nullable Boolean rolloverEnabled;
	private @Nullable Long multiClickThreshold;
	private @Nullable Icon icon;
	private @Nullable Icon pressedIcon;
	private @Nullable Icon selectedIcon;
	private @Nullable Icon rolloverIcon;
	private @Nullable Icon rolloverSelectedIcon;
	private @Nullable Icon disabledIcon;
	private @Nullable Icon disabledSelectedIcon;
	private @Nullable Integer iconTextGap;
	private @Nullable Insets insets;
	private @Nullable ButtonGroup buttonGroup;
	private @Nullable Boolean selected;
	private @Nullable Action action;

	protected AbstractButtonBuilder() {}

	@Override
	public final B text(@Nullable String text) {
		this.text = text;
		return self();
	}

	@Override
	public final B mnemonic(int mnemonic) {
		this.mnemonic = mnemonic;
		return self();
	}

	@Override
	public final B includeText(boolean includeText) {
		this.includeText = includeText;
		return self();
	}

	@Override
	public final B horizontalAlignment(int horizontalAlignment) {
		this.horizontalAlignment = horizontalAlignment;
		return self();
	}

	@Override
	public final B verticalAlignment(int verticalAlignment) {
		this.verticalAlignment = verticalAlignment;
		return self();
	}

	@Override
	public final B horizontalTextPosition(int horizontalTextPosition) {
		this.horizontalTextPosition = horizontalTextPosition;
		return self();
	}

	@Override
	public final B verticalTextPosition(int verticalTextPosition) {
		this.verticalTextPosition = verticalTextPosition;
		return self();
	}

	@Override
	public final B borderPainted(boolean borderPainted) {
		this.borderPainted = borderPainted;
		return self();
	}

	@Override
	public final B contentAreaFilled(boolean contentAreaFilled) {
		this.contentAreaFilled = contentAreaFilled;
		return self();
	}

	@Override
	public final B focusPainted(boolean focusPainted) {
		this.focusPainted = focusPainted;
		return self();
	}

	@Override
	public final B rolloverEnabled(boolean rolloverEnabled) {
		this.rolloverEnabled = rolloverEnabled;
		return self();
	}

	@Override
	public final B multiClickThreshold(long multiClickThreshold) {
		this.multiClickThreshold = multiClickThreshold;
		return self();
	}

	@Override
	public final B icon(@Nullable Icon icon) {
		this.icon = icon;
		return self();
	}

	@Override
	public final B pressedIcon(@Nullable Icon pressedIcon) {
		this.pressedIcon = pressedIcon;
		return self();
	}

	@Override
	public final B selectedIcon(@Nullable Icon selectedIcon) {
		this.selectedIcon = selectedIcon;
		return self();
	}

	@Override
	public final B rolloverIcon(@Nullable Icon rolloverIcon) {
		this.rolloverIcon = rolloverIcon;
		return self();
	}

	@Override
	public final B rolloverSelectedIcon(@Nullable Icon rolloverSelectedIcon) {
		this.rolloverSelectedIcon = rolloverSelectedIcon;
		return self();
	}

	@Override
	public final B disabledIcon(@Nullable Icon disabledIcon) {
		this.disabledIcon = disabledIcon;
		return self();
	}

	@Override
	public final B disabledSelectedIcon(@Nullable Icon disabledSelectedIcon) {
		this.disabledSelectedIcon = disabledSelectedIcon;
		return self();
	}

	@Override
	public final B iconTextGap(int iconTextGap) {
		this.iconTextGap = iconTextGap;
		return self();
	}

	@Override
	public final B margin(@Nullable Insets insets) {
		this.insets = insets;
		return self();
	}

	@Override
	public final B buttonGroup(@Nullable ButtonGroup buttonGroup) {
		this.buttonGroup = buttonGroup;
		return self();
	}

	@Override
	public final B selected(boolean selected) {
		this.selected = selected;
		return self();
	}

	@Override
	public final B action(@Nullable Action action) {
		this.action = action;
		return self();
	}

	@Override
	public final B control(Control control) {
		return action(control);
	}

	@Override
	public final B control(Supplier<? extends Control> control) {
		return control(requireNonNull(control).get());
	}

	@Override
	public final B actionListener(ActionListener actionListener) {
		this.actionListeners.add(requireNonNull(actionListener));
		return self();
	}

	@Override
	protected final C createComponent() {
		C button = createButton();
		if (action != null) {
			button.setAction(action);
			button.setBackground((Color) action.getValue(BACKGROUND));
			button.setForeground((Color) action.getValue(FOREGROUND));
			Font actionFont = (Font) action.getValue(FONT);
			if (actionFont != null) {
				button.setFont(actionFont);
			}
			action.addPropertyChangeListener(new ActionPropertyChangeListener(button));
		}
		actionListeners.forEach(new AddActionListener(button));
		if (includeText != null && !includeText) {
			button.setText(null);
			button.setHideActionText(true);
		}
		else if (text != null) {
			button.setText(text);
		}
		if (horizontalAlignment != null) {
			button.setHorizontalAlignment(horizontalAlignment);
		}
		if (verticalAlignment != null) {
			button.setVerticalAlignment(verticalAlignment);
		}
		if (verticalTextPosition != null) {
			button.setVerticalTextPosition(verticalTextPosition);
		}
		if (horizontalTextPosition != null) {
			button.setHorizontalTextPosition(horizontalTextPosition);
		}
		if (borderPainted != null) {
			button.setBorderPainted(borderPainted);
		}
		if (contentAreaFilled != null) {
			button.setContentAreaFilled(contentAreaFilled);
		}
		if (focusPainted != null) {
			button.setFocusPainted(focusPainted);
		}
		if (rolloverEnabled != null) {
			button.setRolloverEnabled(rolloverEnabled);
		}
		if (multiClickThreshold != null) {
			button.setMultiClickThreshhold(multiClickThreshold);
		}
		if (mnemonic != null) {
			button.setMnemonic(mnemonic);
		}
		if (icon != null) {
			button.setIcon(icon);
		}
		if (pressedIcon != null) {
			button.setPressedIcon(pressedIcon);
		}
		if (selectedIcon != null) {
			button.setSelectedIcon(selectedIcon);
		}
		if (rolloverIcon != null) {
			button.setRolloverIcon(rolloverIcon);
		}
		if (rolloverSelectedIcon != null) {
			button.setRolloverSelectedIcon(rolloverSelectedIcon);
		}
		if (disabledIcon != null) {
			button.setDisabledIcon(disabledIcon);
		}
		if (disabledSelectedIcon != null) {
			button.setDisabledSelectedIcon(disabledSelectedIcon);
		}
		if (iconTextGap != null) {
			button.setIconTextGap(iconTextGap);
		}
		if (insets != null) {
			button.setMargin(insets);
		}
		if (buttonGroup != null) {
			buttonGroup.add(button);
		}
		if (selected != null) {
			button.setSelected(selected);
		}

		return button;
	}

	protected abstract C createButton();

	private static final class ActionPropertyChangeListener implements PropertyChangeListener {

		private final AbstractButton button;

		private ActionPropertyChangeListener(AbstractButton button) {
			this.button = requireNonNull(button);
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			switch (evt.getPropertyName()) {
				case BACKGROUND:
					button.setBackground((Color) evt.getNewValue());
					break;
				case FOREGROUND:
					button.setForeground((Color) evt.getNewValue());
					break;
				case FONT:
					if (evt.getNewValue() != null) {
						button.setFont((Font) evt.getNewValue());
					}
					break;
				case Action.MNEMONIC_KEY:
					button.setMnemonic((Integer) evt.getNewValue());
					break;
				default:
					break;
			}
		}
	}

	private static final class AddActionListener implements Consumer<ActionListener> {

		private final AbstractButton button;

		private AddActionListener(AbstractButton button) {
			this.button = button;
		}

		@Override
		public void accept(ActionListener actionListener) {
			button.addActionListener(actionListener);
		}
	}
}
