package xdi2.core.impl.wrapped;

import java.util.Iterator;

import xdi2.core.ContextNode;
import xdi2.core.Literal;
import xdi2.core.Relation;
import xdi2.core.impl.AbstractContextNode;
import xdi2.core.impl.memory.MemoryContextNode;
import xdi2.core.impl.memory.MemoryLiteral;
import xdi2.core.impl.memory.MemoryRelation;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.syntax.XDIArc;
import xdi2.core.util.iterators.MappingIterator;
import xdi2.core.util.iterators.ReadOnlyIterator;

public class WrappedContextNode extends AbstractContextNode implements ContextNode {

	private static final long serialVersionUID = 4930852359817860369L;

	private MemoryContextNode memoryContextNode;

	WrappedContextNode(WrappedGraph graph, WrappedContextNode contextNode, MemoryContextNode memoryContextNode) {

		super(graph, contextNode);

		this.memoryContextNode = memoryContextNode;
	}

	@Override
	public XDIArc getXDIArc() {

		return this.memoryContextNode.getXDIArc();
	}

	/*
	 * Methods related to context nodes of this context node
	 */

	@Override
	public synchronized ContextNode setContextNode(XDIArc arc) {

		MemoryContextNode ret = (MemoryContextNode) this.memoryContextNode.setContextNode(arc);

		return new WrappedContextNode((WrappedGraph) this.getGraph(), this, ret);
	}

	@Override
	public ContextNode getContextNode(XDIArc arc, boolean subgraph) {

		MemoryContextNode ret = (MemoryContextNode) this.memoryContextNode.getContextNode(arc, subgraph);

		return ret == null ? null : new WrappedContextNode((WrappedGraph) this.getGraph(), this, ret);
	}

	@Override
	public ReadOnlyIterator<ContextNode> getContextNodes() {

		ReadOnlyIterator<ContextNode> ret = this.memoryContextNode.getContextNodes();

		return new ReadOnlyIterator<ContextNode> (new WrappedContextNodeMappingIterator(ret));
	}

	@Override
	public boolean containsContextNode(XDIArc arc) {

		return this.memoryContextNode.containsContextNode(arc);
	}

	@Override
	public boolean containsContextNodes() {

		return this.memoryContextNode.containsContextNodes();
	}

	@Override
	public synchronized void delContextNode(XDIArc arc) {

		this.memoryContextNode.delContextNode(arc);
	}

	@Override
	public synchronized void delContextNodes() {

		this.memoryContextNode.delContextNodes();
	}

	/*
	 * Methods related to relations of this context node
	 */

	@Override
	public synchronized Relation setRelation(XDIAddress arc, ContextNode targetContextNode) {

		MemoryRelation ret = (MemoryRelation) this.memoryContextNode.setRelation(arc, targetContextNode);

		return new WrappedRelation(this, ret);
	}

	@Override
	public Relation getRelation(XDIAddress arc, XDIAddress targetContextNodeAddress) {

		MemoryRelation ret = (MemoryRelation) this.memoryContextNode.getRelation(arc, targetContextNodeAddress);

		return ret == null ? null : new WrappedRelation(this, ret);
	}

	@Override
	public ReadOnlyIterator<Relation> getRelations(XDIAddress arc) {

		ReadOnlyIterator<Relation> ret = this.memoryContextNode.getRelations(arc);

		return new ReadOnlyIterator<Relation> (new WrappedRelationMappingIterator(ret));
	}

	@Override
	public ReadOnlyIterator<Relation> getRelations() {

		ReadOnlyIterator<Relation> ret = this.memoryContextNode.getRelations();

		return new ReadOnlyIterator<Relation> (new WrappedRelationMappingIterator(ret));
	}

	@Override
	public boolean containsRelation(XDIAddress arc, XDIAddress targetContextNodeAddress) {

		return this.memoryContextNode.containsRelation(arc, targetContextNodeAddress);
	}

	@Override
	public boolean containsRelations(XDIAddress arc) {

		return this.memoryContextNode.containsRelations(arc);
	}

	@Override
	public boolean containsRelations() {

		return this.memoryContextNode.containsRelations();
	}

	@Override
	public synchronized void delRelation(XDIAddress arc, XDIAddress targetContextNodeAddress) {

		this.memoryContextNode.delRelation(arc, targetContextNodeAddress);
	}

	@Override
	public synchronized void delRelations(XDIAddress arc) {

		this.memoryContextNode.delRelations(arc);
	}

	@Override
	public synchronized void delRelations() {

		this.memoryContextNode.delRelations();
	}

	/*
	 * Methods related to literals of this context node
	 */

	@Override
	public synchronized Literal setLiteral(Object literalData) {

		MemoryLiteral ret = (MemoryLiteral) this.memoryContextNode.setLiteral(literalData);

		return ret == null ? null : new WrappedLiteral(this, ret);
	}

	@Override
	public Literal getLiteral() {

		MemoryLiteral ret = (MemoryLiteral) this.memoryContextNode.getLiteral();

		return ret == null ? null : new WrappedLiteral(this, ret);
	}

	@Override
	public boolean containsLiteral() {

		return this.memoryContextNode.containsLiteral();
	}

	@Override
	public synchronized void delLiteral() {

		this.memoryContextNode.delLiteral();
	}

	private class WrappedContextNodeMappingIterator extends MappingIterator<ContextNode, ContextNode> {

		public WrappedContextNodeMappingIterator(Iterator<ContextNode> iterator) {

			super(iterator);
		}

		@Override
		public ContextNode map(ContextNode memoryContextNode) {

			return new WrappedContextNode((WrappedGraph) WrappedContextNode.this.getGraph(), WrappedContextNode.this, (MemoryContextNode) memoryContextNode);
		}
	}

	private class WrappedRelationMappingIterator extends MappingIterator<Relation, Relation> {

		public WrappedRelationMappingIterator(Iterator<Relation> iterator) {

			super(iterator);
		}

		@Override
		public Relation map(Relation memoryRelation) {

			return new WrappedRelation(WrappedContextNode.this, (MemoryRelation) memoryRelation);
		}
	}
}
