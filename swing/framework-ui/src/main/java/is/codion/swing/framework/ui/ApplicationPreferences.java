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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Text;
import is.codion.common.model.UserPreferences;

import org.json.JSONObject;

import java.awt.Dimension;

import static is.codion.common.model.UserPreferences.getUserPreference;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

final class ApplicationPreferences {

	private static final String LEGACY_DEFAULT_USERNAME_PROPERTY = "is.codion.swing.framework.ui.defaultUsername";
	private static final String LEGACY_LOOK_AND_FEEL_PROPERTY = "is.codion.swing.framework.ui.LookAndFeel";
	private static final String LEGACY_FONT_SIZE_PROPERTY = "is.codion.swing.framework.ui.FontSize";
	private static final String LEGACY_FRAME_SIZE_PROPERTY = "is.codion.swing.framework.ui.frameSize";
	private static final String LEGACY_FRAME_MAXIMIZED_PROPERTY = "is.codion.swing.framework.ui.maximized";

	private static final String PREFERENCES_KEY = "#preferences";

	private static final String DEFAULT_USERNAME_KEY = "defaultUsername";
	private static final String LOOK_AND_FEEL_KEY = "lookAndFeel";
	private static final String FONT_SIZE_KEY = "fontSize";
	private static final String FRAME_SIZE_KEY = "frameSize";
	private static final String FRAME_MAXIMIZED_KEY = "maximized";

	private final String defaultUsername;
	private final String lookAndFeel;
	private final int fontSize;
	private final Dimension frameSize;
	private final boolean frameMaximized;

	ApplicationPreferences(String defaultUsername, String lookAndFeel, int fontSize,
												 Dimension frameSize, boolean frameMaximized) {
		this.defaultUsername = defaultUsername;
		this.lookAndFeel = lookAndFeel;
		this.fontSize = fontSize;
		this.frameSize = frameSize;
		this.frameMaximized = frameMaximized;
	}

	String defaultUsername() {
		return defaultUsername;
	}

	String lookAndFeel() {
		return lookAndFeel;
	}

	int fontSize() {
		return fontSize;
	}

	Dimension frameSize() {
		return frameSize;
	}

	boolean frameMaximized() {
		return frameMaximized;
	}

	void save(Class<?> applicationClassName) {
		UserPreferences.setUserPreference(applicationClassName.getName() + PREFERENCES_KEY, toJSONObject().toString());
	}

	private JSONObject toJSONObject() {
		JSONObject preferences = new JSONObject();
		preferences.put(DEFAULT_USERNAME_KEY, defaultUsername);
		preferences.put(LOOK_AND_FEEL_KEY, lookAndFeel);
		preferences.put(FONT_SIZE_KEY, fontSize);
		preferences.put(FRAME_SIZE_KEY, frameSize.width + "x" + frameSize.height);
		preferences.put(FRAME_MAXIMIZED_KEY, frameMaximized);

		return preferences;
	}

	static ApplicationPreferences load(Class<?> applicationClass) {
		String preferences = getUserPreference(applicationClass.getName() + PREFERENCES_KEY, "");
		if (preferences.isEmpty()) {
			String applicationDefaultUsernameProperty = LEGACY_DEFAULT_USERNAME_PROPERTY + "#" + applicationClass.getSimpleName();
			String applicationLookAndFeelProperty = LEGACY_LOOK_AND_FEEL_PROPERTY + "#" + applicationClass.getSimpleName();
			String applicationFontSizeProperty = LEGACY_FONT_SIZE_PROPERTY + "#" + applicationClass.getSimpleName();
			String applicationFrameSizeProperty = LEGACY_FRAME_SIZE_PROPERTY + "#" + applicationClass.getSimpleName();
			String applicationFrameMaximizedProperty = LEGACY_FRAME_MAXIMIZED_PROPERTY + "#" + applicationClass.getSimpleName();

			return new ApplicationPreferences(
							getUserPreference(applicationDefaultUsernameProperty, System.getProperty("user.name")),
							getUserPreference(applicationLookAndFeelProperty, null),
							parseInt(getUserPreference(applicationFontSizeProperty, "100")),
							parseFrameSize(getUserPreference(applicationFrameSizeProperty, "")),
							parseBoolean(getUserPreference(applicationFrameMaximizedProperty, "false")));
		}

		return fromString(preferences);
	}

	private static ApplicationPreferences fromString(String preferences) {
		JSONObject jsonObject = new JSONObject(preferences);

		return new ApplicationPreferences(
						jsonObject.has(DEFAULT_USERNAME_KEY) ? jsonObject.getString(DEFAULT_USERNAME_KEY) : null,
						jsonObject.has(LOOK_AND_FEEL_KEY) ? jsonObject.getString(LOOK_AND_FEEL_KEY) : null,
						jsonObject.has(FONT_SIZE_KEY) ? jsonObject.getInt(FONT_SIZE_KEY) : 100,
						jsonObject.has(FRAME_SIZE_KEY) ? parseFrameSize(jsonObject.getString(FRAME_SIZE_KEY)) : null,
						jsonObject.has(FONT_SIZE_KEY) && jsonObject.getBoolean(FRAME_MAXIMIZED_KEY));
	}

	private static Dimension parseFrameSize(String userPreference) {
		if (Text.nullOrEmpty(userPreference)) {
			return null;
		}

		String[] split = userPreference.split("x");

		return new Dimension(parseInt(split[0]), parseInt(split[1]));
	}
}
