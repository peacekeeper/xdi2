package xdi2.core.features.nodetypes;

import java.util.Iterator;

import xdi2.core.ContextNode;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.GraphUtil;
import xdi2.core.util.iterators.MappingIterator;
import xdi2.core.util.iterators.NotNullIterator;

/**
 * An XDI ordered attribute member, represented as a context node.
 * 
 * @author markus
 */
public class XdiAttributeMemberOrdered extends XdiAbstractMemberOrdered<XdiAttributeCollection, XdiAttribute, XdiAttributeCollection, XdiAttributeMemberUnordered, XdiAttributeMemberOrdered, XdiAttributeMember> implements XdiAttributeMember {

	private static final long serialVersionUID = 3562576098019686485L;

	protected XdiAttributeMemberOrdered(ContextNode contextNode) {

		super(contextNode);
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if a context node is a valid XDI ordered attribute member.
	 * @param contextNode The context node to check.
	 * @return True if the context node is a valid XDI ordered attribute member.
	 */
	public static boolean isValid(ContextNode contextNode) {

		if (contextNode == null) throw new NullPointerException();

		if (contextNode.getXDIArc() == null || ! isValidXDIArc(contextNode.getXDIArc())) return false;
		if (contextNode.getContextNode() == null || ! XdiAttributeCollection.isValid(contextNode.getContextNode())) return false;

		return true;
	}

	/**
	 * Factory method that creates an XDI ordered attribute member bound to a given context node.
	 * @param contextNode The context node that is an XDI ordered attribute member.
	 * @return The XDI ordered attribute member.
	 */
	public static XdiAttributeMemberOrdered fromContextNode(ContextNode contextNode) {

		if (contextNode == null) throw new NullPointerException();

		if (! isValid(contextNode)) return null;

		if (contextNode.getXDIArc().isDefinition() && contextNode.getXDIArc().isVariable()) return new Definition.Variable(contextNode);
		if (contextNode.getXDIArc().isDefinition() && ! contextNode.getXDIArc().isVariable()) return new Definition(contextNode);
		if (! contextNode.getXDIArc().isDefinition() && contextNode.getXDIArc().isVariable()) return new Variable(contextNode);
		return new XdiAttributeMemberOrdered(contextNode);
	}

	public static XdiAttributeMemberOrdered fromXDIAddress(XDIAddress XDIaddress) {

		return fromContextNode(GraphUtil.contextNodeFromComponents(XDIaddress));
	}

	/*
	 * Methods for arcs
	 */

	public static boolean isValidXDIArc(XDIArc XDIarc) {

		if (XDIarc == null) throw new NullPointerException();

		if (! XdiAbstractMemberOrdered.isValidXDIArc(XDIarc, XdiAttributeCollection.class)) return false;

		return true;
	}

	/*
	 * Instance methods
	 */

	/**
	 * Returns the parent XDI collection of this XDI ordered attribute member.
	 * @return The parent XDI collection.
	 */
	@Override
	public XdiAttributeCollection getXdiCollection() {

		return new XdiAttributeCollection(this.getContextNode().getContextNode());
	}

	/**
	 * Creates or returns an XDI value under this XDI ordered attribute member.
	 * @param create Whether or not to create the context node if it doesn't exist.
	 * @return The XDI value.
	 */
	@Override
	public XdiValue getXdiValue(boolean create) {

		XDIArc valuearc = XdiValue.createXDIArc();

		ContextNode valueContextNode = create ? this.getContextNode().setContextNode(valuearc) : this.getContextNode().getContextNode(valuearc, false);
		if (valueContextNode == null) return null;

		return new XdiValue(valueContextNode);
	}

	/*
	 * Definition and Variable classes
	 */

	public static class Definition extends XdiAttributeMemberOrdered implements XdiDefinition<XdiAttribute> {

		private static final long serialVersionUID = 9076766650130455708L;

		private Definition(ContextNode contextNode) {

			super(contextNode);
		}

		public static boolean isValid(ContextNode contextNode) {

			return XdiAttributeMemberOrdered.isValid(contextNode) &&
					contextNode.getXDIArc().isDefinition() &&
					! contextNode.getXDIArc().isVariable();
		}

		public static Definition fromContextNode(ContextNode contextNode) {

			if (contextNode == null) throw new NullPointerException();

			if (! isValid(contextNode)) return null;

			return new Definition(contextNode);
		}

		public static class Variable extends Definition implements XdiVariable<XdiAttribute> {

			private static final long serialVersionUID = 8232065600537713537L;

			private Variable(ContextNode contextNode) {

				super(contextNode);
			}

			public static boolean isValid(ContextNode contextNode) {

				return XdiAttributeMemberOrdered.isValid(contextNode) &&
						contextNode.getXDIArc().isDefinition() &&
						contextNode.getXDIArc().isVariable();
			}

			public static Variable fromContextNode(ContextNode contextNode) {

				if (contextNode == null) throw new NullPointerException();

				if (! isValid(contextNode)) return null;

				return new Variable(contextNode);
			}
		}
	}

	public static class Variable extends XdiAttributeMemberOrdered implements XdiVariable<XdiAttribute> {

		private static final long serialVersionUID = -3150796131750366380L;

		private Variable(ContextNode contextNode) {

			super(contextNode);
		}

		public static boolean isValid(ContextNode contextNode) {

			return XdiAttributeMemberOrdered.isValid(contextNode) &&
					! contextNode.getXDIArc().isDefinition() &&
					contextNode.getXDIArc().isVariable();
		}

		public static Variable fromContextNode(ContextNode contextNode) {

			if (contextNode == null) throw new NullPointerException();

			if (! isValid(contextNode)) return null;

			return new Variable(contextNode);
		}
	}

	/*
	 * Helper classes
	 */

	public static class MappingContextNodeXdiAttributeMemberOrderedIterator extends NotNullIterator<XdiAttributeMemberOrdered> {

		public MappingContextNodeXdiAttributeMemberOrderedIterator(Iterator<ContextNode> contextNodes) {

			super(new MappingIterator<ContextNode, XdiAttributeMemberOrdered> (contextNodes) {

				@Override
				public XdiAttributeMemberOrdered map(ContextNode contextNode) {

					return XdiAttributeMemberOrdered.fromContextNode(contextNode);
				}
			});
		}
	}
}
