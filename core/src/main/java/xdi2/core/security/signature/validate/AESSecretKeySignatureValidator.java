package xdi2.core.security.signature.validate;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xdi2.core.features.signatures.AESSignature;
import xdi2.core.syntax.XDIAddress;

/**
 * This is an AESSignatureValidator that validate an XDI AESSignature using a secret key,
 * which can be obtained using the XDI address that identifies the signer.
 */
public abstract class AESSecretKeySignatureValidator extends AbstractAESSignatureValidator {

	private static Logger log = LoggerFactory.getLogger(AESSecretKeySignatureValidator.class.getName());

	public AESSecretKeySignatureValidator() {

	}

	@Override
	public boolean validate(byte[] normalizedSerialization, byte[] signatureValue, AESSignature signature, XDIAddress signerXDIAddress) throws GeneralSecurityException {

		// obtain secret key

		SecretKey secretKey = this.getSecretKey(signerXDIAddress);

		if (secretKey == null) {

			if (log.isDebugEnabled()) log.debug("No secret key found for " + signerXDIAddress);
			return false;
		}

		if (log.isDebugEnabled()) log.debug("Secret key found for " + signerXDIAddress + ": " + new String(Base64.encodeBase64(secretKey.getEncoded()), Charset.forName("UTF-8")));

		// validate

		String jceAlgorithm = signature.getJCEAlgorithm();

		if (log.isDebugEnabled()) log.debug("Validating for " + signature.getClass().getSimpleName() + " with algorithm " + jceAlgorithm);

		Mac jceMac = Mac.getInstance(jceAlgorithm);
		jceMac.init(secretKey);
		jceMac.update(normalizedSerialization);

		return Arrays.equals(signatureValue, jceMac.doFinal());
	}

	protected abstract SecretKey getSecretKey(XDIAddress signerXDIAddress) throws GeneralSecurityException;

	/*
	 * Helper methods
	 */

	public static SecretKey aesSecretKeyFromSecretKeyString(String secretKeyString) throws GeneralSecurityException {

		if (secretKeyString == null) return null;

		byte[] secretKeyBytes = Base64.decodeBase64(secretKeyString.getBytes(Charset.forName("UTF-8")));

		return new SecretKeySpec(secretKeyBytes, 0, secretKeyBytes.length, "AES");
	}
}
