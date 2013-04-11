package xdi2.core.util.iterators;

import java.util.Iterator;

/**
 * An iterator that doesn't alter any elements but casts them to a desired type.
 *  
 * @author markus
 */
public class CastingIterator<I, O> implements Iterator<O> {

	private Iterator<? extends I> iterator;
	private Class<? extends O> safeClass;

	public CastingIterator(Iterator<? extends I> iterator, Class<? extends O> safeClass) {

		this.iterator = iterator;
		this.safeClass = safeClass;
	}

	public CastingIterator(Iterator<? extends I> iterator) {

		this(iterator, null);
	}


	@Override
	public boolean hasNext() {

		return this.iterator.hasNext();
	}

	@Override
	@SuppressWarnings("unchecked")
	public O next() {

		Object next = this.iterator.next();

		if (this.getSafeClass() != null && (! this.getSafeClass().isAssignableFrom(next.getClass()))) return null;

		return (O) next;
	}

	@Override
	public void remove() {

		this.iterator.remove();
	}

	public Class<? extends O> getSafeClass() {

		return this.safeClass;
	}
}
