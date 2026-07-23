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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.laf;

import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.scaler.Scaler;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultLookAndFeelEnabler implements LookAndFeelEnabler {

	private static final Consumer<LookAndFeelInfo> DEFAULT_ENABLER = new DefaultEnabler();

	static final Map<String, LookAndFeelEnabler> LOOK_AND_FEEL_PROVIDERS = new HashMap<>();

	static {
		LookAndFeelProvider.instances().forEach(providers ->
						providers.get().forEach(lookAndFeelEnabler -> LOOK_AND_FEEL_PROVIDERS
										.put(requireNonNull(lookAndFeelEnabler).lookAndFeelInfo().getClassName(), lookAndFeelEnabler)));
	}

	private final LookAndFeelInfo lookAndFeelInfo;
	private final Consumer<LookAndFeelInfo> enabler;
	private final boolean installed;

	DefaultLookAndFeelEnabler(LookAndFeelInfo lookAndFeelInfo) {
		this(lookAndFeelInfo, DEFAULT_ENABLER, false);
	}

	DefaultLookAndFeelEnabler(LookAndFeelInfo lookAndFeelInfo, Consumer<LookAndFeelInfo> enabler) {
		this(lookAndFeelInfo, enabler, false);
	}

	DefaultLookAndFeelEnabler(LookAndFeelInfo lookAndFeelInfo, boolean installed) {
		this(lookAndFeelInfo, DEFAULT_ENABLER, installed);
	}

	private DefaultLookAndFeelEnabler(LookAndFeelInfo lookAndFeelInfo, Consumer<LookAndFeelInfo> enabler, boolean installed) {
		this.lookAndFeelInfo = requireNonNull(lookAndFeelInfo);
		this.enabler = requireNonNull(enabler);
		this.installed = installed;
	}

	@Override
	public LookAndFeelInfo lookAndFeelInfo() {
		return lookAndFeelInfo;
	}

	@Override
	public void enable() {
		enabler.accept(lookAndFeelInfo);
	}

	@Override
	public boolean installed() {
		return installed;
	}

	@Override
	public LookAndFeel lookAndFeel() {
		try {
			return (LookAndFeel) Class.forName(lookAndFeelInfo.getClassName()).getDeclaredConstructor().newInstance();
		}
		catch (Exception e) {
			throw Exceptions.runtime(e);
		}
	}

	@Override
	public String toString() {
		return lookAndFeelInfo.getName();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DefaultLookAndFeelEnabler)) {
			return false;
		}

		DefaultLookAndFeelEnabler that = (DefaultLookAndFeelEnabler) obj;

		return lookAndFeelInfo.getClassName().equals(that.lookAndFeelInfo.getClassName());
	}

	@Override
	public int hashCode() {
		return lookAndFeelInfo().getClassName().hashCode();
	}

	private static final class DefaultEnabler implements Consumer<LookAndFeelInfo> {

		@Override
		public void accept(LookAndFeelInfo lookAndFeelInfo) {
			try {
				UIManager.setLookAndFeel(requireNonNull(lookAndFeelInfo).getClassName());
				Scaler.instance(lookAndFeelInfo.getClassName()).ifPresent(Scaler::apply);
				Utilities.updateComponentTreeForAllWindows();
			}
			catch (Exception e) {
				throw Exceptions.runtime(e);
			}
		}
	}
}
