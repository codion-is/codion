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
 * Copyright (c) Heinz M. Kabutz.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.reactive.value.Value;

import org.jspecify.annotations.Nullable;

import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ItemEvent;
import java.awt.event.MouseListener;

public class NullableCheckBox extends JCheckBox {

	/**
	 * The item state NULL.
	 * @see ItemEvent#SELECTED
	 * @see ItemEvent#DESELECTED
	 */
	public static final int NULL = 3;

	protected NullableCheckBox(@Nullable String text, @Nullable Icon icon) {
		super(text, icon);
		super.setModel(new NullableCheckBoxModel());
		setIcon(new NullableIcon());
	}

	@Override
	public void updateUI() {
		super.updateUI();
		setIcon(new NullableIcon());
	}

	/**
	 * Disabled.
	 * @param model the model
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public final void setModel(ButtonModel model) {
		if (getModel() instanceof NullableCheckBoxModel) {
			throw new UnsupportedOperationException("Setting the model of a NullableCheckBox after construction is not supported");
		}
		super.setModel(model);
	}

	/**
	 * @return the toggle state
	 */
	public final @Nullable Boolean get() {
		return model().value.get();
	}

	/**
	 * @param state the toggle state
	 */
	public final void set(@Nullable Boolean state) {
		model().value.set(state);
	}

	/**
	 * <p>Toggles between the states: false -&gt; null -&gt; true
	 * <p>This is the same order as used by macOS, win32, IntelliJ IDEA and on the web as recommended by W3C.
	 */
	public final void toggle() {
		model().toggle();
	}

	/**
	 * Finalize this one since we call it in the constructor
	 * @param listener the listener
	 */
	@Override
	public final synchronized void addMouseListener(MouseListener listener) {
		super.addMouseListener(listener);
	}

	@Override
	public final void setIcon(Icon defaultIcon) {
		super.setIcon(defaultIcon);
	}

	private NullableCheckBoxModel model() {
		return (NullableCheckBoxModel) getModel();
	}

	/**
	 * Instantiates a new NullableCheckBox with no caption.
	 */
	public static NullableCheckBox nullableCheckBox() {
		return new NullableCheckBox(null, null);
	}

	/**
	 * Instantiates a new NullableCheckBox.
	 * @param text the caption text, if any
	 */
	public static NullableCheckBox nullableCheckBox(@Nullable String text) {
		return new NullableCheckBox(text, null);
	}

	/**
	 * Instantiates a new NullableCheckBox.
	 * @param text the caption text, if any
	 * @param icon the icon, if any
	 */
	public static NullableCheckBox nullableCheckBox(@Nullable String text, @Nullable Icon icon) {
		return new NullableCheckBox(text, icon);
	}

	private final class NullableIcon implements Icon {

		private final Icon icon = UIManager.getIcon("CheckBox.icon");

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			icon.paintIcon(component, graphics, x, y);
			if (get() == null) {
				int width = getIconWidth();
				int height = getIconHeight();

				int dashWidth = width - 4;
				int dashHeight = Math.max(2, height / 6);
				int dashX = x + 2;
				int dashY = y + (height - dashHeight) / 2;

				Graphics2D graphics2D = (Graphics2D) graphics;
				graphics2D.setColor(isEnabled() ? getForeground() : UIManager.getColor("CheckBoxMenuItem.disabledForeground"));
				graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics2D.fillRect(dashX, dashY, dashWidth, dashHeight);
			}
		}

		@Override
		public int getIconWidth() {
			return icon.getIconWidth();
		}

		@Override
		public int getIconHeight() {
			return icon.getIconHeight();
		}
	}

	private static final class NullableCheckBoxModel extends ToggleButtonModel {

		private final Value<Boolean> value = Value.builder()
						.<Boolean>nullable()
						.consumer(this::onStateChanged)
						.build();

		@Override
		public boolean isSelected() {
			return value.is(true);
		}

		/**
		 * Toggles the underlying state
		 * @param ignored ignored
		 */
		@Override
		public void setSelected(boolean ignored) {
			toggle();
		}

		private void toggle() {
			Boolean state = value.get();
			if (state == null) {
				value.set(true);
			}
			else if (state) {
				value.set(false);
			}
			else {
				value.set(null);
			}
		}

		private void onStateChanged(Boolean state) {
			fireStateChanged();
			fireItemStateChanged(new ItemEvent(NullableCheckBoxModel.this, ItemEvent.ITEM_STATE_CHANGED, this,
							state == null ? NULL : (state ? ItemEvent.SELECTED : ItemEvent.DESELECTED)));
		}
	}
}