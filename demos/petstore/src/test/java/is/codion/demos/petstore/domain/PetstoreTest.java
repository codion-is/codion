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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.demos.petstore.domain;

import is.codion.framework.domain.test.DomainTest;

import org.junit.jupiter.api.Test;

import static is.codion.demos.petstore.domain.Petstore.*;

public class PetstoreTest extends DomainTest {

	public PetstoreTest() {
		super(new Petstore());
	}

	@Test
	void address() {
		test(Address.TYPE);
	}

	@Test
	void category() {
		test(Category.TYPE);
	}

	@Test
	void item() {
		test(Item.TYPE);
	}

	@Test
	void product() {
		test(Product.TYPE);
	}

	@Test
	void sellerInfo() {
		test(SellerContactInfo.TYPE);
	}

	@Test
	void tag() {
		test(Tag.TYPE);
	}

	@Test
	void tagItem() {
		test(TagItem.TYPE);
	}
}
