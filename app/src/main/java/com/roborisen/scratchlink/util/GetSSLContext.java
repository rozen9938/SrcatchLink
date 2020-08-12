package com.roborisen.scratchlink.util;

import android.content.Context;
import android.util.Log;

import com.roborisen.scratchlink.R;

import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class GetSSLContext {
    final String TAG = getClass().getSimpleName();
    Context mContext;
    public GetSSLContext(Context context){
        mContext = context;
    }

    public SSLContext doGetSSLContext() {
        SSLContext sslContext = null;
        InputStream caInput = mContext.getResources().openRawResource(R.raw.cert);
        try
        {
            // 클라이언트 인증서를 로드한다.
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(caInput, "Scratch".toCharArray());

            // 클라이언트 인증서를 이용해서 KeyManager를 만든다.
            String kmfAlgorithm = KeyManagerFactory.getDefaultAlgorithm();
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(kmfAlgorithm);
            kmf.init(keyStore, "pass".toCharArray());
            // 클라이언트 인증서를 이용해서 TrustManager를 만든다.
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // TrustManager 와 KeyManager를 이용해서 SSLContext 를 생성한다.
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        }
        catch(Exception exp)
        {
            Log.e(TAG,"Error:"+exp.toString());
            sslContext = null;
        }

        return sslContext;
    }
}
