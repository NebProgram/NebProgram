package ru.kos.neb.neb_builder;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509TrustManager;

public class DummyTrustmanager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        // do nothing
    }

    @Override
    public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
        // do nothing
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[0];
    }
}
