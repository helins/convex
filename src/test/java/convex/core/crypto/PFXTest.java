package convex.core.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.junit.jupiter.api.Test;

import convex.core.Init;

public class PFXTest {

	@Test public void testNewStore() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, SecurityException, SignatureException, UnrecoverableKeyException {
		File f=File.createTempFile("temp-keystore", "pfx");
		
		PFXUtils.createStore(f, "test");
		
		// check password is being applied
		assertThrows(IOException.class,()->PFXUtils.loadStore(f,"foobar"));
		
		// don't throw, no integrity checking on null?
		//assertThrows(IOException.class,()->PFXUtils.loadStore(f,null));
		
		KeyStore ks=PFXUtils.loadStore(f, "test");
		AKeyPair kp=Init.HERO_KP;
		PFXUtils.saveKey(ks, kp, "thehero");
		PFXUtils.saveStore(ks, f, "test");
		
		String alias=Init.HERO_KP.getAccountKey().toHexString();
		KeyStore ks2=PFXUtils.loadStore(f, "test");
		assertEquals(alias,ks2.aliases().asIterator().next());
		
		AKeyPair kp2=PFXUtils.getKeyPair(ks2,alias, "thehero");
		assertEquals(kp.signData(1L).getEncoding(),kp2.signData(1L).getEncoding());
	}
}