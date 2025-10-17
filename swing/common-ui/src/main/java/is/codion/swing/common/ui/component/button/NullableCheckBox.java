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

import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.key.KeyEvents;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static is.codion.swing.common.model.component.button.NullableToggleButtonModel.nullableToggleButtonModel;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.util.Objects.requireNonNull;

/**
 * A JCheckBox implementation, which allows null values, via {@link NullableToggleButtonModel}.
 * This component is heavily influenced on TristateCheckBox by Heinz M. Kabutz.
 * Original article: http://www.javaspecialists.eu/archive/Issue145.html
 * Included with express permission from the author, 2019.
 * Naming, formatting and behavior refinements by Björn Darri Sigurðsson.
 * @author Heinz M. Kabutz
 * @author Björn Darri Sigurðsson
 */
public class NullableCheckBox extends JCheckBox {

	protected NullableCheckBox(NullableToggleButtonModel model, @Nullable String text, @Nullable Icon icon) {
		super(text, icon);
		super.setModel(requireNonNull(model));
		setIcon(new NullableIcon());
		addMouseListener(new NullableMouseListener());
		KeyEvents.builder()
						.keyCode(VK_SPACE)
						.action(new NextStateAction(model))
						.enable(this);
	}

	@Override
	public void updateUI() {
		super.updateUI();
		setIcon(new NullableIcon());
	}

	/**
	 * @return the underlying button model
	 */
	public final NullableToggleButtonModel model() {
		return (NullableToggleButtonModel) getModel();
	}

	/**
	 * Disabled.
	 * @param model the model
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public final void setModel(ButtonModel model) {
		if (getModel() instanceof NullableToggleButtonModel) {
			throw new UnsupportedOperationException("Setting the model of a NullableCheckBox after construction is not supported");
		}
		super.setModel(model);
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

	/**
	 * Instantiates a new NullableCheckBox with no caption.
	 */
	public static NullableCheckBox nullableCheckBox() {
		return new NullableCheckBox(nullableToggleButtonModel(), null, null);
	}

	/**
	 * Instantiates a new NullableCheckBox with no caption.
	 * @param model the model
	 */
	public static NullableCheckBox nullableCheckBox(NullableToggleButtonModel model) {
		return new NullableCheckBox(model, null, null);
	}

	/**
	 * Instantiates a new NullableCheckBox.
	 * @param model the model
	 * @param text the caption text, if any
	 */
	public static NullableCheckBox nullableCheckBox(NullableToggleButtonModel model, @Nullable String text) {
		return new NullableCheckBox(model, text, null);
	}

	/**
	 * Instantiates a new NullableCheckBox.
	 * @param model the model
	 * @param text the caption text, if any
	 * @param icon the icon, if any
	 */
	public static NullableCheckBox nullableCheckBox(NullableToggleButtonModel model, @Nullable String text, @Nullable Icon icon) {
		return new NullableCheckBox(model, text, icon);
	}

	private final class NullableMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (isEnabled() && (e == null || notModified(e))) {
				model().next();
			}
		}

		private boolean notModified(MouseEvent e) {
			return !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() &&
							!e.isAltGraphDown() && !e.isMetaDown() && !e.isPopupTrigger();
		}
	}

	private final class NullableIcon implements Icon {

		private final Icon icon = UIManager.getIcon("CheckBox.icon");

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			icon.paintIcon(component, graphics, x, y);
			if (model().get() == null) {
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

	private static final class NextStateAction extends AbstractAction {

		private final NullableToggleButtonModel model;

		private NextStateAction(NullableToggleButtonModel model) {
			this.model = model;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			model.next();
		}
	}
}
