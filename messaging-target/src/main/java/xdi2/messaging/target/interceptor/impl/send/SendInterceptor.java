package xdi2.messaging.target.interceptor.impl.send;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xdi2.agent.XDIAgent;
import xdi2.agent.impl.XDIBasicAgent;
import xdi2.client.XDIClient;
import xdi2.client.XDIClientRoute;
import xdi2.client.exceptions.Xdi2AgentException;
import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.client.manipulator.impl.SetLinkContractMessageManipulator;
import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.features.linkcontracts.instance.PublicLinkContract;
import xdi2.core.features.nodetypes.XdiAbstractEntity;
import xdi2.core.features.nodetypes.XdiEntity;
import xdi2.core.features.nodetypes.XdiInnerRoot;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.CopyUtil;
import xdi2.core.util.iterators.IteratorListMaker;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.operations.Operation;
import xdi2.messaging.operations.SendOperation;
import xdi2.messaging.response.MessagingResponse;
import xdi2.messaging.target.MessagingTarget;
import xdi2.messaging.target.Prototype;
import xdi2.messaging.target.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.execution.ExecutionContext;
import xdi2.messaging.target.impl.AbstractMessagingTarget;
import xdi2.messaging.target.interceptor.InterceptorResult;
import xdi2.messaging.target.interceptor.OperationInterceptor;
import xdi2.messaging.target.interceptor.impl.AbstractInterceptor;
import xdi2.messaging.target.interceptor.impl.linkcontract.LinkContractInterceptor;
import xdi2.messaging.util.MessagingCloneUtil;

/**
 * This interceptor can process $send operations.
 */
public class SendInterceptor extends AbstractInterceptor<MessagingTarget> implements OperationInterceptor, Prototype<SendInterceptor> {

	private XDIAgent xdiAgent;

	public SendInterceptor(XDIAgent xdiAgent) {

		this.xdiAgent = xdiAgent;
	}

	public SendInterceptor() {

		this(new XDIBasicAgent());
	}

	/*
	 * Prototype
	 */

	@Override
	public SendInterceptor instanceFor(xdi2.messaging.target.Prototype.PrototypingContext prototypingContext) throws Xdi2MessagingException {

		// create new contributor

		SendInterceptor contributor = new SendInterceptor();

		// set the agent

		contributor.setXdiAgent(this.getXdiAgent());

		// done

		return contributor;
	}

	/*
	 * OperationInterceptor
	 */

	@Override
	public InterceptorResult before(Operation operation, Graph operationResultGraph, ExecutionContext executionContext) throws Xdi2MessagingException {

		// check operation

		if (! (operation instanceof SendOperation)) return InterceptorResult.DEFAULT;

		// get forwarding message(s)

		List<Message> forwardingMessages = this.getForwardingMessages(operation, executionContext);

		// send

		for (Message forwardingMessage : forwardingMessages) {

			this.send(forwardingMessage, operation, operationResultGraph, executionContext);
		}

		// done

		return InterceptorResult.SKIP_MESSAGING_TARGET;
	}

	@Override
	public InterceptorResult after(Operation operation, Graph operationResultGraph, ExecutionContext executionContext) throws Xdi2MessagingException {

		// done

		return InterceptorResult.DEFAULT;
	}

	/*
	 * Helper methods
	 */

	public List<Message> getForwardingMessages(Operation operation, ExecutionContext executionContext) throws Xdi2MessagingException {

		List<Message> forwardingMessages = SendInterceptor.getForwardingMessages(executionContext);
		if (forwardingMessages != null) return forwardingMessages;

		if (forwardingMessages == null && operation.getTargetXDIAddress() != null) forwardingMessages = this.forwardingMessageFromTargetXDIAddress(operation.getTargetXDIAddress(), executionContext);
		if (forwardingMessages == null && operation.getTargetXdiInnerRoot() != null) forwardingMessages = this.forwardingMessagesFromTargetXdiInnerRoot(operation.getTargetXdiInnerRoot(), executionContext);
		if (forwardingMessages == null) throw new Xdi2MessagingException("No forwarding messages(s) in operation " + operation, null, executionContext);

		SendInterceptor.putForwardingMessages(executionContext, forwardingMessages);

		return forwardingMessages;
	}

	private List<Message> forwardingMessageFromTargetXDIAddress(XDIAddress targetXDIAddress, ExecutionContext executionContext) throws Xdi2MessagingException {

		// use agent to obtain forwarding message

		XDIAddress forwardingMessageXDIaddress = targetXDIAddress;

		ContextNode forwardingMessageContextNode;

		try {

			forwardingMessageContextNode = this.getXdiAgent().get(
					forwardingMessageXDIaddress,
					new SetLinkContractMessageManipulator(PublicLinkContract.class));
		} catch (Exception ex) {

			throw new Xdi2MessagingException("Unable to obtain forwarding message at address " + targetXDIAddress + ": " + ex.getMessage(), ex, executionContext);
		}

		if (forwardingMessageContextNode == null) throw new Xdi2MessagingException("Cannot find forwarding message at address " + targetXDIAddress, null, executionContext);

		XdiEntity forwardingMessageXdiEntity = XdiAbstractEntity.fromContextNode(forwardingMessageContextNode);
		if (forwardingMessageXdiEntity == null) throw new Xdi2MessagingException("Invalid forwarding message context node at address " + targetXDIAddress, null, executionContext);

		Message forwardingMessage = Message.fromXdiEntity(forwardingMessageXdiEntity);
		if (forwardingMessage == null) throw new Xdi2MessagingException("Invalid forwarding message at address " + targetXDIAddress, null, executionContext);

		// clone forwarding message with new ID

		forwardingMessage = MessagingCloneUtil.cloneMessage(forwardingMessage, true);

		// done

		return Collections.singletonList(forwardingMessage);
	}

	private List<Message> forwardingMessagesFromTargetXdiInnerRoot(XdiInnerRoot targetXdiInnerRoot, ExecutionContext executionContext) throws Xdi2MessagingException {

		// get the inner graph

		Graph innerGraph = targetXdiInnerRoot.getInnerGraph();

		// clone forwarding messages without new ID

		List<Message> forwardingMessages = new ArrayList<Message> ();

		for (Message message : MessageEnvelope.fromGraph(innerGraph).getMessages()) {

			forwardingMessages.add(MessagingCloneUtil.cloneMessage(message, true));
		}

		// return forwarding messages

		return new IteratorListMaker<Message> (MessageEnvelope.fromGraph(innerGraph).getMessages()).list();
	}

	private void send(Message forwardingMessage, Operation operation, Graph operationResultGraph, ExecutionContext executionContext) throws Xdi2MessagingException {

		// find route for forwarding message

		XDIArc toPeerRootXDIArc = forwardingMessage.getToPeerRootXDIArc();

		XDIClientRoute<? extends XDIClient<? extends MessagingResponse>> xdiClientRoute;

		try {

			xdiClientRoute = this.getXdiAgent().route(toPeerRootXDIArc);
		} catch (Xdi2AgentException ex) {

			throw new Xdi2MessagingException("Agent problem while routing to " + toPeerRootXDIArc + ": " + ex.getMessage(), ex, executionContext);
		} catch (Xdi2ClientException ex) {

			throw new Xdi2MessagingException("Client problem while routing to " + toPeerRootXDIArc + ": " + ex.getMessage(), ex, executionContext);
		}

		// disable link contracts in case the forwarding message is routed back to us

		AbstractMessagingTarget messagingTarget = (AbstractMessagingTarget) executionContext.getCurrentMessagingTarget();

		LinkContractInterceptor linkContractInterceptor = messagingTarget.getInterceptors().getInterceptor(LinkContractInterceptor.class);
		if (linkContractInterceptor != null) linkContractInterceptor.setDisabledForMessage(forwardingMessage);

		// send the forwarding message

		XDIClient<? extends MessagingResponse> xdiClient = xdiClientRoute.constructXDIClient();

		try {

			MessagingResponse forwardingMessagingResponse = xdiClient.send(forwardingMessage.getMessageEnvelope());

			// TODO: what if we get a FutureMessagingResponse from an XDIWebSocketClient?

			CopyUtil.copyGraph(forwardingMessagingResponse.getResultGraph(), operationResultGraph, null);
		} catch (Xdi2ClientException ex) {

			throw new Xdi2MessagingException("Problem while sending forwarding message " + forwardingMessage + " to " + toPeerRootXDIArc + ": " + ex.getMessage(), ex, executionContext);
		}
	}

	/*
	 * Getters and setters
	 */

	public XDIAgent getXdiAgent() {

		return this.xdiAgent;
	}

	public void setXdiAgent(XDIAgent xdiAgent) {

		this.xdiAgent = xdiAgent;
	}

	/*
	 * ExecutionContext helper methods
	 */

	private static final String EXECUTIONCONTEXT_KEY_FORWARDINGMESSAGES_PER_OPERATION = SendInterceptor.class.getCanonicalName() + "#forwardingmessagesperoperation";
	private static final String EXECUTIONCONTEXT_KEY_NEWID_PER_OPERATION = SendInterceptor.class.getCanonicalName() + "#newidperoperation";

	@SuppressWarnings("unchecked")
	public static List<Message> getForwardingMessages(ExecutionContext executionContext) {

		return (List<Message>) executionContext.getOperationAttribute(EXECUTIONCONTEXT_KEY_FORWARDINGMESSAGES_PER_OPERATION);
	}

	public static void putForwardingMessages(ExecutionContext executionContext, List<Message> forwardingMessages) {

		executionContext.putOperationAttribute(EXECUTIONCONTEXT_KEY_FORWARDINGMESSAGES_PER_OPERATION, forwardingMessages);
	}

	public static Boolean getNewId(ExecutionContext executionContext) {

		return (Boolean) executionContext.getMessageAttribute(EXECUTIONCONTEXT_KEY_NEWID_PER_OPERATION);
	}

	public static void putNewId(ExecutionContext executionContext, Boolean newId) {

		executionContext.putMessageAttribute(EXECUTIONCONTEXT_KEY_NEWID_PER_OPERATION, newId);
	}
}
