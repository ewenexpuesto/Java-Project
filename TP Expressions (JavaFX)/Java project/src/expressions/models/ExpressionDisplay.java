package expressions.models;

import expressions.Expression;

/**
 * A simple Data class to show {@link Expression}s in a JavaFX
 * {@link javafx.scene.control.TableView} where 1st column contains
 * the {@link Expression#toString()} and second column contains the
 * {@link Expression#value()} (if any)
 * @param <E> the type of numbers to display
 */
public class ExpressionDisplay<E extends Number> extends NamedDataDisplay<E>
{
	/**
	 * Reference to expression being displayed
	 */
	private final Expression<E> expression;

	/**
	 * Valued constructor
	 * @param expression the expression to show
	 */
	public ExpressionDisplay(Expression<E> expression)
	{
		super(expression.toString(), expression.hasValue() ? expression.value() : null);
		this.expression = expression;
	}

	/**
	 * Get the expression represented by this class
	 * @return the expression displayed  by this class
	 */
	public Expression<E> getExpression()
	{
		return expression;
	}
}
