package terminator.model;

/**
A StyleMutator is an object capable of changing certain aspects of a style and leaving others
unchanged.  It doesn't actually mutate a Style instance, which are immutable, but produces
a modified copy.

@author Phil Norman
*/

public interface StyleMutator {
	public Style mutate(Style originalStyle);
}
