package xdi2.core.features.policy.condition;

import xdi2.core.ContextNode;
import xdi2.core.constants.XDIPolicyConstants;
import xdi2.core.features.policy.evaluation.PolicyEvaluationContext;
import xdi2.core.impl.AbstractLiteralNode;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIStatement;

/**
 * An XDI $lesser condition, represented as a statement.
 * 
 * @author markus
 */
public class LesserCondition extends Condition {

	private static final long serialVersionUID = -8479583948559334331L;

	protected LesserCondition(XDIStatement XDIstatement) {

		super(XDIstatement);
	}

	/*
	 * Static methods
	 */

	/**
	 * Checks if a statement is a valid XDI $lesser condition.
	 * @param XDIstatement The statement to check.
	 * @return True if the statement is a valid XDI $lesser condition.
	 */
	public static boolean isValid(XDIStatement XDIstatement) {

		if (! XDIstatement.isRelationStatement()) return false;

		if (! XDIPolicyConstants.XDI_ADD_LESSER.equals(XDIstatement.getRelationXDIAddress())) return false;

		return true;
	}

	/**
	 * Factory method that creates an XDI $lesser condition bound to a given statement.
	 * @param XDIstatement The statement that is an XDI $lesser condition.
	 * @return The XDI $lesser condition.
	 */
	public static LesserCondition fromStatement(XDIStatement XDIstatement) {

		if (! isValid(XDIstatement)) return null;

		return new LesserCondition(XDIstatement);
	}

	public static LesserCondition fromSubjectAndObject(XDIAddress subject, XDIAddress object) {

		return fromStatement(XDIStatement.fromRelationComponents(subject, XDIPolicyConstants.XDI_ADD_LESSER, object));
	}

	/*
	 * Instance methods
	 */

	@Override
	public Boolean evaluateInternal(PolicyEvaluationContext policyEvaluationContext) {

		ContextNode subject = policyEvaluationContext.getContextNode(this.getXDIStatement().getSubject());
		ContextNode object = policyEvaluationContext.getContextNode((XDIAddress) this.getXDIStatement().getObject());

		if (subject == null || object == null) return Boolean.FALSE;

		if (subject.containsLiteralNode()) {

			if (! (object.containsLiteralNode())) return Boolean.FALSE;

			Object subjectLiteralData = subject.getLiteralNode().getLiteralData();
			Object objectLiteralData = object.getLiteralNode().getLiteralData();

			if (subjectLiteralData == null || objectLiteralData == null) return Boolean.FALSE;

			return Boolean.valueOf(AbstractLiteralNode.LITERALDATACOMPARATOR.compare(subjectLiteralData, objectLiteralData) < 0);
		}

		return Boolean.FALSE;
	}
}