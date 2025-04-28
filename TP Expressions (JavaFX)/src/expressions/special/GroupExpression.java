package expressions.special;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import expressions.AbstractExpression;
import expressions.Expression;

/**
 * Group Expression containing other expressions.
 * Group Expression is solely designed to act as a root node to other
 * expressions in a Tree View (swing or JavaFX)
 * @param <E> the type of numbers in expressions
 * @implNote Group expression's value has no sense and therefore the value
 * provided by {@link #value()} has no sense but whenever {@link #hasValue()} is
 * true indicates all expressions in this group have values.
 */
public class GroupExpression<E extends Number> extends AbstractExpression<E>
    implements Collection<Expression<E>>
{
	/**
	 * The collection of children expressions in this group
	 */
	Collection<Expression<E>> expressions;

	/**
	 * Default constructor
	 */
	public GroupExpression()
	{
		expressions = new ArrayList<>();
	}

	/**
	 * Copy constructor from a collection of expressions
	 * @param c the collection of expressions to copy
	 */
	public GroupExpression(Collection<? extends Expression<E>> c)
	{
		this();
		addAll(c);
	}

    /**
     * Returns the number of expressions in this group.
     * @return the number of expressions in this group
     */
	@Override
	public int size()
	{
		return expressions.size();
	}

	/**
	 * Indicate if this group is empty
	 * @return true if this group doesn't contain any expressions
	 */
	@Override
	public boolean isEmpty()
	{
		return expressions.isEmpty();
	}

	/**
	 * Numeric value of this expression
	 * @return always null since a value for a group of expressions have no sense
	 * @throws IllegalStateException if a value can't be evaluated right now
	 * @implSpec Default implementation only throws an {@link IllegalStateException}
	 * whenever {@link #hasValue()} is false
	 */
	@Override
	public E value() throws IllegalStateException
	{
		if (!hasValue())
		{
			throw new IllegalStateException("No values to provide");
		}
		return null;
	}

	/**
	 * Indicate if this expression can be evaluated right now to procude a value
	 * @return true if all expressions have a value
	 */
	@Override
	public boolean hasValue()
	{
		boolean result = true;
		for (Expression<E> expression : expressions)
		{
			result &= expression.hasValue();
		}
		return result;
	}

	/**
	 * Set parent to root node (always fails).
	 * This method always throw an {@link IllegalArgumentException} since
	 * group node are designed to be the root node of an expressions tree.
	 * @param parent to parent to (not) set.
	 * @throws IllegalArgumentException is always thrown since
	 * {@link GroupExpression} are designed to be root nodes.
	 */
	@Override
	public void setParent(Expression<E> parent) throws IllegalArgumentException
	{
		throw new IllegalArgumentException("Group nodes can't have parents");
	}

//	/**
//	 * String representation of this group of expressions
//	 * @return a String representation of this group of expressions
//	 */
//	@Override
//	public String toString()
//	{
//		StringBuilder sb = new StringBuilder();
//		for (Expression<E> expression : expressions)
//		{
//			sb.append(expression.toString());
//			sb.append('\n');
//		}
//		return sb.toString();
//	}

	/**
	 * String representation of this group of expressions.
	 * Returns an empty string in order to NOT show all expressions
	 * @return an empty string
	 */
	@Override
	public String toString()
	{
		return "Expressions";
	}

	/**
	 * Test for presence of provided expressions among children expressions
	 * @param expr the expression to search among children expressions
	 * @return true if the provided expression has been found among children
	 * expressions
	 */
	@Override
	public boolean contains(Expression<E> expr)
	{
		return expressions.contains(expr);
	}

	/**
	 * Test the presence of the provided object among children expressions
	 * @param o the object tro search
	 * @return true if object o is an {@link Expression} that has been found
	 * amon children expressions.
	 */
	@Override
	public boolean contains(Object o)
	{
		return expressions.contains(o);
	}

	/**
	 * Iterator over children expressions
	 * @return an iterator over children expressions
	 */
	@Override
	public Iterator<Expression<E>> iterator()
	{
		return expressions.iterator();
	}

	/**
	 * Convert children to array
	 * @return an array of children expressions
	 */
	@Override
	public Object[] toArray()
	{
		return expressions.toArray();
	}

	/**
	 * Convert children to array (with prototype)
	 * @param a the prototype array to store children (iff big enough)
	 * @return an array of children expressions
	 */
	@Override
	public <T> T[] toArray(T[] a)
	{
		return expressions.toArray(a);
	}

	/**
	 * Add an expression to the children expressions
	 * @param e the expression to add
	 * @return true if the provided expression has been added to childrens
	 * expressions
	 * @throws NullPointerException if the provided expression is null since we
	 * don't allow null expressions
	 * @throws ClassCastException if the provided expression is also a
	 * {@link GroupExpression}.
	 */
	@Override
	public boolean add(Expression<E> e)
	    throws NullPointerException,
	    ClassCastException
	{
		Objects.requireNonNull(e);
		if (e instanceof GroupExpression<?>)
		{
			throw new ClassCastException("child expression can't be group");
		}
		boolean added = expressions.add(e);
		if (added)
		{
			e.setParent(this);
		}
		return added;
	}

	/**
	 * Removes the provided object from children expressions
	 * @para o the object to remove from children expressions
	 * @return true if the provided object was an expression found among
	 * children expressions and has been removed
	 */
	@Override
	public boolean remove(Object o)
	{
		boolean removed = expressions.remove(o);
		if (removed)
		{
			Expression<?> expr = (Expression<?>) o;
			expr.setParent(null);
		}
		return removed;
	}

	/**
	 * Check containment of a collection of object
	 * @param c the collection of objects to search
	 * @return true if all aobjects in the provided collection have been found
	 * in the children expressions
	 */
	@Override
	public boolean containsAll(Collection<?> c)
	{
		return expressions.containsAll(c);
	}

	/**
	 * Add a collection of expressions to this group
	 * @param c the collection of expressions to add to the children expressions
	 * @return true if the children expressions has been modified, false otherwise.
	 * @throws NullPointerException if the provided collection is null
	 * @throws NullPointerException if one the provided expressions is null
	 * @throws ClassCastException if one of the provided expressions can't be added.
	 */
	@Override
	public boolean addAll(Collection<? extends Expression<E>> c)
	{
		Objects.requireNonNull(c);
		boolean modified = false;
		for (Expression<E> expr : c)
		{
			modified |= add(expr);
		}
		return modified;
	}

	/**
	 * Removes a collection of expressions from this group
	 * @param c the collection of expressions to remove from the children expressions.
	 * @return true if the children expressions has been modified, false otherwise.
	 */
	@Override
	public boolean removeAll(Collection<?> c)
	{
		boolean removed = false;
		if (c != null)
		{
			for (Object o : c)
			{
				removed |= remove(o);
			}
		}
		return removed;
	}

	/**
	 * Retain in children expressions only the expressions found in provided
	 * collection.
	 * @param c a collection of objects to retain in children expressions
	 * @return true if the children expressions has been modified.
	 */
	@Override
	public boolean retainAll(Collection<?> c)
	{
		return expressions.retainAll(c);
	}

	/**
	 * Removes all expressions from children expressions
	 */
	@Override
	public void clear()
	{
		for (Iterator<Expression<E>> it = expressions.iterator(); it.hasNext();)
		{
			Expression<E> expr = it.next();
			it.remove();
			expr.setParent(null);
		}
	}


}
