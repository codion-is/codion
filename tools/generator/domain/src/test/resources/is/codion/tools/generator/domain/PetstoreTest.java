package is.codion.petstore.domain;

import static is.codion.petstore.domain.Petstore.*;

import is.codion.framework.domain.test.DomainTest;
import org.junit.jupiter.api.Test;

public final class PetstoreTest extends DomainTest {
	public PetstoreTest() {
		super(new Petstore());
	}

	@Test
	public void address() {
		test(Address.TYPE);
	}

	@Test
	public void category() {
		test(Category.TYPE);
	}

	@Test
	public void contactInfo() {
		test(ContactInfo.TYPE);
	}

	@Test
	public void itemTagsView() {
		test(ItemTagsView.TYPE);
	}

	@Test
	public void tag() {
		test(Tag.TYPE);
	}

	@Test
	public void product() {
		test(Product.TYPE);
	}

	@Test
	public void item() {
		test(Item.TYPE);
	}

	@Test
	public void tagItem() {
		test(TagItem.TYPE);
	}
}
