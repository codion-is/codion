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
package is.codion.swing.common.ui.laf;

import is.codion.swing.common.ui.Utilities;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Optional;
import java.util.function.Consumer;

import static is.codion.common.model.UserPreferences.getUserPreference;
import static is.codion.swing.common.ui.Utilities.systemLookAndFeelClassName;
import static java.util.Objects.requireNonNull;

/**
 * Provides and enables a {@link LookAndFeel} implementation.
 */
public interface LookAndFeelEnabler {

	/**
	 * @return the look and feel info
	 */
	LookAndFeelInfo lookAndFeelInfo();

	/**
	 * Configures and enables this LookAndFeel.
	 */
	void enable();

	/**
	 * @return a new instance of the {@link LookAndFeel} represented by this provider
	 * @throws RuntimeException in case the class is not found or if the {@link LookAndFeel} could not be instantiated
	 */
	LookAndFeel lookAndFeel();

	/**
	 * Instantiates a new {@link LookAndFeelEnabler}, using {@link UIManager#setLookAndFeel(String)} to enable.
	 * @param lookAndFeelInfo the look and feel info
	 * @return a look and feel provider
	 */
	static LookAndFeelEnabler lookAndFeelEnabler(LookAndFeelInfo lookAndFeelInfo) {
		return new DefaultLookAndFeelEnabler(lookAndFeelInfo);
	}

	/**
	 * Instantiates a new {@link LookAndFeelEnabler}.
	 * <p>The {@code enabler} is responsible for configuring and enabling the look and feel as well as updating the component
	 * tree of all application windows, for example by calling {@link Utilities#updateComponentTreeForAllWindows()}
	 * @param lookAndFeelInfo the look and feel info
	 * @param enabler configures and enables the look and feel as well as updates the component tree of all application windows
	 * @return a look and feel provider
	 */
	static LookAndFeelEnabler lookAndFeelEnabler(LookAndFeelInfo lookAndFeelInfo, Consumer<LookAndFeelInfo> enabler) {
		return new DefaultLookAndFeelEnabler(lookAndFeelInfo, enabler);
	}

	/**
	 * Enables the look and feel specified by the given user preference or the default system look and feel if no preference value is found.
	 * @param userPreferencePropertyName the name of the user preference look and feel property
	 */
	static void enableLookAndFeel(String userPreferencePropertyName) {
		enableLookAndFeel(userPreferencePropertyName, systemLookAndFeelClassName());
	}

	/**
	 * Enables the look and feel specified by the given user preference or the default one if no preference value is found.
	 * @param userPreferencePropertyName the name of the user preference look and feel property
	 * @param defaultLookAndFeel the default look and feel class to use if none is found in user preferences
	 */
	static void enableLookAndFeel(String userPreferencePropertyName, Class<? extends LookAndFeel> defaultLookAndFeel) {
		enableLookAndFeel(userPreferencePropertyName, requireNonNull(defaultLookAndFeel).getName());
	}

	/**
	 * Enables the look and feel specified by the given user preference or the default one if no preference value is found.
	 * @param userPreferencePropertyName the name of the user preference look and feel property
	 * @param defaultLookAndFeel the classname of the default look and feel to use if none is found in user preferences
	 */
	static void enableLookAndFeel(String userPreferencePropertyName, String defaultLookAndFeel) {
		Optional.ofNullable(getUserPreference(userPreferencePropertyName, defaultLookAndFeel))
						.map(LookAndFeelProvider::findLookAndFeel)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.ifPresent(LookAndFeelEnabler::enable);
	}
}