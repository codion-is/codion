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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.laf;

import is.codion.common.Configuration;
import is.codion.common.model.UserPreferences;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.Utilities;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.Window;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Provides a LookAndFeel implementation.
 */
public interface LookAndFeelProvider {

	/**
	 * Specifies whether the cross platform Look and Feel should be available for selection.<br>
	 * Value type: Boolean<br>
	 * Default value: true
	 */
	PropertyValue<Boolean> CROSS_PLATFORM = Configuration.booleanValue("is.codion.swing.common.ui.laf.LookAndFeelProvider.crossPlatform", true);

	/**
	 * Specifies whether the cross platform Look and Feel should be available for selection.<br>
	 * Value type: Boolean<br>
	 * Default value: true
	 */
	PropertyValue<Boolean> SYSTEM = Configuration.booleanValue("is.codion.swing.common.ui.laf.LookAndFeelProvider.system", true);

	/**
	 * @return the look and feel info
	 */
	LookAndFeelInfo lookAndFeelInfo();

	/**
	 * Configures and enables this LookAndFeel.
	 */
	void enable();

	/**
	 * @return the LookAndFeel instance represented by this provider
	 * @throws Exception in case an instance could not be created
	 */
	LookAndFeel lookAndFeel() throws Exception;

	/**
	 * Instantiates a new LookAndFeelProvider, using {@link UIManager#setLookAndFeel(String)} to enable.
	 * @param lookAndFeelInfo the look and feel info
	 * @return a look and feel provider
	 */
	static LookAndFeelProvider lookAndFeelProvider(LookAndFeelInfo lookAndFeelInfo) {
		return new DefaultLookAndFeelProvider(lookAndFeelInfo);
	}

	/**
	 * Instantiates a new LookAndFeelProvider.
	 * @param lookAndFeelInfo the look and feel info
	 * @param enabler configures and enables this look and feel
	 * @return a look and feel provider
	 */
	static LookAndFeelProvider lookAndFeelProvider(LookAndFeelInfo lookAndFeelInfo, Consumer<LookAndFeelInfo> enabler) {
		return new DefaultLookAndFeelProvider(lookAndFeelInfo, enabler);
	}

	/**
	 * Adds a new look and feel provider.
	 * @param lookAndFeelInfo the look and feel info
	 */
	static void addLookAndFeel(LookAndFeelInfo lookAndFeelInfo) {
		addLookAndFeel(lookAndFeelProvider(lookAndFeelInfo));
	}

	/**
	 * Adds a new look and feel provider.
	 * @param lookAndFeelInfo the look and feel info
	 * @param enabler configures and enables this look and feel
	 */
	static void addLookAndFeel(LookAndFeelInfo lookAndFeelInfo, Consumer<LookAndFeelInfo> enabler) {
		addLookAndFeel(lookAndFeelProvider(lookAndFeelInfo, enabler));
	}

	/**
	 * Adds the given look and feel provider.
	 * Note that this replaces any existing look and feel provider based on the same classname.
	 * @param lookAndFeelProvider the look and feel provider to add
	 */
	static void addLookAndFeel(LookAndFeelProvider lookAndFeelProvider) {
		DefaultLookAndFeelProvider.LOOK_AND_FEEL_PROVIDERS
						.put(requireNonNull(lookAndFeelProvider).lookAndFeelInfo().getClassName(), lookAndFeelProvider);
	}

	/**
	 * @return the available {@link LookAndFeelProvider}s
	 * @see #addLookAndFeel(LookAndFeelProvider)
	 */
	static Map<String, LookAndFeelProvider> lookAndFeelProviders() {
		return Collections.unmodifiableMap(DefaultLookAndFeelProvider.LOOK_AND_FEEL_PROVIDERS);
	}

	/**
	 * Returns a look and feel provider with the given classname, if available
	 * @param className the look and feel classname
	 * @return a look and feel provider, an empty Optional if not found
	 */
	static Optional<LookAndFeelProvider> findLookAndFeelProvider(String className) {
		return className == null ? Optional.empty() : Optional.ofNullable(DefaultLookAndFeelProvider.LOOK_AND_FEEL_PROVIDERS.get(className));
	}

	/**
	 * Returns the look and feel specified by the given user preference or the system look and feel if no preference value is found.
	 * @param userPreferencePropertyName the name of the user preference look and feel property
	 * @return the look and feel specified by user preference or the default system look and feel
	 */
	static String defaultLookAndFeelName(String userPreferencePropertyName) {
		return UserPreferences.getUserPreference(userPreferencePropertyName, Utilities.systemLookAndFeelClassName());
	}

	/**
	 * Enables the given look and feel and updates all window component trees.
	 * @param lookAndFeelProvider the look and feel provider to enable
	 */
	static void enableLookAndFeel(LookAndFeelProvider lookAndFeelProvider) {
		requireNonNull(lookAndFeelProvider).enable();
		for (Window window : Window.getWindows()) {
			SwingUtilities.updateComponentTreeUI(window);
		}
	}
}