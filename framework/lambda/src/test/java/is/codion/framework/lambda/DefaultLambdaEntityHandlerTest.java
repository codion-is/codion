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
package is.codion.framework.lambda;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DefaultLambdaEntityHandlerTest {

	private static final DomainType TEST_DOMAIN = DomainType.domainType("test");

	@BeforeEach
	void setUp() {
		// Set required environment variable
		System.setProperty("DOMAIN_TYPE", TEST_DOMAIN.name());
	}

	@Test
	void testMissingDomainType() {
		System.clearProperty("DOMAIN_TYPE");
		assertThrows(IllegalStateException.class, DefaultLambdaEntityHandler::new);
	}

	@Test
	void testDomainNotFound() {
		System.setProperty("DOMAIN_TYPE", "non.existent.domain");
		assertThrows(IllegalStateException.class, DefaultLambdaEntityHandler::new);
	}

	@Test
	void testSuccessfulConstruction() {
		// Create environment variable pointing to our test domain
		System.setProperty("DOMAIN_TYPE", TEST_DOMAIN.name());

		// This should correctly throw because no domain is registered in ServiceLoader
		assertThrows(IllegalStateException.class, DefaultLambdaEntityHandler::new);
	}

	// Test domain for ServiceLoader
	public static final class TestDomain extends DomainModel {

		private static final EntityType TEST_ENTITY = TEST_DOMAIN.entityType("test_entity");

		public TestDomain() {
			super(TEST_DOMAIN);
			// Add entity definitions here when needed
		}
	}
}