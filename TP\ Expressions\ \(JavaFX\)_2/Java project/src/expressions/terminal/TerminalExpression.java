package expressions.terminal;

import java.util.Optional;

import expressions.AbstractExpression;
import expressions.Expression;

/**
 * Base class for all terminal expressions such as constants (5) or variables (a)
 * holding a value.
 * @param <E> The type of numbers in this expression
 * @author davidroussel
 */
public abstract class TerminalExpression<E extends Number> extends AbstractExpression<E>
{
	/**
	 * The value hold by this terminal expression.
	 */
	protected Optional<E> value;


	/**
	 * Default protected constructor
	 * Designed to be used only in subclasses
	 */
	protected TerminalExpression()
	{
		value = null;
	}

	/**
	 * Valued constructor with a provided value (which can be null);
	 * @param value the value to provide to this terminal expression
	 * @implSpec If provided value is null then {@link Optional} {@link #value}
	 * will be empty.
	 * @see Optional#of(Object)
	 */
	protected TerminalExpression(E value)
	{
		if (value == null)
		{
			this.value = Optional.empty();
		}
		else
		{
			this.value = Optional.of(value);
		}
	}

	/**
	 * Numeric value of this expression
	 * @return the numeric value of this expression
	 * @throws IllegalStateException if a value can't be evaluated right now
	 * @see Optional#get()
	 */
	@Override
	public E value() throws IllegalStateException
	{
		if (value.isEmpty())
		{
			throw new IllegalStateException("No value yet");
		}
		return value.get();
	}

	/**
	 * Indicate if this expression can be evaluated right now to procude a value
	 * @return true if expression can produce a value
	 * and calling {@link #value()} is legal. False otherwise
	 * @see Optional#isPresent()
	 */
	@Override
	public boolean hasValue()
	{
		return value.isPresent();
	}

	/**
	 * Set new parent to expression.
	 * @param parent The parent to set
	 * @throws IllegalArgumentException if the provided parent is also a
	 * {@link TerminalExpression} since {@link TerminalExpression}s can't have
	 * childrens.
	 */
	@Override
	public void setParent(Expression<E> parent) throws IllegalArgumentException
	{
		if (parent instanceof TerminalExpression<?>)
		{
			throw new IllegalArgumentException("parent is terminal expression");
		}
		this.parent = parent;
	}
}
