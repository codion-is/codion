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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.laf;

import is.codion.swing.common.ui.component.Components;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIDefaults;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static javax.swing.BorderFactory.createLineBorder;

final class LookAndFeelPanel extends JPanel {

	private static final int BORDER_THICKNESS = 5;
	private static final int COLOR_LABEL_WIDTH = 42;

	private final UIDefaults nullDefaults = new UIDefaults(0, 0.1f);
	private final Map<LookAndFeelProvider, UIDefaults> lookAndFeelDefaults = new ConcurrentHashMap<>();

	private final JLabel textLabel = new JLabel();
	private final JLabel colorLabel = Components.label()
					.preferredWidth(COLOR_LABEL_WIDTH)
					.build();

	LookAndFeelPanel() {
		super(new BorderLayout());
		add(textLabel, BorderLayout.CENTER);
		add(colorLabel, BorderLayout.EAST);
	}

	void setLookAndFeel(LookAndFeelProvider lookAndFeel, boolean selected) {
		textLabel.setOpaque(true);
		colorLabel.setOpaque(true);
		textLabel.setText(lookAndFeel.lookAndFeelInfo().getName());
		UIDefaults defaults = defaults(lookAndFeel);
		if (defaults == nullDefaults) {
			textLabel.setBackground(selected ? Color.LIGHT_GRAY : Color.WHITE);
			textLabel.setForeground(Color.BLACK);
			colorLabel.setBackground(Color.WHITE);
			colorLabel.setBorder(null);
		}
		else {
			textLabel.setFont(defaults.getFont("TextField.font"));
			textLabel.setForeground(defaults.getColor(selected ? "TextField.selectionForeground" : "Label.foreground"));
			textLabel.setBackground(defaults.getColor(selected ? "TextField.selectionBackground" : "Label.background"));
			colorLabel.setBackground(defaults.getColor("TextField.background"));
			colorLabel.setBorder(createLineBorder(defaults.getColor("ProgressBar.foreground"), BORDER_THICKNESS));
		}
	}

	private UIDefaults defaults(LookAndFeelProvider lookAndFeelProvider) {
		return lookAndFeelDefaults.computeIfAbsent(lookAndFeelProvider, this::initializeLookAndFeelDefaults);
	}

	private UIDefaults initializeLookAndFeelDefaults(LookAndFeelProvider lookAndFeelProvider) {
		try {
			return lookAndFeelProvider.lookAndFeel().getDefaults();
		}
		catch (RuntimeException e) {
			System.err.println(e.getCause().getMessage());
			return nullDefaults;
		}
	}
}
