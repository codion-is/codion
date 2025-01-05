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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.laf;

import javax.swing.LookAndFeel;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static is.codion.swing.common.ui.laf.DefaultLookAndFeelEnabler.LOOK_AND_FEEL_PROVIDERS;
import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.lookAndFeelEnabler;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

/**
 * Provides one or more look and feel enablers.
 * @see LookAndFeelEnabler
 */
public interface LookAndFeelProvider {

	/**
	 * @return the available {@link LookAndFeelEnabler}s
	 */
	Collection<LookAndFeelEnabler> get();

	/**
	 * @return all {@link LookAndFeelProvider} registered with the {@link ServiceLoader}
	 */
	static Collection<LookAndFeelProvider> instances() {
		return stream(ServiceLoader.load(LookAndFeelProvider.class).spliterator(), false)
						.collect(Collectors.toList());
	}

	/**
	 * Adds a new look and feel provider.
	 * @param lookAndFeelInfo the look and feel info
	 */
	static void addLookAndFeel(LookAndFeelInfo lookAndFeelInfo) {
		addLookAndFeel(lookAndFeelEnabler(lookAndFeelInfo));
	}

	/**
	 * Adds a new look and feel provider.
	 * @param lookAndFeelInfo the look and feel info
	 * @param enabler configures and enables this look and feel
	 */
	static void addLookAndFeel(LookAndFeelInfo lookAndFeelInfo, Consumer<LookAndFeelInfo> enabler) {
		addLookAndFeel(lookAndFeelEnabler(lookAndFeelInfo, enabler));
	}

	/**
	 * Adds the given look and feel provider.
	 * Note that this replaces any existing look and feel provider based on the same classname.
	 * @param lookAndFeelEnabler the look and feel provider to add
	 */
	static void addLookAndFeel(LookAndFeelEnabler lookAndFeelEnabler) {
		LOOK_AND_FEEL_PROVIDERS
						.put(requireNonNull(lookAndFeelEnabler).lookAndFeelInfo().getClassName(), lookAndFeelEnabler);
	}

	/**
	 * @return the available {@link LookAndFeelEnabler}s
	 * @see #addLookAndFeel(LookAndFeelEnabler)
	 */
	static Collection<LookAndFeelEnabler> lookAndFeels() {
		return unmodifiableCollection(new ArrayList<>(LOOK_AND_FEEL_PROVIDERS.values()));
	}

	/**
	 * Returns a look and feel provider for the given class, if available
	 * @param clazz the look and feel class
	 * @return a look and feel provider, an empty Optional if not found
	 */
	static Optional<LookAndFeelEnabler> findLookAndFeel(Class<? extends LookAndFeel> clazz) {
		return findLookAndFeel(requireNonNull(clazz).getName());
	}

	/**
	 * Returns a look and feel provider with the given classname, if available
	 * @param className the look and feel classname
	 * @return a look and feel provider, an empty Optional if not found
	 */
	static Optional<LookAndFeelEnabler> findLookAndFeel(String className) {
		return className == null ? Optional.empty() : Optional.ofNullable(LOOK_AND_FEEL_PROVIDERS.get(className));
	}
}