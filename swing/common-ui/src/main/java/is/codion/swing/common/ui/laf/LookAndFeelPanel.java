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
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.EMPTY_MAP;
import static javax.swing.BorderFactory.createLineBorder;

final class LookAndFeelPanel extends JPanel {

	private static final int BORDER_THICKNESS = 5;
	private static final int COLOR_LABEL_WIDTH = 42;
	private static final String TEXT_FIELD_FONT = "TextField.font";
	private static final String TEXT_FIELD_SELECTION_FOREGROUND = "TextField.selectionForeground";
	private static final String TEXT_FIELD_SELECTION_BACKGROUND = "TextField.selectionBackground";
	private static final String LABEL_FOREGROUND = "Label.foreground";
	private static final String LABEL_BACKGROUND = "Label.background";
	private static final String TEXT_FIELD_BACKGROUND = "TextField.background";
	private static final String PROGRESS_BAR_FOREGROUND = "ProgressBar.foreground";

	private final Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults;

	private final JLabel textLabel = new JLabel();
	private final JLabel colorLabel = Components.label()
					.preferredWidth(COLOR_LABEL_WIDTH)
					.build();

	LookAndFeelPanel(Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults) {
		super(new BorderLayout());
		this.lookAndFeelDefaults = lookAndFeelDefaults;
		add(textLabel, BorderLayout.CENTER);
		add(colorLabel, BorderLayout.EAST);
	}

	void setLookAndFeel(LookAndFeelEnabler lookAndFeel, boolean selected) {
		textLabel.setOpaque(true);
		colorLabel.setOpaque(true);
		textLabel.setText(lookAndFeel.lookAndFeelInfo().getName());
		Map<String, Object> defaults = defaults(lookAndFeel);
		if (defaults == EMPTY_MAP) {
			textLabel.setBackground(selected ? Color.LIGHT_GRAY : Color.WHITE);
			textLabel.setForeground(Color.BLACK);
			colorLabel.setBackground(Color.WHITE);
			colorLabel.setBorder(null);
		}
		else {
			textLabel.setFont((Font) defaults.get(TEXT_FIELD_FONT));
			textLabel.setForeground((Color) defaults.get(selected ? TEXT_FIELD_SELECTION_FOREGROUND : LABEL_FOREGROUND));
			textLabel.setBackground((Color) defaults.get(selected ? TEXT_FIELD_SELECTION_BACKGROUND : LABEL_BACKGROUND));
			colorLabel.setBackground((Color) defaults.get(TEXT_FIELD_BACKGROUND));
			colorLabel.setBorder(createLineBorder((Color) defaults.get(PROGRESS_BAR_FOREGROUND), BORDER_THICKNESS));
		}
	}

	private Map<String, Object> defaults(LookAndFeelEnabler lookAndFeelEnabler) {
		return lookAndFeelDefaults.computeIfAbsent(lookAndFeelEnabler, LookAndFeelPanel::initializeLookAndFeelDefaults);
	}

	private static Map<String, Object> initializeLookAndFeelDefaults(LookAndFeelEnabler lookAndFeelEnabler) {
		try {
			UIDefaults uiDefaults = lookAndFeelEnabler.lookAndFeel().getDefaults();
			Map<String, Object> defaults = new HashMap<>();
			defaults.put(TEXT_FIELD_FONT, uiDefaults.getFont(TEXT_FIELD_FONT));
			defaults.put(TEXT_FIELD_SELECTION_FOREGROUND, uiDefaults.getColor(TEXT_FIELD_SELECTION_FOREGROUND));
			defaults.put(TEXT_FIELD_SELECTION_BACKGROUND, uiDefaults.getColor(TEXT_FIELD_SELECTION_BACKGROUND));
			defaults.put(LABEL_FOREGROUND, uiDefaults.getColor(LABEL_FOREGROUND));
			defaults.put(LABEL_BACKGROUND, uiDefaults.getColor(LABEL_BACKGROUND));
			defaults.put(TEXT_FIELD_BACKGROUND, uiDefaults.getColor(TEXT_FIELD_BACKGROUND));
			defaults.put(PROGRESS_BAR_FOREGROUND, uiDefaults.getColor(PROGRESS_BAR_FOREGROUND));

			return defaults;
		}
		catch (RuntimeException e) {
			System.err.println("Could not initialize defaults for LookAndFeel: " +
							lookAndFeelEnabler.lookAndFeelInfo() + ": " + e.getCause().getMessage());
			return EMPTY_MAP;
		}
	}
}
