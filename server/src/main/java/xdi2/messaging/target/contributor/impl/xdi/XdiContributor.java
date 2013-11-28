package xdi2.messaging.target.contributor.impl.xdi;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.client.XDIClient;
import xdi2.client.exceptions.Xdi2ClientException;
import xdi2.client.http.XDIHttpClient;
import xdi2.core.features.nodetypes.XdiPeerRoot;
import xdi2.core.util.CopyUtil;
import xdi2.core.util.StatementUtil;
import xdi2.core.util.XDI3Util;
import xdi2.core.xri3.XDI3Segment;
import xdi2.core.xri3.XDI3Statement;
import xdi2.discovery.XDIDiscoveryClient;
import xdi2.discovery.XDIDiscoveryResult;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.MessageResult;
import xdi2.messaging.Operation;
import xdi2.messaging.exceptions.Xdi2MessagingException;
import xdi2.messaging.target.ExecutionContext;
import xdi2.messaging.target.MessagingTarget;
import xdi2.messaging.target.Prototype;
import xdi2.messaging.target.contributor.AbstractContributor;
import xdi2.messaging.target.contributor.ContributorXri;
import xdi2.messaging.target.contributor.impl.xdi.manipulator.MessageEnvelopeManipulator;
import xdi2.messaging.target.contributor.impl.xdi.manipulator.MessageResultManipulator;
import xdi2.messaging.target.interceptor.MessageInterceptor;
import xdi2.messaging.util.MessagingCloneUtil;

/**
 * This contributor can answer request by forwarding them to another XDI endpoint.
 */
@ContributorXri(addresses={"{{=@+*!$}}$keypair", "{{(=@+*!$)}}$keypair", "$keypair", "{{=@+*!$}}<$key>", "{{(=@+*!$)}}<$key>", "<$key>"})
public class XdiContributor extends AbstractContributor implements MessageInterceptor, Prototype<XdiContributor> {

	private static final Logger log = LoggerFactory.getLogger(XdiContributor.class);

	private XDI3Segment toAuthority;
	private XDIClient xdiClient;

	private XDIDiscoveryClient xdiDiscoveryClient;

	private List<MessageEnvelopeManipulator> messageEnvelopeManipulators;
	private List<MessageResultManipulator> messageResultManipulators;

	public XdiContributor() {

		this.xdiDiscoveryClient = new XDIDiscoveryClient();
	}

	/*
	 * Prototype
	 */

	@Override
	public XdiContributor instanceFor(xdi2.messaging.target.Prototype.PrototypingContext prototypingContext) throws Xdi2MessagingException {

		// done

		return this;
	}

	/*
	 * Init and shutdown
	 */

	@Override
	public void init(MessagingTarget messagingTarget) throws Exception {

		super.init(messagingTarget);
	}

	/*
	 * Contributor methods
	 */

	@Override
	public boolean executeOnAddress(XDI3Segment[] contributorXris, XDI3Segment contributorsXri, XDI3Segment relativeTargetAddress, Operation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		// check forwarding target

		XDI3Segment toAuthority = getToAuthority(executionContext);
		XDIClient xdiClient = getXdiClient(executionContext);

		if (toAuthority == null || xdiClient == null) return false;

		// prepare the forwarding message envelope

		XDI3Segment targetAddress = XDI3Util.concatXris(contributorsXri, relativeTargetAddress);

		Message forwardingMessage = MessagingCloneUtil.cloneMessage(operation.getMessage());

		forwardingMessage.deleteOperations();
		forwardingMessage.createOperation(operation.getOperationXri(), targetAddress);

		MessageEnvelope forwardingMessageEnvelope = forwardingMessage.getMessageEnvelope();

		// manipulate the forwarding message envelope

		for (MessageEnvelopeManipulator messageEnvelopeManipulator : this.messageEnvelopeManipulators) {

			messageEnvelopeManipulator.manipulate(forwardingMessageEnvelope, executionContext);
		}

		// prepare the forwarding message result

		MessageResult forwardingMessageResult = new MessageResult();

		// send the forwarding message envelope

		try {

			if (log.isDebugEnabled() && this.getXdiClient() instanceof XDIHttpClient) log.debug("Forwarding operation " + operation.getOperationXri() + " on target address " + targetAddress + " to " + ((XDIHttpClient) this.getXdiClient()).getEndpointUri() + ".");

			this.getXdiClient().send(forwardingMessageEnvelope, forwardingMessageResult);
		} catch (Xdi2ClientException ex) {

			throw new Xdi2MessagingException("Problem while forwarding XDI request: " + ex.getMessage(), ex, executionContext);
		}

		// manipulate the forwarding message result

		for (MessageResultManipulator messageResultManipulator : this.messageResultManipulators) {

			if (log.isDebugEnabled()) log.debug("Executing message result manipulator " + messageResultManipulator.getClass().getSimpleName() + " with operation " + operation.getOperationXri() + " on address " + targetAddress + ".");

			messageResultManipulator.manipulate(forwardingMessageResult, executionContext);
		}

		// done

		CopyUtil.copyGraph(forwardingMessageResult.getGraph(), messageResult.getGraph(), null);

		return true;
	}

	@Override
	public boolean executeOnStatement(XDI3Segment[] contributorXris, XDI3Segment contributorsXri, XDI3Statement relativeTargetStatement, Operation operation, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		// prepare the forwarding message envelope

		XDI3Statement targetStatement = StatementUtil.concatXriStatement(contributorsXri, relativeTargetStatement, true);

		Message forwardingMessage = MessagingCloneUtil.cloneMessage(operation.getMessage());

		forwardingMessage.deleteOperations();
		forwardingMessage.createOperation(operation.getOperationXri(), targetStatement);

		MessageEnvelope forwardingMessageEnvelope = forwardingMessage.getMessageEnvelope();

		// manipulate the forwarding message envelope

		for (MessageEnvelopeManipulator messageEnvelopeManipulator : this.messageEnvelopeManipulators) {

			messageEnvelopeManipulator.manipulate(forwardingMessageEnvelope, executionContext);
		}

		// prepare the forwarding message result

		MessageResult forwardingMessageResult = new MessageResult();

		// send the forwarding message envelope

		try {

			if (log.isDebugEnabled() && this.getXdiClient() instanceof XDIHttpClient) log.debug("Forwarding operation " + operation.getOperationXri() + " on target statement " + targetStatement + " to " + ((XDIHttpClient) this.getXdiClient()).getEndpointUri() + ".");

			this.getXdiClient().send(forwardingMessageEnvelope, forwardingMessageResult);
		} catch (Xdi2ClientException ex) {

			throw new Xdi2MessagingException("Problem while forwarding XDI request: " + ex.getMessage(), ex, executionContext);
		}

		// manipulate the forwarding message result

		for (MessageResultManipulator messageResultManipulator : this.messageResultManipulators) {

			if (log.isDebugEnabled()) log.debug("Executing message result manipulator " + messageResultManipulator.getClass().getSimpleName() + " with operation " + operation.getOperationXri() + " on statement " + targetStatement + ".");

			messageResultManipulator.manipulate(forwardingMessageResult, executionContext);
		}

		// done

		CopyUtil.copyGraph(forwardingMessageResult.getGraph(), messageResult.getGraph(), null);

		return true;
	}

	/*
	 * Getters and setters
	 */

	public XDIClient getXdiClient() {

		return this.xdiClient;
	}

	public void setXdiClient(XDIClient xdiClient) {

		this.xdiClient = xdiClient;
	}

	public XDI3Segment getToAuthority() {

		return this.toAuthority;
	}

	public void setToAuthority(XDI3Segment toAuthority) {

		this.toAuthority = toAuthority;
	}

	public XDIDiscoveryClient getXdiDiscoveryClient() {

		return this.xdiDiscoveryClient;
	}

	public void setXdiDiscoveryClient(XDIDiscoveryClient xdiDiscoveryClient) {

		this.xdiDiscoveryClient = xdiDiscoveryClient;
	}

	/*
	 * MessageInterceptor
	 */

	@Override
	public boolean before(Message message, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		// if there is a static forwarding target, we use it

		if (this.getToAuthority() != null && this.getXdiClient() != null) {

			// just use the configured forwarding target

			if (log.isDebugEnabled()) log.debug("Static forwarding target: " + this.getToAuthority() + " (" + this.getXdiClient() + ")");

			putToAuthority(executionContext, this.getToAuthority());
			putXdiClient(executionContext, this.getXdiClient());

			return false;
		}

		// if the target is local, then there is no forwarding target

		MessagingTarget messagingTarget = executionContext.getCurrentMessagingTarget();
		XDI3Segment ownerAuthority = messagingTarget.getOwnerAuthority();
		XDI3Segment toAuthority = message.getToAuthority();

		if (log.isDebugEnabled()) log.debug("ownerAuthority=" + ownerAuthority + ", toAuthority=" + toAuthority);

		if (toAuthority == null) return false;
		if (toAuthority.equals(ownerAuthority)) return false;

		// no static forwarding target, and target is not local, so we discover the forwarding target

		XDIDiscoveryResult xdiDiscoveryResult;

		try {

			xdiDiscoveryResult = this.getXdiDiscoveryClient().discoverFromRegistry(XdiPeerRoot.getXriOfPeerRootArcXri(toAuthority.getFirstSubSegment()), null);
		} catch (Xdi2ClientException ex) {

			throw new Xdi2MessagingException("XDI Discovery failed on " + toAuthority + ": " + ex.getMessage(), ex, executionContext);
		}

		if (xdiDiscoveryResult.getCloudNumber() == null) throw new Xdi2MessagingException("Could not discover Cloud Number for " + toAuthority, null, executionContext);
		if (xdiDiscoveryResult.getXdiEndpointUri() == null) throw new Xdi2MessagingException("Could not discover XDI endpoint URI for " + toAuthority, null, executionContext);

		putToAuthority(executionContext, xdiDiscoveryResult.getCloudNumber().getPeerRootXri());
		putXdiClient(executionContext, new XDIHttpClient(xdiDiscoveryResult.getXdiEndpointUri()));

		return false;
	}

	@Override
	public boolean after(Message message, MessageResult messageResult, ExecutionContext executionContext) throws Xdi2MessagingException {

		return false;
	}

	/*
	 * ExecutionContext helper methods
	 */

	private static final String EXECUTIONCONTEXT_KEY_TO_AUTHORITY_PER_MESSAGE = XdiContributor.class.getCanonicalName() + "#toauthoritypermessage";
	private static final String EXECUTIONCONTEXT_KEY_XDI_CLIENT_PER_MESSAGE = XdiContributor.class.getCanonicalName() + "#xdiclientpermessage";

	public static XDI3Segment getToAuthority(ExecutionContext executionContext) {

		return (XDI3Segment) executionContext.getMessageAttribute(EXECUTIONCONTEXT_KEY_TO_AUTHORITY_PER_MESSAGE);
	}

	public static void putToAuthority(ExecutionContext executionContext, XDI3Segment toAuthority) {

		executionContext.putMessageAttribute(EXECUTIONCONTEXT_KEY_TO_AUTHORITY_PER_MESSAGE, toAuthority);
	}

	public static XDIClient getXdiClient(ExecutionContext executionContext) {

		return (XDIClient) executionContext.getMessageAttribute(EXECUTIONCONTEXT_KEY_XDI_CLIENT_PER_MESSAGE);
	}

	public static void putXdiClient(ExecutionContext executionContext, XDIClient xdiClient) {

		executionContext.putMessageAttribute(EXECUTIONCONTEXT_KEY_XDI_CLIENT_PER_MESSAGE, xdiClient);
	}
}
