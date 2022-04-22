package com.ics.nceph.core.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.ssl.exception.SSLContextInitializationException;

/**
 * 
 * @author Anshul
 * @since 14-Mar-2022
 */
public class NcephSSLContext 
{
	/**
	 * 
	 * @return SSLContext
	 * @throws SSLContextInitializationException
	 */
	public static SSLContext getSSLContext() throws SSLContextInitializationException
	{
		 //Create/initialize the SSLContext with key material
		SSLContext sslContext = null;
		
		try 
		{
			sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(
				createKeyManagers(getFileInputStream(Configuration.APPLICATION_PROPERTIES.getConfig("keyStore.fileName")), "storepass", "keypass"), //TODO name should come from config file - keyStoreName, path - use getClassLoader()
				createTrustManagers(getFileInputStream("trustedCerts.jks"), "storepass"), //TODO name should come from config file - trustStoreName, path - use getClassLoader()
				new SecureRandom());
		} catch (NullPointerException e) {
			throw new SSLContextInitializationException("No key store file found with name: ", e);
		} catch (KeyManagementException e) {
			throw new SSLContextInitializationException("sslContext init failed: " + e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new SSLContextInitializationException("SSLContext getInstance failed", e);
		}
		return sslContext;
	}
	
	/**
     * Creates the key managers required to initiate the {@link SSLContext}, using a JKS keystore as an input.
     *
     * @param filepath - the path to the JKS keystore.
     * @param keystorePassword - the keystore's password.
     * @param keyPassword - the key's passsword.
     * @return {@link KeyManager} array that will be used to initiate the {@link SSLContext}.
	 * @throws SSLContextInitializationException 
     * @throws Exception
     */
    protected static KeyManager[] createKeyManagers(InputStream keyStoreIS, String keystorePassword, String keyPassword) throws SSLContextInitializationException 
    {
        KeyManagerFactory kmf = null;
		try 
		{
			KeyStore keyStore = KeyStore.getInstance("JKS");
			try 
			{
			    keyStore.load(keyStoreIS, keystorePassword.toCharArray());
			} finally {
			    if (keyStoreIS != null) 
			        keyStoreIS.close();
			}
			kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(keyStore, keyPassword.toCharArray());
		} catch (UnrecoverableKeyException e) {
			throw new SSLContextInitializationException("key cannot be recovered (e.g. the given password is wrong)", e);
		} catch (KeyStoreException e) {
			throw new SSLContextInitializationException("KeyStore get instance failed", e);
		} catch (NoSuchAlgorithmException e) {
			throw new SSLContextInitializationException("Key manager, algorithm not found:", e);
		} catch (CertificateException e) {
			throw new SSLContextInitializationException("Key store: certificate load issue", e);
		} catch (IOException e) {
			throw new SSLContextInitializationException("Key store: IO issue", e);
		}
        return kmf.getKeyManagers();
    }

    /**
     * Creates the trust managers required to initiate the {@link SSLContext}, using a JKS keystore as an input.
     *
     * @param filepath - the path to the JKS keystore.
     * @param keystorePassword - the keystore's password.
     * @return {@link TrustManager} array, that will be used to initiate the {@link SSLContext}.
     * @throws Exception
     */
    protected static TrustManager[] createTrustManagers(InputStream trustStoreIS, String keystorePassword) throws SSLContextInitializationException 
    {
    	TrustManagerFactory trustFactory = null;
        try 
        {
			KeyStore trustStore = KeyStore.getInstance("JKS");
			try 
			{
			    trustStore.load(trustStoreIS, keystorePassword.toCharArray());
			} finally {
			    if (trustStoreIS != null) 
			        trustStoreIS.close();
			}
			trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustFactory.init(trustStore);
		} catch (KeyStoreException e) {
			throw new SSLContextInitializationException("Trust get instance failed", e);
		}  catch (NoSuchAlgorithmException e) {
			throw new SSLContextInitializationException("Trust manager, algorithm not found:", e);
		} catch (CertificateException e) {
			throw new SSLContextInitializationException("Trust store: certificate load issue", e);
		} catch (IOException e) {
			throw new SSLContextInitializationException("Trust store: IO issue", e);
		}
        return trustFactory.getTrustManagers();
    }
    
    
    private static InputStream getFileInputStream(String fileName) 
    {
    	return NcephSSLContext.class.getClassLoader().getResourceAsStream(fileName);
    }
}
