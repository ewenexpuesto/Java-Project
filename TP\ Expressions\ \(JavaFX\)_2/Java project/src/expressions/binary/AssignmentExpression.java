package expressions.binary;

import expressions.Expression;
import expressions.terminal.TerminalExpression;
import expressions.terminal.VariableExpression;

/**
 * Assignment expression.
 * Special case of binary expression used to assign an {@link Expression}
 * to a {@link VariableExpression}
 * @param <E> The type of numbers in expressions
 */
public class AssignmentExpression<E extends Number> extends BinaryExpression<E>
{

	/**
	 * Valued constructor
	 * @param left the {@link VariableExpression} to assign with this operator
	 * @param right an {@link Expression} to be assigned
	 * @implSpec if right side expression can be evaluated its result shall be
	 * assigned to the left variable expression
	 */
	public AssignmentExpression(VariableExpression<E> left, Expression<E> right)
	{
		super(left, right, BinaryOperatorRules.ASSIGNMENT);
		// DONE Complete ...
		setLeft(left);
        setRight(right);
	}

	/**
	 * Default constructor.
	 * Builds an empty non evaluable assigment expression
	 */
	public AssignmentExpression()
	{
		this(null, null);
	}

	/**
	 * Numeric value of this expression.
	 * This implementation uses the {@link BinaryExpression#value()} but also
	 * ensures that if right side has a value it is copied to the left side.
	 * @return the numeric value of this expression as computed by
	 * {@link #operate(Number, Number)}
	 * @throws IllegalStateException if a value can't be evaluated right now
	 */
	@Override
	public E value() throws IllegalStateException
	{
		if (!hasValue())
		{
			throw new IllegalStateException("Assignment don't have a value yet");
		}
		// DONE Replace with correct implementation
		E value = right.value();
		
		// Assign value to left side variable
		if (left instanceof VariableExpression<E> variable) {
			variable.setValue(value);
		}
		
		// Return the assigned value
		return value;
	}

	/**
	 * Indicate if this assignment has a value.
	 * An assignement has a value when both sides are non null and the right
	 * side can produce a value (which shall be copied to the left side during
	 * evaluation).
	 * @return true if expression can produce a value
	 * and calling {@link #value()} is legal. False otherwise
	 */
	@Override
	public boolean hasValue()
	{
		// DONE Replace with correct implementation
		return getLeft() != null && getRight() != null && getRight().hasValue();
	}

	/**
	 * Operate the concrete operation performed by this expression on operands,
	 * assignment operation always evaluate to the assigned variable value (value1)
	 * @param value1 first operand
	 * @param value2 second operand
	 * @return the value of the first operand
	 */
	@Override
	protected E operate(E value1, E value2)
	{
		// DONE Replace with correct implementation
		return value1;
	}

	/**
	 * Set new parent to expression.
	 * @param parent The parent to set
	 * @throws IllegalArgumentException if provided parent is a
	 * {@link BinaryExpression} since {@link AssignmentExpression} can't have a
	 * parent arithmetic expression
	 * @throws IllegalArgumentException if provided parent is a
	 * {@link TerminalExpression} since {@link AssignmentExpression} can't have
	 * a parent arithmetic expression
	 * @throws IllegalArgumentException if provided parent is a
	 * {@link BinaryExpression} since {@link AssignmentExpression} can't have
	 * a parent arithmetic expression
	 */
	@Override
	public void setParent(Expression<E> parent) throws IllegalArgumentException
	{
		// DONE Replace with correct implementation
		if (parent instanceof BinaryExpression || parent instanceof TerminalExpression) {
			throw new IllegalArgumentException(
				"AssignmentExpression can't have an arithmetic expression (Binary or Terminal) as parent"
			);
		}
		super.setParent(parent);
	}

	/**
	 * Left side expression setter.
	 * Special case for {@link AssignmentExpression} since the left side must be
	 * a {@link VariableExpression}.
	 * @param left the left expression to set
	 * @throws IllegalArgumentException if the provided left is not a
	 * {@link VariableExpression}
	 * @implSpec if provided left is non null and is a {@link VariableExpression}
	 * then replace this left side with provided left
	 * @implSpec if left side can be replaced by provided left and right side
	 * has a value, ensures right side value is copied to new left side.
	 */
	@Override
	public void setLeft(Expression<E> left) throws IllegalArgumentException
	{
		// DONE replace with correct implementation
		if (!(left instanceof VariableExpression)) {
			throw new IllegalArgumentException("Left side of AssignmentExpression must be a VariableExpression.");
		}
	
		super.setLeft(left);
	
		// Si possible, propager la valeur de droite vers la nouvelle gauche
		if (left != null && right != null && right.hasValue()) {
			((VariableExpression<E>) left).setValue(right.value());
		}
	}

	/**
	 * Right side expression setter.
	 * Special case for {@link AssignmentExpression}
	 * @param right the right expression to set
	 * @implSpec if provided right is non null and have a value ensures its
	 * value is copied to the left side
	 * @implSpec if provided right side is null or doesn't have a value ensures
	 * left side reflects that.
	 */
	@Override
	public void setRight(Expression<E> right)
	{
		// DONE replace with correct implementation
		super.setRight(right);

		// Si possible, propager la valeur de droite vers la gauche
		if (left instanceof VariableExpression<E> variable) {
			if (right != null && right.hasValue()) {
				variable.setValue(right.value());
			} else {
				variable.clearValue(); // Supposition raisonnable : refléter l’absence de valeur
			}
		}
	}
}
