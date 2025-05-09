package expressions.binary;

import expressions.AbstractExpression;
import expressions.Expression;
import expressions.terminal.TerminalExpression;

/**
 * Binary expression to evaluate an operator between two expressions (i.e. a + b)
 * A Binary expression is composed of a left side expression, a right side
 * expression, and and operation type {@link BinaryOperatorRules} which can be
 * used to determine operation priority, hashCode and toString for the concrete
 * operation.
 * Since the operator is not specified yet this class is abstract.
 * The subclasses will specify the operation preformed between sides.
 * @param <E> Type of numbers in this expression
 * @implNote Caution: due to the way binary expression will be parsed
 * we need to support left and right null expressions during construction.
 * And be able to set left and right expressions later.
 */
public abstract class BinaryExpression<E extends Number> extends AbstractExpression<E>
{
	/**
	 * Left expression
	 */
	protected Expression<E> left;

	/**
	 * Right expression
	 */
	protected Expression<E> right;

	/**
	 * Binary operator rules applicable to this binary expression
	 * these rules are used to determine the priority of this binary expression
	 * as well as the existence of a unary variant of this operator.
	 * @see BinaryOperatorRules#priority()
	 * @see BinaryOperatorRules#hasUnary()
	 */
	protected final BinaryOperatorRules rules;

	/**
	 * Valued constructor (to be used by subclasses)
	 * @param left left part of this binary expression
	 * @param right right part if this binary expression
	 * @param rules binary expression type
	 */
	protected BinaryExpression(Expression<E> left,
	                           Expression<E> right,
	                           BinaryOperatorRules rules)
	{
		this.left = left;
		if (this.left != null)
		{
			this.left.setParent(this);
		}

		this.right = right;
		if (this.right != null)
		{
			this.right.setParent(this);
		}

		this.rules = rules;
	}

	/**
	 * Constructor with only the operation type
	 * @param rules the rules of operation to perform
	 * @implSpec left and right side expressions shall be set to null
	 */
	protected BinaryExpression(BinaryOperatorRules rules)
	{
		this(null, null, rules);
	}

	/**
	 * Left accessor
	 * @return the left expression
	 */
	public Expression<E> getLeft()
	{
		return left;
	}

	/**
	 * Right accessor
	 * @return the right expression
	 */
	public Expression<E> getRight()
	{
		return right;
	}

	/**
	 * Left side expression setter
	 * @param left the left expression to set
	 * @implSpec if existing left expression is non null it should be properly
	 * detached (removing parent) before being replaced.
	 * @implSpec Once left expression has been set it should have this as its
	 * parent.
	 */
	public void setLeft(Expression<E> left)
	{
		if (this.left != null)
		{
			this.left.setParent(null);
		}

		this.left = left;

		if (this.left != null)
		{
			this.left.setParent(this);
		}
	}

	/**
	 * Right side expression setter
	 * @param right the right expression to set
	 * @implSpec if existing right expression is non null it should be properly
	 * detached (removing parent) before being replaced.
	 * @implSpec Once right expression has been set it should have this as its
	 * parent.
	 */
	public void setRight(Expression<E> right)
	{
		if (this.right != null)
		{
			this.right.setParent(null);
		}

		this.right = right;

		if (this.right != null)
		{
			this.right.setParent(this);
		}
	}

	/**
	 * Accessor to operator rules
	 * @return the binary expression rules
	 */
	public BinaryOperatorRules getRules()
	{
		return rules;
	}

	/**
	 * Numeric value of this expression
	 * @return the numeric value of this expression as computed by
	 * {@link #operate(Number, Number)}
	 * @throws IllegalStateException if a value can't be evaluated right now
	 * @see #operate(Number, Number)
	 */
	@Override
	public E value() throws IllegalStateException
	{
		if (!hasValue())
		{
			throw new IllegalStateException("Both side don't have a value yet");
		}

		return operate(left.value(), right.value());
	}

	/**
	 * Indicate if both {@link #left} and {@link #right} side have values so
	 * that the operation can produce a value
	 * @return true if expression can produce a value
	 * and calling {@link #value()} is legal. False otherwise
	 */
	@Override
	public boolean hasValue()
	{
		if ((left != null) && (right != null))
		{
			return left.hasValue() && right.hasValue();
		}
		return false;
	}

	/**
	 * Operate the concrete operation performed by this expression on operands
	 * @param value1 first operand's value
	 * @param value2 second operand's value
	 * @return the actual value resulting from this binary expression
	 * or throws an exception if the operation can't be performed
	 * @throws UnsupportedOperationException if the type E of the operands
	 * is not one of {@link Integer}, {@link Float} or {@link Double}
	 * @implSpec it is assumed only value1 is tested to check for either
	 * {@link Integer}, {@link Float} or {@link Double}.
	 */
	protected abstract E operate(E value1, E value2)
		throws UnsupportedOperationException;

	/**
	 * Set new parent to expression.
	 * @param parent The parent to set
	 * @throws IllegalArgumentException if the provided parent is a
	 * {@link TerminalExpression} since {@link TerminalExpression} can't have
	 * children.
	 * @throws IllegalArgumentException if the provided new parent is contained
	 * in this expression (which includes the case where the proposed parent is self)
	 */
	@Override
	public void setParent(Expression<E> parent) throws IllegalArgumentException
	{
		if (parent instanceof TerminalExpression<?>)
		{
			throw new IllegalArgumentException("parent can't be terminal expression");
		}
		if (contains(parent))
		{
			throw new IllegalArgumentException("parent can't be contained");
		}
		this.parent = parent;
	}

	/**
	 * Test containment of another expression
	 * @param expr the expression to test
	 * @return true if the provided expression can be found in this expression.
	 * @implSpec for non terminal expressions, containing an expression means the
	 * same thing as for non terminal expressions but also if the provided expression
	 * can be found on the left or right side
	 */
	@Override
	public boolean contains(Expression<E> expr)
	{
		if (super.contains(expr))
		{
			return true;
		}

		boolean leftContained = (left != null ? left.contains(expr) : false);
		boolean rightContained = (right != null ? right.contains(expr) : false);
		return leftContained || rightContained;
	}

	/**
	 * String representation of this binary expression :
	 * left side expressions string + operation string + right side expression
	 * string
	 * @return a string representation of this binary operation
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		if (left != null)
		{
			sb.append(left.toString());
			sb.append(' ');
		}

		sb.append(rules.toString());

		if (right != null)
		{
			sb.append(' ');
			/*
			 * If right side has lower or equal priority then parentheses are
			 * required
			 */
			boolean lowerPriority = hasLowerPriority(right);
			if (lowerPriority)
			{
				sb.append("(");
			}

			sb.append(right.toString());

			if (lowerPriority)
			{
				sb.append(")");
			}
		}

		return sb.toString();
	}

	/**
	 * Method to determine if an operand has operators with lower priority
	 * than the current operator
	 * @param operand the operand to test for lower priority operators
	 * @return true if the operand contains operators with lower priority than
	 * the current operator
	 * @see #toString()
	 */
	private boolean hasLowerPriority(Expression<E> operand)
	{
		if (operand instanceof BinaryExpression<?>)
		{
			BinaryExpression<E> binop  = (BinaryExpression<E>) operand;
			return binop.rules.priority() <= rules.priority();
		}
		return false;
	}
}
