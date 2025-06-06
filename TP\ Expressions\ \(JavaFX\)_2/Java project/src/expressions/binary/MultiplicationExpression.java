package expressions.binary;

import expressions.Expression;

/**
 * Multiplication of two expressions
 * @param <E> The type of numbers used in this expression
 */
public class MultiplicationExpression<E extends Number> extends BinaryExpression<E>
{
	/**
	 * Valued consrtructor
	 * @param left the left side of the expression
	 * @param right the right side of the expression
	 */
	public MultiplicationExpression(Expression<E> left,
	                                Expression<E> right)
	{
		super(left, right, BinaryOperatorRules.MULTIPLICATION);
	}

	/**
	 * Default constructor
	 */
	public MultiplicationExpression()
	{
		this(null, null);
	}

	/**
	 * Operate the concrete addition of operands values
	 * @param value1 first operand's value
	 * @param value2 second operand's value
	 * @return the actual value resulting from this binary expression
	 * or throws an exception if the operation can't be performed
	 * @throws UnsupportedOperationException if the type E of the operands
	 * is not one of {@link Integer}, {@link Float} or {@link Double}
	 * @implSpec it is assumed only value1 is tested to check for either
	 * {@link Integer}, {@link Float} or {@link Double}.
	 * @see Number#intValue()
	 * @see Number#floatValue()
	 * @see Number#doubleValue()
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected E operate(E value1, E value2) throws UnsupportedOperationException
	{
		/*
		 * DONE Update to match multiplication operations
		 */
		if (value1 instanceof Integer)
		{
			return (E) Integer.valueOf(value1.intValue() * value2.intValue());
		}
		if (value1 instanceof Float)
		{
			return (E) Float.valueOf(value1.floatValue() * value2.floatValue());
		}
		if (value1 instanceof Double)
		{
			return (E) Double.valueOf(value1.doubleValue() * value2.doubleValue());
		}
		throw new UnsupportedOperationException("Unknown Number type "
		    + value1.getClass().getSimpleName());
	}
}
