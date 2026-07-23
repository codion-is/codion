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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.flatlaf.themes;

import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.scaler.Scaler;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.lookAndFeelEnabler;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Provides all available Flat Look and Feels
 */
public final class FlatLookAndFeelThemes {

	private static final Consumer<LookAndFeelInfo> ENABLER = new DefaultEnabler();

	private static final Collection<LookAndFeelEnabler> LOOK_AND_FEELS = unmodifiableList(asList(
					lookAndFeelEnabler(new LookAndFeelInfo("Flat Darcula ", FlatDarculaLaf.class.getName()), ENABLER),
					lookAndFeelEnabler(new LookAndFeelInfo("Flat Dark", FlatDarkLaf.class.getName()), ENABLER),
					lookAndFeelEnabler(new LookAndFeelInfo("Flat IntelliJ", FlatIntelliJLaf.class.getName()), ENABLER),
					lookAndFeelEnabler(new LookAndFeelInfo("Flat Light", FlatLightLaf.class.getName()), ENABLER),
					lookAndFeelEnabler(new LookAndFeelInfo("Flat Mac Dark", FlatMacDarkLaf.class.getName()), ENABLER),
					lookAndFeelEnabler(new LookAndFeelInfo("Flat Mac Light", FlatMacLightLaf.class.getName()), ENABLER)
	));

	private FlatLookAndFeelThemes() {}

	/**
	 * Registers the IntelliJ themes, making them available via {@link LookAndFeelProvider}, for example
	 * to a {@link is.codion.swing.common.ui.laf.LookAndFeelComboBox}. Call once during application startup.
	 */
	public static void addAll() {
		add(info -> true);
	}

	/**
	 * Registers the IntelliJ themes, making them available via {@link LookAndFeelProvider}, for example
	 * to a {@link is.codion.swing.common.ui.laf.LookAndFeelComboBox}. Call once during application startup.
	 * @param include controls which look and feels to include
	 */
	public static void add(Predicate<LookAndFeelInfo> include) {
		requireNonNull(include);
		LOOK_AND_FEELS.stream()
						.filter(enabler -> include.test(enabler.lookAndFeelInfo()))
						.forEach(LookAndFeelProvider::addLookAndFeel);
	}

	/**
	 * Requrired since scaling must happen before the look and feel is applied.
	 */
	private static final class DefaultEnabler implements Consumer<LookAndFeelInfo> {

		@Override
		public void accept(LookAndFeelInfo lookAndFeelInfo) {
			try {
				Scaler.instance(requireNonNull(lookAndFeelInfo).getClassName()).ifPresent(Scaler::apply);
				UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
				Utilities.updateComponentTreeForAllWindows();
			}
			catch (Exception e) {
				throw Exceptions.runtime(e);
			}
		}
	}
}
