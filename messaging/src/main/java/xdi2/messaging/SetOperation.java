package xdi2.messaging;

import xdi2.core.Relation;
import xdi2.core.features.nodetypes.XdiEntitySingleton;
import xdi2.core.util.XDIAddressUtil;
import xdi2.messaging.constants.XDIMessagingConstants;

/**
 * A $set XDI operation, represented as a relation.
 * 
 * @author markus
 */
public class SetOperation extends Operation {

	private static final long serialVersionUID = -9053418535565359957L;

	protected SetOperation(Message message, Relation relation) {

		super(message, relation);
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if an relation is a valid XDI $set operation.
	 * @param relation The relation to check.
	 * @return True if the relation is a valid XDI $set operation.
	 */
	public static boolean isValid(Relation relation) {

		if (XDIAddressUtil.startsWithXDIAddress(relation.getXDIAddress(), XDIMessagingConstants.XDI_ADD_SET) == null) return false;
		if (! XdiEntitySingleton.createXDIArc(XDIMessagingConstants.XDI_ARC_DO).equals(relation.getContextNode().getXDIArc())) return false;

		return true;
	}

	/**
	 * Factory method that creates an XDI $set operation bound to a given relation.
	 * @param relation The relation that is an XDI $set operation.
	 * @return The XDI $set operation.
	 */
	public static SetOperation fromMessageAndRelation(Message message, Relation relation) {

		if (! isValid(relation)) return null;

		return new SetOperation(message, relation);
	}
}
