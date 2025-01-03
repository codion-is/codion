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
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.key.KeyEvents;

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
import java.awt.geom.Rectangle2D;

import static java.awt.event.KeyEvent.VK_SPACE;
import static java.util.Objects.requireNonNull;

/**
 * A JCheckBox implementation, which allows null values, via {@link NullableToggleButtonModel}.
 * Heavily influenced by TristateCheckBox by Heinz M. Kabutz.
 * <a href="https://www.javaspecialists.eu/archive/Issue145.html">https://www.javaspecialists.eu/archive/Issue145.html</a>
 * Included with permission.
 * @author Heinz M. Kabutz
 * @author Björn Darri Sigurðsson
 */
public class NullableCheckBox extends JCheckBox {

	/**
	 * Instantiates a new NullableCheckBox with no caption.
	 */
	public NullableCheckBox() {
		this(new NullableToggleButtonModel());
	}

	/**
	 * Instantiates a new NullableCheckBox with no caption.
	 * @param model the model
	 */
	public NullableCheckBox(NullableToggleButtonModel model) {
		this(model, null);
	}

	/**
	 * Instantiates a new NullableCheckBox.
	 * @param model the model
	 * @param text the caption text, if any
	 */
	public NullableCheckBox(NullableToggleButtonModel model, String text) {
		this(model, text, null);
	}

	/**
	 * Instantiates a new NullableCheckBox.
	 * @param model the model
	 * @param text the caption text, if any
	 * @param icon the icon, if any
	 */
	public NullableCheckBox(NullableToggleButtonModel model, String text, Icon icon) {
		super(text, icon);
		super.setModel(requireNonNull(model));
		setIcon(new NullableIcon());
		addMouseListener(new NullableMouseListener());
		KeyEvents.builder(VK_SPACE)
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

	private final class NullableMouseListener extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (isEnabled() && (e == null || notModified(e))) {
				model().toggleState().next();
			}
		}

		private boolean notModified(MouseEvent e) {
			return !e.isAltDown() && !e.isControlDown() && !e.isShiftDown() &&
							!e.isAltGraphDown() && !e.isMetaDown() && !e.isPopupTrigger();
		}
	}

	private final class NullableIcon implements Icon {

		private final Icon icon = UIManager.getIcon("CheckBox.icon");
		private final boolean flatLaf = getUI().getClass().getSimpleName().startsWith("Flat");

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			icon.paintIcon(component, graphics, x, y);
			if (model().toggleState().get() != null) {
				return;
			}

			double width = getIconWidth() / 3d;
			double height = getIconHeight() / 3d;

			//todo remove x/y adjustment hack for FlatLaf
			double xCorner = (x + width) + (flatLaf ? 0.5 : 0);
			double yCorner = (y + height) - (flatLaf ? 0.25 : 0);

			Rectangle2D rectangle = new Rectangle2D.Double(xCorner, yCorner, width, height);

			Graphics2D graphics2D = (Graphics2D) graphics;
			graphics2D.setColor(isEnabled() ? getForeground() : UIManager.getColor("CheckBoxMenuItem.disabledForeground"));
			graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics2D.fill(rectangle);
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
			model.toggleState().next();
		}
	}
}
