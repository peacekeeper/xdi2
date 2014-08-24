package xdi2.core.features.nodetypes;

import java.util.Iterator;

import xdi2.core.ContextNode;
import xdi2.core.constants.XDIConstants;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.iterators.MappingIterator;
import xdi2.core.util.iterators.NotNullIterator;

/**
 * An XDI entity singleton, represented as a context node.
 * 
 * @author markus
 */
public final class XdiEntitySingleton extends XdiAbstractSingleton<XdiEntity> implements XdiEntity {

	private static final long serialVersionUID = 7600443284706530972L;

	protected XdiEntitySingleton(ContextNode contextNode) {

		super(contextNode);
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if a context node is a valid XDI entity singleton.
	 * @param contextNode The context node to check.
	 * @return True if the context node is a valid XDI entity singleton.
	 */
	public static boolean isValid(ContextNode contextNode) {

		if (contextNode == null) return false;

		return 
				isValidXDIArc(contextNode.getXDIArc()) &&
				( ! XdiAttributeCollection.isValid(contextNode.getContextNode()) && ! XdiAbstractAttribute.isValid(contextNode.getContextNode()) );
	}

	/**
	 * Factory method that creates an XDI entity singleton bound to a given context node.
	 * @param contextNode The context node that is an XDI entity singleton.
	 * @return The XDI entity singleton.
	 */
	public static XdiEntitySingleton fromContextNode(ContextNode contextNode) {

		if (! isValid(contextNode)) return null;

		return new XdiEntitySingleton(contextNode);
	}

	/*
	 * Methods for XRIs
	 */

	public static XDIArc createXDIArc(XDIArc arc) {

		return arc;
	}

	public static boolean isValidXDIArc(XDIArc arc) {

		if (arc == null) return false;

		if (arc.isAttributeXs()) return false;
		if (arc.isClassXs()) return false;

		if (! arc.hasLiteral() && ! arc.hasXRef()) return false;

		if (XDIConstants.CS_CLASS_UNRESERVED.equals(arc.getCs()) || XDIConstants.CS_CLASS_RESERVED.equals(arc.getCs())) {

		} else if (XDIConstants.CS_AUTHORITY_PERSONAL.equals(arc.getCs()) || XDIConstants.CS_AUTHORITY_LEGAL.equals(arc.getCs()) || XDIConstants.CS_AUTHORITY_GENERAL.equals(arc.getCs())) {

		} else {

			return false;
		}

		return true;
	}

	/*
	 * Helper classes
	 */

	public static class MappingContextNodeXdiEntitySingletonIterator extends NotNullIterator<XdiEntitySingleton> {

		public MappingContextNodeXdiEntitySingletonIterator(Iterator<ContextNode> contextNodes) {

			super(new MappingIterator<ContextNode, XdiEntitySingleton> (contextNodes) {

				@Override
				public XdiEntitySingleton map(ContextNode contextNode) {

					return XdiEntitySingleton.fromContextNode(contextNode);
				}
			});
		}
	}
}
