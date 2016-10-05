package com.synconset;

/**
* A plugin for storing strings in the Android KeyStore
*/

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.json.JSONArray;
import org.json.JSONException;
import android.util.Log;
import android.content.Context;

import javax.crypto.SecretKey;

public class KeychainPlugin extends CordovaPlugin {
    private KeyStore ks;
    private KeyStore.PasswordProtection prot;
    private char[] Password = "".toCharArray();
    private String FileName = "";
    private String TAG = "";
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (action.equals("getForKey")) {
            rwl.readLock().lock();
            try {
                GetForKey getForKey = new GetForKey(data, callbackContext);
                cordova.getThreadPool().execute(getForKey);
            } finally {
                rwl.readLock().unlock();
            }
            return true;
        } else if (action.equals("setForKey")) {
            rwl.writeLock().lock();
            try {
                SetForKey setForKey = new SetForKey(data, callbackContext);
                cordova.getThreadPool().execute(setForKey);
            } finally {
                rwl.writeLock().unlock();
            }
            return true;
        } else if (action.equals("removeForKey")) {
            rwl.writeLock().lock();
            try {
                RemoveForKey removeForKey = new RemoveForKey (data, callbackContext);
                cordova.getThreadPool().execute(removeForKey);
            } finally {
                rwl.writeLock().unlock();
            }
            return true;
        } else {
            Log.d("PLUGIN", "unknown action");
            return false;
        }
    }

    private void loadKeyStore(Context context) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        if (ks == null) {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            prot = new KeyStore.PasswordProtection(Password);
        }
        try {
            InputStream s = context.openFileInput(FileName);
            ks.load(s, Password);
        } catch (FileNotFoundException e) {
            ks.load(null, Password);
        }
    }

    private class KeyChainBase {
        protected String key;
        protected String servicename;
        protected String value;
        protected CallbackContext callbackContext;

        public KeyChainBase(JSONArray params, CallbackContext callbackContext) throws JSONException {
            this.key = params.getString(0);
            this.servicename = params.getString(1);
            if (params.length() > 2) {
                this.value = params.getString(2);
            }
            this.callbackContext = callbackContext;
        }
    }

    private class GetForKey extends KeyChainBase implements Runnable {
        public GetForKey(JSONArray params, CallbackContext callbackContext) throws JSONException {
            super (params, callbackContext);
        }

        @Override
        public void run() {
            try {
                loadKeyStore(cordova.getActivity().getApplicationContext());

                KeyStore.SecretKeyEntry e = (KeyStore.SecretKeyEntry) ks.getEntry(key, prot);
                byte[] bytes = e.getSecretKey().getEncoded();
                String serialized = new String(bytes);

                callbackContext.success(serialized);
            } catch (UnrecoverableEntryException e) {
                Log.w(TAG, "UnrecoverableEntryException ");
                callbackContext.error(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "NoSuchAlgorithmException", e);
                callbackContext.error(e.getMessage());
            } catch (KeyStoreException e) {
                Log.w(TAG, "KeyStoreException", e);
                callbackContext.error(e.getMessage());
            } catch (CertificateException e) {
                Log.w(TAG, "CertificateException", e);
                callbackContext.error(e.getMessage());
            } catch (IOException e) {
                Log.w(TAG, "IOException", e);
                callbackContext.error(e.getMessage());
            }
        }
    }

    private class SetForKey extends KeyChainBase implements Runnable {
        public SetForKey(JSONArray params, CallbackContext callbackContext) throws JSONException {
            super (params, callbackContext);
        }

        @Override
        public void run() {
            try {
                loadKeyStore(cordova.getActivity().getApplicationContext());

                RawKey rawKey = new RawKey(value);
                KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(rawKey);
                ks.setEntry(key, entry, prot);

                saveStore();

                callbackContext.success();
            } catch (KeyStoreException e) {
                Log.w(TAG, "KeyStoreException", e);
                callbackContext.error(e.getMessage());
            } catch (IOException e) {
                Log.w(TAG, "IOException", e);
                callbackContext.error(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "NoSuchAlgorithmException", e);
                callbackContext.error(e.getMessage());
            } catch (CertificateException e) {
                Log.w(TAG, "CertificateException", e);
                callbackContext.error(e.getMessage());
            }
        }
    }

    private class RemoveForKey extends KeyChainBase implements Runnable {
        public RemoveForKey(JSONArray params, CallbackContext callbackContext) throws JSONException {
            super (params, callbackContext);
        }

        @Override
        public void run() {
            try {
                loadKeyStore(cordova.getActivity().getApplicationContext());

                ks.deleteEntry(key);
                saveStore();

                callbackContext.success();
            } catch (KeyStoreException e) {
                Log.w(TAG, "KeyStoreException", e);
                callbackContext.error(e.getMessage());
            } catch (IOException e) {
                Log.w(TAG, "IOException", e);
                callbackContext.error(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "NoSuchAlgorithmException", e);
                callbackContext.error(e.getMessage());
            } catch (CertificateException e) {
                Log.w(TAG, "CertificateException", e);
                callbackContext.error(e.getMessage());
            }
        }
    }

    private void saveStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        FileOutputStream o = cordova.getActivity().getApplicationContext().openFileOutput(FileName, Context.MODE_PRIVATE);
        ks.store(o,Password);
    }

    private class RawKey implements SecretKey {
        private byte[] bytes;

        public RawKey(String s) {
            bytes = s.getBytes();
        }

        public byte[] getEncoded() {
            return bytes;
        }

        public String getFormat() {
            return "RAW";
        }

        public String getAlgorithm() {
            return "RAW";
        }
    }
}
