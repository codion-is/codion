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

import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.entity.Entities;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.*;

public final class DomainResolverTest {

	private static final DomainType TYPE = DomainType.domainType("DomainResolverTest");
	private static final DomainType OTHER = DomainType.domainType("DomainResolverTestOther");

	@Test
	void single() {
		Domain base = new Base();
		assertSame(base, DomainResolver.resolve(List.of(base)).get(0));
	}

	@Test
	void subclassWinsRegardlessOfOrder() {
		//the extension registers the superset, it must win no matter the ServiceLoader iteration order
		Base base = new Base();
		Extended extended = new Extended();
		assertSame(extended, resolveSingle(asList(base, extended)));
		assertSame(extended, resolveSingle(asList(extended, base)));
	}

	@Test
	void deepestWins() {
		assertInstanceOf(DeeperExtended.class,
						resolveSingle(asList(new Base(), new DeeperExtended(), new Extended())));
	}

	@Test
	void identicalImplementationsResolve() {
		//a jar present twice, either instance, no throw
		assertInstanceOf(Extended.class, resolveSingle(asList(new Extended(), new Extended())));
	}

	@Test
	void unrelatedImplementationsThrow() {
		IllegalStateException exception = assertThrows(IllegalStateException.class,
						() -> DomainResolver.resolve(asList(new SiblingA(), new SiblingB())));
		String message = exception.getMessage();
		assertTrue(message.contains(TYPE.name()), message);
		assertTrue(message.contains(SiblingA.class.getName()), message);
		assertTrue(message.contains(SiblingB.class.getName()), message);
		assertTrue(message.contains("remove all but one from the classpath"), message);
	}

	@Test
	void diamondThrows() {
		//base plus two independent extensions of it, no unique most derived
		assertThrows(IllegalStateException.class,
						() -> DomainResolver.resolve(asList(new Base(), new Extended(), new SiblingOfExtended())));
	}

	@Test
	void winnerPerType() {
		//grouping keeps the types apart, each resolved on its own
		List<Domain> resolved = DomainResolver.resolve(asList(new Base(), new Extended(), new Other()));
		assertEquals(2, resolved.size());
		assertTrue(resolved.stream().anyMatch(Extended.class::isInstance));
		assertTrue(resolved.stream().anyMatch(Other.class::isInstance));
		assertFalse(resolved.stream().anyMatch(domain -> domain.getClass() == Base.class));
	}

	private static Domain resolveSingle(List<Domain> candidates) {
		List<Domain> resolved = DomainResolver.resolve(candidates);
		assertEquals(1, resolved.size());

		return resolved.get(0);
	}

	private abstract static class StubDomain implements Domain {

		private final DomainType type;

		private StubDomain(DomainType type) {
			this.type = type;
		}

		@Override
		public final DomainType type() {
			return type;
		}

		@Override
		public Entities entities() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<ReportType<?, ?>, Report<?, ?, ?>> reports() {
			return emptyMap();
		}

		@Override
		public Map<ProcedureType<?, ?>, DatabaseProcedure<?, ?>> procedures() {
			return emptyMap();
		}

		@Override
		public Map<FunctionType<?, ?, ?>, DatabaseFunction<?, ?, ?>> functions() {
			return emptyMap();
		}

		@Override
		public <P, R> Report<?, P, R> report(ReportType<P, R> reportType) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <C, P> DatabaseProcedure<C, P> procedure(ProcedureType<C, P> procedureType) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <C, P, R> DatabaseFunction<C, P, R> function(FunctionType<C, P, R> functionType) {
			throw new UnsupportedOperationException();
		}
	}

	private static class Base extends StubDomain {
		private Base() {
			super(TYPE);
		}
	}

	private static class Extended extends Base {}

	private static class DeeperExtended extends Extended {}

	private static final class SiblingOfExtended extends Base {}

	private static final class SiblingA extends StubDomain {
		private SiblingA() {
			super(TYPE);
		}
	}

	private static final class SiblingB extends StubDomain {
		private SiblingB() {
			super(TYPE);
		}
	}

	private static final class Other extends StubDomain {
		private Other() {
			super(OTHER);
		}
	}
}
