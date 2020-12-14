package xdi2.messaging.container.interceptor.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.agent.XDIAgent;
import xdi2.agent.impl.XDIBasicAgent;
import xdi2.agent.routing.impl.http.XDIHttpDiscoveryAgentRouter;
import xdi2.client.XDIClient;
import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.constants.XDIConstants;
import xdi2.core.constants.XDIDictionaryConstants;
import xdi2.core.constants.XDILinkContractConstants;
import xdi2.core.constants.XDITimestampsConstants;
import xdi2.core.features.equivalence.Equivalence;
import xdi2.core.features.linkcontracts.instance.ConnectLinkContract;
import xdi2.core.features.linkcontracts.instance.PublicLinkContract;
import xdi2.core.features.linkcontracts.instance.RootLinkContract;
import xdi2.core.features.linkcontracts.instance.SendLinkContract;
import xdi2.core.features.nodetypes.XdiCommonRoot;
import xdi2.core.features.nodetypes.XdiPeerRoot;
import xdi2.core.features.nodetypes.XdiPeerRoot.MappingContextNodePeerRootIterator;
import xdi2.core.features.policy.PolicyAnd;
import xdi2.core.features.policy.PolicyOr;
import xdi2.core.features.policy.PolicyRoot;
import xdi2.core.features.policy.PolicyUtil;
import xdi2.core.features.timestamps.Timestamps;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.syntax.XDIStatement;
import xdi2.core.util.CopyUtil;
import xdi2.core.util.CopyUtil.CompoundCopyStrategy;
import xdi2.core.util.CopyUtil.CopyStrategy;
import xdi2.core.util.CopyUtil.ReplaceRegexLiteralStringCopyStrategy;
import xdi2.core.util.CopyUtil.ReplaceXDIAddressCopyStrategy;
import xdi2.core.util.XDIAddressUtil;
import xdi2.core.util.iterators.IteratorArrayMaker;
import xdi2.discovery.XDIDiscoveryClient;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.container.MessagingContainer;
import xdi2.messaging.container.Prototype;
import xdi2.messaging.container.exceptions.Xdi2MessagingException;
import xdi2.messaging.container.execution.ExecutionContext;
import xdi2.messaging.container.execution.ExecutionResult;
import xdi2.messaging.container.impl.graph.GraphMessagingContainer;
import xdi2.messaging.container.interceptor.impl.linkcontract.LinkContractInterceptor;

/**
 * This interceptor can initialize an empty XDI graph with basic bootstrapping data,
 * such as the owner XDI address of the graph, and initial link contracts.
 * 
 * @author markus
 */
public class BootstrapInterceptor extends AbstractInterceptor<MessagingContainer> implements Prototype<BootstrapInterceptor> {

	private static Logger log = LoggerFactory.getLogger(BootstrapInterceptor.class.getName());

	public final static int INIT_PRIORITY = 150;
	public final static int SHUTDOWN_PRIORITY = 150;

	public final static XDIArc XDI_ARC_SELF = XDIArc.create("{$self}");

	private XDIAddress bootstrapOwner;
	private XDIAddress[] bootstrapOwnerSynonyms;
	private boolean bootstrapRootLinkContract;
	private boolean bootstrapPublicLinkContract;
	private boolean bootstrapConnectLinkContract;
	private boolean bootstrapSendLinkContract;
	private boolean bootstrapTimestamp;
	private Graph bootstrapGraph;
	private MessageEnvelope bootstrapMessageEnvelope;

	public BootstrapInterceptor() {

		super(INIT_PRIORITY, SHUTDOWN_PRIORITY);

		this.bootstrapOwner = null;
		this.bootstrapOwnerSynonyms = null;
		this.bootstrapRootLinkContract = false;
		this.bootstrapPublicLinkContract = false;
		this.bootstrapConnectLinkContract = false;
		this.bootstrapSendLinkContract = false;
		this.bootstrapTimestamp = false;
		this.bootstrapGraph = null;
		this.bootstrapMessageEnvelope = null;
	}

	/*
	 * Prototype
	 */

	@Override
	public BootstrapInterceptor instanceFor(PrototypingContext prototypingContext) {

		// create new interceptor

		BootstrapInterceptor interceptor = new BootstrapInterceptor();

		// set the owner, root link contract, public link contract, connect link contract, send link contract

		interceptor.setBootstrapOwner(prototypingContext.getOwnerXDIAddress());
		interceptor.setBootstrapRootLinkContract(this.getBootstrapRootLinkContract());
		interceptor.setBootstrapPublicLinkContract(this.getBootstrapPublicLinkContract());
		interceptor.setBootstrapConnectLinkContract(this.getBootstrapConnectLinkContract());
		interceptor.setBootstrapSendLinkContract(this.getBootstrapSendLinkContract());

		// set the owner synonyms

		XDIAddress[] bootstrapOwnerSynonyms = null;

		if (prototypingContext.getOwnerXdiPeerRoot() != null) {

			Iterator<ContextNode> ownerSynonymPeerRootContextNodes = Equivalence.getIncomingReferenceContextNodes(prototypingContext.getOwnerXdiPeerRoot().getContextNode());
			XdiPeerRoot[] ownerSynonymPeerRoots = (new IteratorArrayMaker<XdiPeerRoot> (new MappingContextNodePeerRootIterator(ownerSynonymPeerRootContextNodes))).array(XdiPeerRoot.class);

			bootstrapOwnerSynonyms = new XDIAddress[ownerSynonymPeerRoots.length];
			for (int i=0; i<bootstrapOwnerSynonyms.length; i++) bootstrapOwnerSynonyms[i] = ownerSynonymPeerRoots[i].getXDIAddressOfPeerRoot();
		}

		interceptor.setBootstrapOwnerSynonyms(bootstrapOwnerSynonyms);

		// set bootstrap statements and operations

		interceptor.setBootstrapGraph(this.getBootstrapGraph());
		interceptor.setBootstrapMessageEnvelope(this.getBootstrapMessageEnvelope());

		// done

		return interceptor;
	}

	/*
	 * Init and shutdown
	 */

	@Override
	public void init(MessagingContainer messagingContainer) throws Exception {

		super.init(messagingContainer);

		if (! (messagingContainer instanceof GraphMessagingContainer)) return;

		GraphMessagingContainer graphMessagingContainer = (GraphMessagingContainer) messagingContainer;
		Graph graph = graphMessagingContainer.getGraph();

		if (log.isDebugEnabled()) log.debug("bootstrapOwner=" + this.getBootstrapOwner() + ", bootstrapOwnerSynonyms=" + (this.getBootstrapOwnerSynonyms() == null ? null : Arrays.asList(this.getBootstrapOwnerSynonyms())) + ", bootstrapLinkContract=" + this.getBootstrapRootLinkContract() + ", bootstrapPublicLinkContract=" + this.getBootstrapPublicLinkContract() + ", bootstrapConnectLinkContract=" + this.getBootstrapConnectLinkContract() + ", bootstrapSendLinkContract=" + this.getBootstrapSendLinkContract() + ", bootstrapGraph=" + (this.getBootstrapGraph() != null) + ", bootstrapMessageEnvelope=" + (this.getBootstrapMessageEnvelope() != null));

		// check if the owner statement exists

		XdiPeerRoot selfXdiPeerRoot = XdiCommonRoot.findCommonRoot(graph).getSelfPeerRoot();

		if (selfXdiPeerRoot != null) {

			if (selfXdiPeerRoot.getXDIAddressOfPeerRoot().equals(this.getBootstrapOwner())) {

				if (log.isDebugEnabled()) log.debug("Owner statement for " + selfXdiPeerRoot.getXDIAddressOfPeerRoot() + " exists already.");
				return;
			} else {

				XDIAddress tempAddress = XDIAddress.create(this.getBootstrapOwner().toString().replace(":did:sov:", ":did:sov:myidsafe:"));

				if (log.isDebugEnabled()) log.debug("Owner statement for " + selfXdiPeerRoot.getXDIAddressOfPeerRoot() + " differs from " + this.getBootstrapOwner());
				if (log.isDebugEnabled()) log.debug("Starting migration flow with temp address: " + tempAddress);

				XDIAgent xdiAgent = new XDIBasicAgent(new XDIHttpDiscoveryAgentRouter(new XDIDiscoveryClient("https://xdi.uniresolver.io/")));
//				XDIDiscoveryClient client = new XDIDiscoveryClient("http://localhost:9501/");
//				XDIDiscoveryResult result = client.discoverFromRegistry(XDIAddress.create("=!:did:sov:myidsafe:KBxwVYaJVREXHRvGaJUoEM"));

				XDIClient<?> xdiClient = xdiAgent.route(tempAddress).constructXDIClient();
				MessageEnvelope me = new MessageEnvelope();
				Message m = me.createMessage();
				m.setFromXDIAddress(selfXdiPeerRoot.getXDIAddressOfPeerRoot());
				m.setToXDIAddress(this.getBootstrapOwner());
				m.setLinkContractClass(PublicLinkContract.class);
				m.createGetOperation(XDIConstants.XDI_ADD_ROOT);
				Graph resultGraph = xdiClient.send(me).getResultGraph();
				graph.clear();
				CopyUtil.copyGraph(resultGraph, graph, null);
			}
		}

		// create bootstrap owner

		ContextNode bootstrapOwnerContextNode = null;
		ContextNode bootstrapOwnerSelfPeerRootContextNode = null;

		if (this.getBootstrapOwner() != null) {

			if (log.isDebugEnabled()) log.debug("Creating bootstrap owner: " + this.getBootstrapOwner());

			bootstrapOwnerContextNode = graph.setDeepContextNode(this.getBootstrapOwner());
			bootstrapOwnerSelfPeerRootContextNode = XdiCommonRoot.findCommonRoot(graph).setSelfPeerRoot(this.getBootstrapOwner()).getContextNode();

			// create bootstrap owner synonyms

			if (this.getBootstrapOwnerSynonyms() != null) {

				if (log.isDebugEnabled()) log.debug("Creating bootstrap owner synonyms: " + Arrays.asList(this.getBootstrapOwnerSynonyms()));

				for (XDIAddress bootstrapOwnerSynonym : this.getBootstrapOwnerSynonyms()) {

					ContextNode bootstrapOwnerSynonymContextNode = graph.setDeepContextNode(bootstrapOwnerSynonym);
					Equivalence.setReferenceContextNode(bootstrapOwnerSynonymContextNode, bootstrapOwnerContextNode, true);

					ContextNode bootstrapOwnerSynonymPeerRootContextNode = XdiCommonRoot.findCommonRoot(graph).getPeerRoot(bootstrapOwnerSynonym, true).getContextNode();
					Equivalence.setReferenceContextNode(bootstrapOwnerSynonymPeerRootContextNode, bootstrapOwnerSelfPeerRootContextNode, false);
				}
			}
		}

		// create bootstrap root link contract

		if (this.getBootstrapRootLinkContract()) {

			if (this.getBootstrapOwner() == null) {

				throw new Xdi2MessagingException("Can only create the bootstrap root link contract if a bootstrap owner is given.", null, null);
			}

			if (log.isDebugEnabled()) log.debug("Creating bootstrap root link contract.");

			RootLinkContract bootstrapRootLinkContract = RootLinkContract.findRootLinkContract(graph, true);
			bootstrapRootLinkContract.setPermissionTargetXDIAddress(XDILinkContractConstants.XDI_ADD_ALL, XDIConstants.XDI_ADD_ROOT);

			PolicyRoot policyRoot = bootstrapRootLinkContract.getPolicyRoot(true);

			PolicyAnd policyAnd = policyRoot.createAndPolicy(true);
			PolicyUtil.createSenderIsOperator(policyAnd, this.getBootstrapOwner());

			PolicyOr policyOr = policyAnd.createOrPolicy(true);
			PolicyUtil.createSecretTokenValidOperator(policyOr);
			PolicyUtil.createSignatureValidOperator(policyOr);
		}

		// create bootstrap public link contract

		if (this.getBootstrapPublicLinkContract()) {

			if (this.getBootstrapOwner() == null) {

				throw new Xdi2MessagingException("Can only create the bootstrap public link contract if a bootstrap owner is given.", null, null);
			}

			if (log.isDebugEnabled()) log.debug("Creating bootstrap public link contract.");

			PublicLinkContract bootstrapPublicLinkContract = PublicLinkContract.findPublicLinkContract(graph, true);
			XDIAddress publicAddress = XDIAddressUtil.concatXDIAddresses(this.getBootstrapOwner(), XDILinkContractConstants.XDI_ADD_PUBLIC);
			bootstrapPublicLinkContract.setPermissionTargetXDIAddress(XDILinkContractConstants.XDI_ADD_GET, publicAddress);

			XDIStatement selfPeerRootRefStatement = XDIStatement.fromRelationComponents(XDIConstants.XDI_ADD_ROOT, XDIDictionaryConstants.XDI_ADD_IS_REF, XDIConstants.XDI_ADD_COMMON_VARIABLE);
			bootstrapPublicLinkContract.setPermissionTargetXDIStatement(XDILinkContractConstants.XDI_ADD_GET, selfPeerRootRefStatement);

			XDIStatement bootstrapOwnerSynonymsIsRefStatement = XDIStatement.fromRelationComponents(this.getBootstrapOwner(), XDIDictionaryConstants.XDI_ADD_IS_REF, XDIConstants.XDI_ADD_COMMON_VARIABLE);
			bootstrapPublicLinkContract.setPermissionTargetXDIStatement(XDILinkContractConstants.XDI_ADD_GET, bootstrapOwnerSynonymsIsRefStatement);

			if (this.getBootstrapOwnerSynonyms() != null) {

				for (XDIAddress bootstrapOwnerSynonym : this.getBootstrapOwnerSynonyms()) {

					XDIStatement bootstrapOwnerSynonymRefStatement = XDIStatement.fromRelationComponents(bootstrapOwnerSynonym, XDIDictionaryConstants.XDI_ADD_REF, this.getBootstrapOwner());
					bootstrapPublicLinkContract.setPermissionTargetXDIStatement(XDILinkContractConstants.XDI_ADD_GET, bootstrapOwnerSynonymRefStatement);
				}
			}
		}

		// create bootstrap connect link contract

		if (this.getBootstrapConnectLinkContract()) {

			if (this.getBootstrapOwner() == null) {

				throw new Xdi2MessagingException("Can only create the bootstrap connect link contract if a bootstrap owner is given.", null, null);
			}

			if (log.isDebugEnabled()) log.debug("Creating bootstrap connect link contract.");

			ConnectLinkContract bootstrapConnectLinkContract = ConnectLinkContract.findConnectLinkContract(graph, true);
			bootstrapConnectLinkContract.setPermissionTargetXDIAddress(XDILinkContractConstants.XDI_ADD_CONNECT, XDIConstants.XDI_ADD_ROOT);

			PolicyRoot policyRoot = bootstrapConnectLinkContract.getPolicyRoot(true);
			policyRoot.createNotPolicy(true);

			PolicyRoot deferPushPolicyRoot = bootstrapConnectLinkContract.getDeferPushPolicyRoot(true);

			PolicyAnd deferPushPolicyAnd = deferPushPolicyRoot.createAndPolicy(true);
			PolicyUtil.createSignatureValidOperator(deferPushPolicyAnd);
		}

		// create bootstrap send link contract

		if (this.getBootstrapSendLinkContract()) {

			if (this.getBootstrapOwner() == null) {

				throw new Xdi2MessagingException("Can only create the bootstrap send link contract if a bootstrap owner is given.", null, null);
			}

			if (log.isDebugEnabled()) log.debug("Creating bootstrap send link contract.");

			SendLinkContract bootstrapSendLinkContract = SendLinkContract.findSendLinkContract(graph, true);
			bootstrapSendLinkContract.setPermissionTargetXDIAddress(XDILinkContractConstants.XDI_ADD_SEND, XDIConstants.XDI_ADD_ROOT);

			PolicyRoot policyRoot = bootstrapSendLinkContract.getPolicyRoot(true);
			policyRoot.createNotPolicy(true);

			PolicyRoot deferPolicyRoot = bootstrapSendLinkContract.getDeferPolicyRoot(true);

			PolicyAnd deferPolicyAnd = deferPolicyRoot.createAndPolicy(true);
			PolicyUtil.createSignatureValidOperator(deferPolicyAnd);
		}

		// create bootstrap timestamp

		if (this.getBootstrapTimestamp()) {

			Timestamps.setTimestamp(XdiCommonRoot.findCommonRoot(graph), XDITimestampsConstants.XDI_ADD_AS_CREATION, new Date());
		}

		// create bootstrap graph

		if (this.getBootstrapGraph() != null) {

			CopyStrategy copyStrategy = new CompoundCopyStrategy(
					new ReplaceXDIAddressCopyStrategy(XDI_ARC_SELF, this.getBootstrapOwner()),
					new ReplaceRegexLiteralStringCopyStrategy(Pattern.quote(XDI_ARC_SELF.toString()), this.getBootstrapOwner().toString()));

			Graph bootstrapGraph = MemoryGraphFactory.getInstance().openGraph();
			CopyUtil.copyGraph(this.getBootstrapGraph(), bootstrapGraph, copyStrategy);

			if (log.isDebugEnabled()) log.debug("Creating bootstrap graph: " + bootstrapGraph.toString());

			CopyUtil.copyGraph(bootstrapGraph, graph, null);

			bootstrapGraph.close();
		}

		// execute bootstrap message envelope

		if (this.getBootstrapMessageEnvelope() != null) {

			CopyStrategy copyStrategy = new ReplaceXDIAddressCopyStrategy(XDI_ARC_SELF, BootstrapInterceptor.this.getBootstrapOwner());

			MessageEnvelope bootstrapMessageEnvelope = new MessageEnvelope();
			CopyUtil.copyGraph(this.getBootstrapMessageEnvelope().getGraph(), bootstrapMessageEnvelope.getGraph(), copyStrategy);

			if (log.isDebugEnabled()) log.debug("Executing bootstrap message envelope: " + bootstrapMessageEnvelope.getGraph().toString());

			ToInterceptor toInterceptor = graphMessagingContainer.getInterceptors().getInterceptor(ToInterceptor.class);
			if (toInterceptor != null) toInterceptor.setDisabledForMessageEnvelope(bootstrapMessageEnvelope);

			RefInterceptor refInterceptor = graphMessagingContainer.getInterceptors().getInterceptor(RefInterceptor.class);
			if (refInterceptor != null) refInterceptor.setDisabledForMessageEnvelope(bootstrapMessageEnvelope);

			LinkContractInterceptor linkContractInterceptor = graphMessagingContainer.getInterceptors().getInterceptor(LinkContractInterceptor.class);
			if (linkContractInterceptor != null) linkContractInterceptor.setDisabledForMessageEnvelope(bootstrapMessageEnvelope);

			ExecutionContext executionContext = ExecutionContext.createExecutionContext();
			ExecutionResult executionResult = ExecutionResult.createExecutionResult(bootstrapMessageEnvelope);

			graphMessagingContainer.execute(bootstrapMessageEnvelope, executionContext, executionResult);
		}
	}

	@Override
	public void shutdown(MessagingContainer messagingContainer) throws Exception {

		super.shutdown(messagingContainer);
	}

	/*
	 * Getters and setters
	 */

	public XDIAddress getBootstrapOwner() {

		return this.bootstrapOwner;
	}

	public void setBootstrapOwner(XDIAddress bootstrapOwner) {

		this.bootstrapOwner = bootstrapOwner;
	}

	public XDIAddress[] getBootstrapOwnerSynonyms() {

		return this.bootstrapOwnerSynonyms;
	}

	public void setBootstrapOwnerSynonyms(XDIAddress[] bootstrapOwnerSynonyms) {

		this.bootstrapOwnerSynonyms = bootstrapOwnerSynonyms;
	}

	public void setBootstrapOwnerSynonyms(String[] bootstrapOwnerSynonyms) {

		this.bootstrapOwnerSynonyms = new XDIAddress[bootstrapOwnerSynonyms.length];
		for (int i=0; i<this.bootstrapOwnerSynonyms.length; i++) this.bootstrapOwnerSynonyms[i] = XDIAddress.create(bootstrapOwnerSynonyms[i]);
	}

	public boolean getBootstrapRootLinkContract() {

		return this.bootstrapRootLinkContract;
	}

	public void setBootstrapRootLinkContract(boolean bootstrapLinkContract) {

		this.bootstrapRootLinkContract = bootstrapLinkContract;
	}

	public boolean getBootstrapPublicLinkContract() {

		return this.bootstrapPublicLinkContract;
	}

	public void setBootstrapPublicLinkContract(boolean bootstrapPublicLinkContract) {

		this.bootstrapPublicLinkContract = bootstrapPublicLinkContract;
	}

	public boolean getBootstrapConnectLinkContract() {

		return this.bootstrapConnectLinkContract;
	}

	public void setBootstrapConnectLinkContract(boolean bootstrapConnectLinkContract) {

		this.bootstrapConnectLinkContract = bootstrapConnectLinkContract;
	}

	public boolean getBootstrapSendLinkContract() {

		return this.bootstrapSendLinkContract;
	}

	public void setBootstrapSendLinkContract(boolean bootstrapSendLinkContract) {

		this.bootstrapSendLinkContract = bootstrapSendLinkContract;
	}

	public boolean getBootstrapTimestamp() {

		return this.bootstrapTimestamp;
	}

	public void setBootstrapTimestamp(boolean bootstrapTimestamp) {

		this.bootstrapTimestamp = bootstrapTimestamp;
	}

	public Graph getBootstrapGraph() {

		return this.bootstrapGraph;
	}

	public void setBootstrapGraph(Graph bootstrapGraph) {

		this.bootstrapGraph = bootstrapGraph;
	}

	public MessageEnvelope getBootstrapMessageEnvelope() {

		return this.bootstrapMessageEnvelope;
	}

	public void setBootstrapMessageEnvelope(MessageEnvelope bootstrapMessageEnvelope) {

		this.bootstrapMessageEnvelope = bootstrapMessageEnvelope;
	}
}
