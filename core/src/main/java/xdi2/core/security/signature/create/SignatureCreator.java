package xdi2.core.security.signature.create;

import java.security.GeneralSecurityException;

import xdi2.core.ContextNode;
import xdi2.core.features.signatures.Signature;
import xdi2.core.syntax.XDIAddress;

/**
 * A SignatureCreator can create an XDI Signature using an XDI address
 * that identifies the signer.
 */
public interface SignatureCreator <SIGNATURE extends Signature> {

	public boolean canCreate(Class<? extends SIGNATURE> clazz);

	/**
	 * Create a signature.
	 */
	public SIGNATURE createSignature(ContextNode contextNode, XDIAddress signerXDIAddress) throws GeneralSecurityException;

	/**
	 * Create a signature.
	 */
	public SIGNATURE createSignature(ContextNode contextNode) throws GeneralSecurityException;

	/**
	 * Create a signature.
	 */
	public void createSignature(SIGNATURE signature, XDIAddress signerXDIAddress) throws GeneralSecurityException;

	/**
	 * Create a signature.
	 */
	public void createSignature(SIGNATURE signature) throws GeneralSecurityException;
}
