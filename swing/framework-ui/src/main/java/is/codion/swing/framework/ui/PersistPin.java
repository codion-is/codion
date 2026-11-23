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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jspecify.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.SwingConstants.LEADING;

/**
 * Lets the user toggle whether an editor value persists, shown as a pin following the caption of the label of its
 * component, dimmed while the value does not persist, and toggled by a key stroke while the component has the focus,
 * or by clicking the pin. Hidden and disabled while not enabled, the edit panel unable to insert.
 */
final class PersistPin {

	private static final String LABELED_BY = "labeledBy";
	private static final float DIMMED_ALPHA = 0.3f;
	private static final int PIN_HIT_MARGIN = 2;

	private final JLabel label;
	private final State persist;
	private final ObservableState enabled;
	private final String toolTipText;
	private final Icon pin = FrameworkIcons.instance().get(FrameworkIcons.PIN).small();
	private final Icon dimmedPin = new DimmedIcon(pin);

	private PersistPin(JLabel label, State persist, ObservableState enabled, @Nullable KeyStroke keyStroke, String caption) {
		this.label = label;
		this.persist = persist;
		this.enabled = enabled;
		this.toolTipText = keyStroke == null ? caption : caption + " (" + KeyboardShortcutsPanel.text(keyStroke) + ")";
		label.setHorizontalTextPosition(LEADING);
		PinMouseListener mouseListener = new PinMouseListener();
		label.addMouseListener(mouseListener);
		label.addMouseMotionListener(mouseListener);
		persist.addListener(() -> SwingUtilities.invokeLater(this::update));
		enabled.addListener(() -> SwingUtilities.invokeLater(this::update));
		update();
	}

	/**
	 * Enables a persist toggle for the given component, the pin shown on the label associated with it, if any.
	 * @param component the component
	 * @param persist the persist state of its value
	 * @param enabled the state controlling whether the toggle is shown and enabled
	 * @param keyStroke the key stroke toggling the state while the component has the focus, null for none
	 * @param caption the caption of the toggle
	 */
	static void enable(JComponent component, State persist, ObservableState enabled, @Nullable KeyStroke keyStroke, String caption) {
		requireNonNull(component);
		requireNonNull(persist);
		requireNonNull(enabled);
		requireNonNull(caption);
		if (keyStroke != null) {
			// A disabled action does not consume the key stroke, leaving it to a mnemonic for one
			KeyEvents.builder()
							.keyStroke(keyStroke)
							.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.action(Control.builder()
											.command(persist::toggle)
											.enabled(enabled)
											.build())
							.enable(component);
		}
		JLabel label = (JLabel) component.getClientProperty(LABELED_BY);
		if (label != null) {
			new PersistPin(label, persist, enabled, keyStroke, caption);
		}
	}

	private void update() {
		if (enabled.is()) {
			label.setIcon(persist.is() ? pin : dimmedPin);
			label.setToolTipText(toolTipText);
		}
		else {
			label.setIcon(null);
			label.setToolTipText(null);
			label.setCursor(null);
		}
	}

	private final class PinMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent event) {
			if (SwingUtilities.isLeftMouseButton(event) && onPin(event.getPoint())) {
				persist.toggle();
			}
		}

		@Override
		public void mouseMoved(MouseEvent event) {
			label.setCursor(onPin(event.getPoint()) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
		}

		@Override
		public void mouseExited(MouseEvent event) {
			label.setCursor(null);
		}

		private boolean onPin(Point point) {
			if (label.getIcon() == null) {
				return false;
			}
			Insets insets = label.getInsets();
			Rectangle view = new Rectangle(insets.left, insets.top,
							label.getWidth() - insets.left - insets.right, label.getHeight() - insets.top - insets.bottom);
			Rectangle iconBounds = new Rectangle();
			SwingUtilities.layoutCompoundLabel(label, label.getFontMetrics(label.getFont()), label.getText(), label.getIcon(),
							label.getVerticalAlignment(), label.getHorizontalAlignment(),
							label.getVerticalTextPosition(), label.getHorizontalTextPosition(),
							view, iconBounds, new Rectangle(), label.getIconTextGap());
			iconBounds.grow(PIN_HIT_MARGIN, PIN_HIT_MARGIN);

			return iconBounds.contains(point);
		}
	}

	private static final class DimmedIcon implements Icon {

		private final Icon icon;

		private DimmedIcon(Icon icon) {
			this.icon = icon;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			try {
				graphics2D.setComposite(AlphaComposite.SrcOver.derive(DIMMED_ALPHA));
				icon.paintIcon(component, graphics2D, x, y);
			}
			finally {
				graphics2D.dispose();
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
}
