package expressions.terminal;

/**
 * Constant expression.
 * Constant expressions can always be evaluated since they are created directly
 * with a specified value
 * @param <E> The type of numbers in this expression
 * @author davidroussel
 */
public class ConstantExpression<E extends Number> extends TerminalExpression<E>
{
	/*
	 * There is no default constructor to enforce the fact that all
	 * constants must have a value.
	 */

	/**
	 * Valued construtor.
	 * @param value the value to provide to this constant expression
	 * @throws NullPointerException if we try to set a null value since constant
	 * expression must have a value
	 */
	public ConstantExpression(E value) throws NullPointerException
	{
		// TODO Complete ...
	}

	/**
	 * Factory method to get a valued constant
	 * @param <E> the type of number to use
	 * @param value the constant value to set in this expression
	 * @return a {@link ConstantExpression} which value is 0
	 * @throws NullPointerException if provided value is null
	 */
	public static <E extends Number> ConstantExpression<E> getConstant(E value)
	{
		return new ConstantExpression<E>(value);
	}

	/**
	 * String representation of this constant expression
	 * @return the toString() of its value.
	 */
	@Override
	public String toString()
	{
		// TODO Replace with correct implementation
		return null;
	}
}
