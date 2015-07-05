package xdi2.client.impl;

import xdi2.client.XDIClient;
import xdi2.client.XDIClientRoute;
import xdi2.core.syntax.XDIArc;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.constants.XDIMessagingConstants;

public abstract class XDIAbstractClientRoute <CLIENT extends XDIClient> implements XDIClientRoute<CLIENT> {

	private XDIArc toPeerRootXDIArc;

	public XDIAbstractClientRoute(XDIArc toPeerRootXDIArc) {

		this.toPeerRootXDIArc = toPeerRootXDIArc;
	}

	@Override
	public XDIArc getToPeerRootXDIArc() {

		return this.toPeerRootXDIArc;
	}

	@Override
	public MessageEnvelope constructMessageEnvelope() {

		MessageEnvelope messageEnvelope = new MessageEnvelope();

		return messageEnvelope;
	}

	@Override
	public Message constructMessage(MessageEnvelope messageEnvelope) {

		Message message = messageEnvelope.createMessage(XDIMessagingConstants.XDI_ADD_ANONYMOUS);
		message.setToPeerRootXDIArc(this.getToPeerRootXDIArc());

		return message;
	}
}
