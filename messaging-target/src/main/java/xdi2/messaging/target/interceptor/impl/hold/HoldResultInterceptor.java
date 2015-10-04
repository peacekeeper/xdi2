package xdi2.messaging.target.interceptor.impl.hold;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.Graph;
import xdi2.core.bootstrap.XDIBootstrap;
import xdi2.core.constants.XDILinkContractConstants;
import xdi2.core.features.index.Index;
import xdi2.core.features.linkcontracts.instance.LinkContract;
import xdi2.core.features.linkcontracts.instantiation.LinkContractInstantiation;
import xdi2.core.features.nodetypes.XdiEntityCollection;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.syntax.XDIStatement;
import xdi2.core.util.CopyUtil;
import xdi2.core.util.GraphAware;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.constants.XDIMessagingConstants;
import xdi2.messaging.operations.Operation;
import xdi2.messaging.target.MessagingTarget;
import xdi2.messaging.target.Prototype;
import xdi2.messaging.target.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.execution.ExecutionContext;
import xdi2.messaging.target.execution.ExecutionResult;
import xdi2.messaging.target.interceptor.InterceptorResult;
import xdi2.messaging.target.interceptor.MessageEnvelopeInterceptor;
import xdi2.messaging.target.interceptor.impl.AbstractInterceptor;

/**
 * This interceptor can add hold results to a messaging target and execution result.
 */
public class HoldResultInterceptor extends AbstractInterceptor<MessagingTarget> implements GraphAware, MessageEnvelopeInterceptor, Prototype<HoldResultInterceptor> {

	private static final Logger log = LoggerFactory.getLogger(HoldResultInterceptor.class);

	private Graph targetGraph;

	public HoldResultInterceptor(Graph targetGraph) {

		this.targetGraph = targetGraph;
	}

	public HoldResultInterceptor() {

		this(null);
	}

	/*
	 * Prototype
	 */

	@Override
	public HoldResultInterceptor instanceFor(xdi2.messaging.target.Prototype.PrototypingContext prototypingContext) throws Xdi2MessagingException {

		// create new contributor

		HoldResultInterceptor contributor = new HoldResultInterceptor();

		// set the graph

		contributor.setTargetGraph(this.getTargetGraph());

		// done

		return contributor;
	}

	/*
	 * GraphAware
	 */

	@Override
	public void setGraph(Graph graph) {

		if (this.getTargetGraph() == null) this.setTargetGraph(graph);
	}

	/*
	 * MessageEnvelopeInterceptor
	 */


	@Override
	public InterceptorResult before(MessageEnvelope messageEnvelope, ExecutionContext executionContext, ExecutionResult executionResult) throws Xdi2MessagingException {

		return InterceptorResult.DEFAULT;
	}

	@Override
	public InterceptorResult after(MessageEnvelope messageEnvelope, ExecutionContext executionContext, ExecutionResult executionResult) throws Xdi2MessagingException {

		// look for hold results

		MessagingTarget messagingTarget = executionContext.getCurrentMessagingTarget();

		Map<Operation, List<HoldResult>> operationHoldResultsMap = getOperationHoldResults(executionContext);
		if (operationHoldResultsMap == null) return InterceptorResult.DEFAULT;

		for (Map.Entry<Operation, List<HoldResult>> operationHoldResults : operationHoldResultsMap.entrySet()) {

			Operation operation = operationHoldResults.getKey();
			List<HoldResult> holdResults = operationHoldResults.getValue();

			if (holdResults.isEmpty()) continue;

			Graph operationPushResultGraph = executionResult.createOperationPushResultGraph(operation);

			Message message = operation.getMessage();

			for (HoldResult holdResult : holdResults) {

				// write message and index into target graph

				if (this.getTargetGraph() != null) {

					CopyUtil.copyContextNode(message.getContextNode(), this.getTargetGraph(), null);
					XdiEntityCollection xdiMessageIndex = Index.getEntityIndex(this.getTargetGraph(), XDIMessagingConstants.XDI_ARC_MSG, true);
					Index.setEntityIndexAggregation(xdiMessageIndex, message.getXdiEntity());
				}

				// hold push result?

				if (holdResult.getPush()) {

					// determine requesting and authorizing authorities

					XDIAddress authorizingAuthority = messagingTarget.getOwnerXDIAddress();
					XDIAddress requestingAuthority = message.getFromXDIAddress();

					// determine variable values

					XDIAddress pushVariableValue = null;
					if (pushVariableValue == null && holdResult.getXDIAddress() != null) pushVariableValue = holdResult.getXDIAddress();
					if (pushVariableValue == null && holdResult.getXDIStatement() != null) pushVariableValue = targetXDIAddressForTargetXDIStatement(holdResult.getXDIStatement());
					if (pushVariableValue == null) throw new NullPointerException();

					XDIAddress msgVariableValue = message.getContextNode().getXDIAddress();

					Map<XDIArc, Object> variableValues = new HashMap<XDIArc, Object> ();
					variableValues.put(XDIArc.create("{$push}"), pushVariableValue);
					variableValues.put(XDIArc.create("{$msg}"), msgVariableValue);

					// instantiate push link contract

					LinkContractInstantiation linkContractInstantiation = new LinkContractInstantiation(XDIBootstrap.HOLD_PUSH_LINK_CONTRACT_TEMPLATE);
					linkContractInstantiation.setAuthorizingAuthority(authorizingAuthority);
					linkContractInstantiation.setRequestingAuthority(requestingAuthority);
					linkContractInstantiation.setVariableValues(variableValues);

					LinkContract pushLinkContract;

					try {

						pushLinkContract = linkContractInstantiation.execute(true);
					} catch (Exception ex) {

						throw new Xdi2MessagingException("Cannot instantiate $push link contract: " + ex.getMessage(), ex, executionContext);
					}

					// write push link contract into operation push result graph

					CopyUtil.copyGraph(pushLinkContract.getContextNode().getGraph(), operationPushResultGraph, null);

					// write push link contract and index into target graph

					if (this.getTargetGraph() != null) {

						CopyUtil.copyGraph(pushLinkContract.getContextNode().getGraph(), this.getTargetGraph(), null);
						XdiEntityCollection xdiLinkContractIndex = Index.getEntityIndex(this.getTargetGraph(), XDILinkContractConstants.XDI_ARC_DO, true);
						Index.setEntityIndexAggregation(xdiLinkContractIndex, pushLinkContract.getXdiEntity());
					}
				}
			}

			if (log.isDebugEnabled()) log.debug("For operation " + operation + " we have operation push result graph " + operationPushResultGraph);
		}

		// done

		return InterceptorResult.DEFAULT;
	}

	@Override
	public void exception(MessageEnvelope messageEnvelope, ExecutionContext executionContext, ExecutionResult executionResult, Exception ex) {

	}

	/*
	 * Getters and setters
	 */

	public Graph getTargetGraph() {

		return this.targetGraph;
	}

	public void setTargetGraph(Graph targetGraph) {

		this.targetGraph = targetGraph;
	}

	/*
	 * Helper methods
	 */

	private static XDIAddress targetXDIAddressForTargetXDIStatement(XDIStatement targetXDIStatement) {

		if (targetXDIStatement.isContextNodeStatement()) {

			return targetXDIStatement.getTargetXDIAddress();
		} else {

			return targetXDIStatement.getContextNodeXDIAddress();
		}
	}

	/*
	 * ExecutionContext helper methods
	 */

	private static final String EXECUTIONCONTEXT_KEY_OPERATIONHOLDRESULTS_PER_MESSAGEENVELOPE = HoldResultInterceptor.class.getCanonicalName() + "#operationholdresultspermessageenvelope";

	@SuppressWarnings("unchecked")
	public static Map<Operation, List<HoldResult>> getOperationHoldResults(ExecutionContext executionContext) {

		return (Map<Operation, List<HoldResult>>) executionContext.getMessageEnvelopeAttribute(EXECUTIONCONTEXT_KEY_OPERATIONHOLDRESULTS_PER_MESSAGEENVELOPE);
	}

	@SuppressWarnings("unchecked")
	public static void addOperationHoldResult(ExecutionContext executionContext, Operation operation, HoldResult holdResult) {

		Map<Operation, List<HoldResult>> holdResultsMap = (Map<Operation, List<HoldResult>>) executionContext.getMessageEnvelopeAttribute(EXECUTIONCONTEXT_KEY_OPERATIONHOLDRESULTS_PER_MESSAGEENVELOPE);
		if (holdResultsMap == null) { holdResultsMap = new HashMap<Operation, List<HoldResult>> (); executionContext.putMessageEnvelopeAttribute(EXECUTIONCONTEXT_KEY_OPERATIONHOLDRESULTS_PER_MESSAGEENVELOPE, holdResultsMap); }

		List<HoldResult> holdResults = holdResultsMap.get(operation);
		if (holdResults == null) { holdResults = new ArrayList<HoldResult> (); holdResultsMap.put(operation, holdResults); }

		holdResults.add(holdResult);
	}

	/*
	 * Helper class
	 */

	public static class HoldResult implements Serializable {

		private static final long serialVersionUID = 904748436911142763L;

		private XDIAddress XDIaddress;
		private XDIStatement XDIstatement;
		private boolean push;

		public HoldResult(XDIAddress XDIaddress, boolean push) {

			this.XDIaddress = XDIaddress;
			this.XDIstatement = null;
			this.push = push;
		}

		public HoldResult(XDIStatement XDIstatement, boolean push) {

			this.XDIaddress = null;
			this.XDIstatement = XDIstatement;
			this.push = push;
		}

		public XDIAddress getXDIAddress() {

			return this.XDIaddress;
		}

		public XDIStatement getXDIStatement() {

			return this.XDIstatement;
		}

		public boolean getPush() {

			return this.push;
		}
	}
}
