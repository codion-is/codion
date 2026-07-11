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
package is.codion.framework.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static java.util.stream.Collectors.*;

/**
 * Resolves the service loaded {@link Domain} implementations to one per {@link DomainType},
 * the most derived implementation winning among multiple implementations of one type.
 * @see Domain#domains()
 */
final class DomainResolver {

	private static final Logger LOG = LoggerFactory.getLogger(DomainResolver.class);

	private DomainResolver() {}

	static List<Domain> resolve(List<Domain> loaded) {
		return loaded.stream()
						.collect(groupingBy(Domain::type, LinkedHashMap::new, toList()))
						.values().stream()
						.map(DomainResolver::mostDerived)
						.collect(collectingAndThen(toList(), Collections::unmodifiableList));
	}

	private static Domain mostDerived(List<Domain> candidates) {
		if (candidates.size() == 1) {
			return candidates.get(0);
		}
		//a candidate wins if every candidate's class is a supertype of it, that is it is the deepest subclass;
		//the winners always share one class, two distinct winners would each subclass the other, impossible
		List<Domain> derived = candidates.stream()
						.filter(candidate -> candidates.stream()
										.allMatch(other -> other.getClass().isAssignableFrom(candidate.getClass())))
						.collect(toList());
		if (derived.isEmpty()) {
			throw new IllegalStateException(unrelated(candidates));
		}
		Domain winner = derived.get(0);
		logResolution(winner, candidates);

		return winner;
	}

	private static void logResolution(Domain winner, List<Domain> candidates) {
		List<String> superseded = candidates.stream()
						.map(candidate -> candidate.getClass().getName())
						.filter(name -> !name.equals(winner.getClass().getName()))
						.distinct()
						.collect(toList());
		if (superseded.isEmpty()) {
			LOG.debug("Multiple identical implementations of domain {} on the classpath: {}",
							winner.type().name(), winner.getClass().getName());
		}
		else {
			LOG.info("Hosting {} for domain {} (extends {})",
							winner.getClass().getName(), winner.type().name(), String.join(", ", superseded));
		}
	}

	private static String unrelated(List<Domain> candidates) {
		return "Multiple unrelated implementations of domain " + candidates.get(0).type().name()
						+ " on the classpath: "
						+ candidates.stream()
						.map(candidate -> candidate.getClass().getName())
						.distinct()
						.sorted()
						.collect(joining(", "))
						+ " - provide a single implementation extending the others, or remove all but one from the classpath";
	}
}
