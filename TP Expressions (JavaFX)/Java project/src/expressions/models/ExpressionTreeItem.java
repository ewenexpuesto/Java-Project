/**
 *
 */
package expressions.models;

import java.util.Objects;

import expressions.Expression;
import expressions.binary.AdditionExpression;
import expressions.binary.AssignmentExpression;
import expressions.binary.BinaryExpression;
import expressions.binary.DivisionExpression;
import expressions.binary.MultiplicationExpression;
import expressions.binary.PowerExpression;
import expressions.binary.SubtractionExpression;
import expressions.special.GroupExpression;
import expressions.terminal.ConstantExpression;
import expressions.terminal.TerminalExpression;
import expressions.terminal.VariableExpression;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import utils.IconFactory;

/**
 * A {@link TreeItem} for supporting {@link Expression}s in a {@link TreeView}
 * @param <E> The type of numbers in {@link Expression}s
 *
 */
public class ExpressionTreeItem<E extends Number> extends TreeItem<Expression<E>>
{
	/**
	 * Cached leaf status (computed only once)
	 */
	private boolean isLeaf;

	/**
	 * Flag indicating the {@link #isLeaf} has not been computed yet.
	 * Becomes false once leaf status is established
	 */
	private boolean isFirstTimeLeaf = true;

	/**
	 * Flag indicating the chlidren obtained with {@link #getChildren()} hasn't
	 * been computed yet.
	 * Becomes false once children have been computed
	 */
	private boolean isFirstTimeChildren = true;

	/**
	 * Constructor from expression.
	 * The internal value of this {@link TreeItem} will be the provided
	 * expression that can be obtained with {@link #getValue()}
	 * @param expression the expression to show
	 */
	public ExpressionTreeItem(Expression<E> expression)
	{
		super(expression, getIcon(expression));
		Objects.requireNonNull(expression);
	}

    /**
     * A TreeItem is a leaf if it has no children. The isLeaf method may of
     * course be overridden by subclasses to support alternate means of defining
     * how a TreeItem may be a leaf, but the general premise is the same: a
     * leaf can not be expanded by the user, and as such will not show a
     * disclosure node or respond to expansion requests.
     * @return true if this TreeItem has no children
     */
	@Override
	public boolean isLeaf()
	{
		if (isFirstTimeLeaf)
		{
			isFirstTimeLeaf = false;
			isLeaf = getValue() instanceof TerminalExpression<?>;
		}
		return isLeaf;
	}

    /**
     * The children of this TreeItem. This method is called frequently, and
     * it is therefore recommended that the returned list be cached by
     * any TreeItem implementations.
     * @return a list that contains the child TreeItems belonging to the TreeItem.
     */
	@Override
	public ObservableList<TreeItem<Expression<E>>> getChildren()
	{
		if (isFirstTimeChildren)
		{
			isFirstTimeChildren = false;
			super.getChildren().setAll(buildChildren());
		}
		return super.getChildren();
	}

	/**
	 * Build children list for this item
	 * @return a new observable list of children (possibly empty if this item
	 * is represents a {@link TerminalExpression})
	 */
	private ObservableList<ExpressionTreeItem<E>> buildChildren()
	{
		Expression<E> currentExpression = getValue();
		if (currentExpression instanceof TerminalExpression<?>)
		{
			return FXCollections.emptyObservableList();
		}

		ObservableList<ExpressionTreeItem<E>> children = FXCollections.observableArrayList();
		if (currentExpression instanceof BinaryExpression<?>)
		{
			BinaryExpression<E> operator = (BinaryExpression<E>) currentExpression;
			children.add(new ExpressionTreeItem<E>(operator.getLeft()));
			children.add(new ExpressionTreeItem<E>(operator.getRight()));
		}
		if (currentExpression instanceof GroupExpression<?>)
		{
			GroupExpression<E> group = (GroupExpression<E>) currentExpression;
			for (Expression<E> expression : group)
			{
				children.add(new ExpressionTreeItem<E>(expression));
			}
		}
		return children;
	}

	/**
	 * Icon factory method to provide a customized graphic node for this
	 * expression. Depending on the type of expression.
	 * @param expression the expression to illustrate with an image
	 * @return an ImageView containing an image illustrating this expression
	 */
	private static Node getIcon(Expression<? extends Number> expression)
	{
		Image icon = null;
		if (expression != null)
		{
			if (expression instanceof TerminalExpression<?>)
			{
				if (expression instanceof ConstantExpression<?>)
				{
					icon = IconFactory.getSmallIcon("constant");
				}
				if (expression instanceof VariableExpression<?>)
				{
					icon = IconFactory.getSmallIcon("variable");
				}
			}
			if (expression instanceof BinaryExpression<?>)
			{
				if (expression instanceof AssignmentExpression<?>)
				{
					icon = IconFactory.getSmallIcon("equals");
				}
				if (expression instanceof AdditionExpression<?>)
				{
					icon = IconFactory.getSmallIcon("plus");
				}
				if (expression instanceof SubtractionExpression<?>)
				{
					icon = IconFactory.getSmallIcon("minus");
				}
				if (expression instanceof MultiplicationExpression<?>)
				{
					icon = IconFactory.getSmallIcon("multiply");
				}
				if (expression instanceof DivisionExpression<?>)
				{
					icon = IconFactory.getSmallIcon("divide");
				}
				if (expression instanceof PowerExpression<?>)
				{
					icon = IconFactory.getSmallIcon("power");
				}
			}
		}

		if (icon == null)
		{
			icon = IconFactory.getSmallIcon("unknown");
		}

		return new ImageView(icon);
	}

//	/**
//	 * String representation of this item.
//	 * Various expression items shall be represented as follows
//	 * <ul>
//	 * 	<li>{@link ConstantExpression} are represented by their value</li>
//	 * 	<li>{@link VariableExpression} are represented by their name</li>
//	 * 	<li>{@link BinaryExpression} are represented by their operator symbol</li>
//	 * 	<li>{@link GroupExpression} are represented by an empty String</li>
//	 * </ul>
//	 */
//	@Override
//	public String toString()
//	{
//		Expression<E> expression = getValue();
//		if (expression instanceof BinaryExpression<?>)
//		{
//			return ((BinaryExpression<E>)expression).getRules().toString();
//		}
//		if (expression instanceof GroupExpression<?>)
//		{
//			return "";
//		}
//		return expression.toString();
//	}

	/**
	 * Clears {@link #isFirstTimeLeaf} and {@link #isFirstTimeChildren} status
	 * to force re-evaluation of leaf status and children.
	 */
	public void reset()
	{
		isFirstTimeLeaf = true;
		isFirstTimeChildren = true;
	}
}
