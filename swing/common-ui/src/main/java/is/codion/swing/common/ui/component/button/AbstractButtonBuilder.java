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

import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

abstract class AbstractButtonBuilder<T, C extends AbstractButton, B extends ButtonBuilder<T, C, B>>
				extends AbstractComponentBuilder<T, C, B> implements ButtonBuilder<T, C, B> {

	private static final String FONT = "Font";
	private static final String BACKGROUND = "Background";
	private static final String FOREGROUND = "Foreground";

	private final List<ActionListener> actionListeners = new ArrayList<>();

	private String text;
	private int mnemonic;
	private boolean includeText = true;
	private int horizontalAlignment = SwingConstants.CENTER;
	private int verticalAlignment = SwingConstants.CENTER;
	private int horizontalTextPosition = SwingConstants.TRAILING;
	private int verticalTextPosition = SwingConstants.CENTER;
	private boolean borderPainted = true;
	private boolean contentAreaFilled = true;
	private boolean focusPainted = true;
	private boolean rolloverEnabled = false;
	private Long multiClickThreshold;
	private Icon icon;
	private Icon pressedIcon;
	private Icon selectedIcon;
	private Icon rolloverIcon;
	private Icon rolloverSelectedIcon;
	private Icon disabledIcon;
	private Icon disabledSelectedIcon;
	private Integer iconTextGap;
	private Insets insets;
	private ButtonGroup buttonGroup;
	private boolean selected = false;
	private Action action;

	protected AbstractButtonBuilder(Value<T> linkedValue) {
		super(linkedValue);
	}

	@Override
	public final B text(String text) {
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
	public final B icon(Icon icon) {
		this.icon = icon;
		return self();
	}

	@Override
	public final B pressedIcon(Icon pressedIcon) {
		this.pressedIcon = pressedIcon;
		return self();
	}

	@Override
	public final B selectedIcon(Icon selectedIcon) {
		this.selectedIcon = selectedIcon;
		return self();
	}

	@Override
	public final B rolloverIcon(Icon rolloverIcon) {
		this.rolloverIcon = rolloverIcon;
		return self();
	}

	@Override
	public final B rolloverSelectedIcon(Icon rolloverSelectedIcon) {
		this.rolloverSelectedIcon = rolloverSelectedIcon;
		return self();
	}

	@Override
	public final B disabledIcon(Icon disabledIcon) {
		this.disabledIcon = disabledIcon;
		return self();
	}

	@Override
	public final B disabledSelectedIcon(Icon disabledSelectedIcon) {
		this.disabledSelectedIcon = disabledSelectedIcon;
		return self();
	}

	@Override
	public final B iconTextGap(int iconTextGap) {
		this.iconTextGap = iconTextGap;
		return self();
	}

	@Override
	public final B margin(Insets insets) {
		this.insets = requireNonNull(insets);
		return self();
	}

	@Override
	public final B buttonGroup(ButtonGroup buttonGroup) {
		this.buttonGroup = requireNonNull(buttonGroup);
		return self();
	}

	@Override
	public final B selected(boolean selected) {
		this.selected = selected;
		return self();
	}

	@Override
	public final B action(Action action) {
		this.action = action;
		return self();
	}

	@Override
	public final B control(Control control) {
		return action(control);
	}

	@Override
	public final B control(Control.Builder<?, ?> controlBuilder) {
		return control(requireNonNull(controlBuilder).build());
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
		if (!includeText) {
			button.setText(null);
			button.setHideActionText(true);
		}
		else if (text != null) {
			button.setText(text);
		}
		button.setHorizontalAlignment(horizontalAlignment);
		button.setVerticalAlignment(verticalAlignment);
		button.setVerticalTextPosition(verticalTextPosition);
		button.setHorizontalTextPosition(horizontalTextPosition);
		if (!borderPainted) {
			button.setBorderPainted(false);
		}
		if (!contentAreaFilled) {
			button.setContentAreaFilled(false);
		}
		if (!focusPainted) {
			button.setFocusPainted(false);
		}
		if (rolloverEnabled) {
			button.setRolloverEnabled(true);
		}
		if (multiClickThreshold != null) {
			button.setMultiClickThreshhold(multiClickThreshold);
		}
		if (mnemonic != 0) {
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
		if (selected) {
			button.setSelected(true);
		}

		return button;
	}

	protected abstract C createButton();

	static ButtonModel createButtonModel(ToggleControl toggleControl) {
		ButtonModel buttonModel = toggleControl.value().isNullable() ?
						new NullableToggleButtonModel(toggleControl.value().get()) : createToggleButtonModel(toggleControl.value().get());
		buttonModel.setEnabled(toggleControl.enabled().get());
		toggleControl.enabled().addConsumer(new SetEnabled(buttonModel));
		new BooleanButtonModelValue(buttonModel).link(toggleControl.value());

		return buttonModel;
	}

	private static JToggleButton.ToggleButtonModel createToggleButtonModel(boolean selected) {
		JToggleButton.ToggleButtonModel buttonModel = new JToggleButton.ToggleButtonModel();
		buttonModel.setSelected(selected);

		return buttonModel;
	}

	private static final class BooleanButtonModelValue extends AbstractValue<Boolean> {

		private final ButtonModel buttonModel;

		private BooleanButtonModelValue(ButtonModel buttonModel) {
			this.buttonModel = buttonModel;
			buttonModel.addItemListener(itemEvent -> notifyListeners());
		}

		@Override
		protected Boolean getValue() {
			if (buttonModel instanceof NullableToggleButtonModel) {
				return ((NullableToggleButtonModel) buttonModel).toggleState().get();
			}

			return buttonModel.isSelected();
		}

		@Override
		protected void setValue(Boolean value) {
			if (SwingUtilities.isEventDispatchThread()) {
				setModelValue(value);
			}
			else {
				try {
					SwingUtilities.invokeAndWait(() -> setModelValue(value));
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(ex);
				}
				catch (InvocationTargetException e) {
					Throwable cause = e.getCause();
					if (cause instanceof RuntimeException) {
						throw (RuntimeException) cause;
					}

					throw new RuntimeException(cause);
				}
				catch (RuntimeException e) {
					throw e;
				}
			}
		}

		private void setModelValue(Boolean value) {
			if (buttonModel instanceof NullableToggleButtonModel) {
				((NullableToggleButtonModel) buttonModel).toggleState().set(value);
			}
			else {
				buttonModel.setSelected(value != null && value);
			}
		}
	}

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

	private static final class AddActionListener implements java.util.function.Consumer<ActionListener> {

		private final AbstractButton button;

		private AddActionListener(AbstractButton button) {
			this.button = button;
		}

		@Override
		public void accept(ActionListener actionListener) {
			button.addActionListener(actionListener);
		}
	}

	private static final class SetEnabled implements Consumer<Boolean> {

		private final ButtonModel buttonModel;

		private SetEnabled(ButtonModel buttonModel) {
			this.buttonModel = buttonModel;
		}

		@Override
		public void accept(Boolean enabled) {
			buttonModel.setEnabled(enabled);
		}
	}
}
