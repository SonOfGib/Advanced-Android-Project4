package edu.temple.mapchat;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class KeyService extends Service {
    private KeyPair keys;
    private String PEMFile;
    private HashMap<String,String> partnerKeys;
    private SharedPreferences prefs;
    private final IBinder mBinder = new LocalBinder();
    private final String PARTNER_MAP_PREF = "PARTNER_MAP_PREF";
    private final String PEM_FILE_PREF = "PEM_FILE_PREF";
    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public KeyService getService() {
            return KeyService.this;
        }
    }


    public KeyService() {
        partnerKeys = new HashMap<>();
    }

    //Generate PEM pair of private and public key.
    void generateMyKeys(){
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            KeyPair pair = kpg.generateKeyPair();
            //Construct PEM file
            StringBuilder builder = new StringBuilder();
            builder.append("-----BEGIN PRIVATE KEY-----\n");
            builder.append(Base64.encodeToString(pair.getPrivate().getEncoded(), Base64.DEFAULT));
            builder.append("-----END PRIVATE KEY-----\n");
            builder.append("-----BEGIN PUBLIC KEY-----\n");
            builder.append(Base64.encodeToString(pair.getPublic().getEncoded(), Base64.DEFAULT));
            builder.append("-----END PUBLIC KEY-----");
            String myPEMFile = builder.toString();
            Log.d("PEM", myPEMFile);
            keys = pair;
            PEMFile = myPEMFile;
            prefs.edit().putString(PEM_FILE_PREF, PEMFile).apply();

        }
        catch (NoSuchAlgorithmException e){
            Log.wtf("Encryption Problem", e);
        }
    }

    void storePublicKeyPEM(String partnerName, String PEM){
        String pubKeyPEM = PEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
        pubKeyPEM = pubKeyPEM.replace("-----END PUBLIC KEY-----", "");

        // Base64 decode the data

        //byte [] encoded = Base64.decode(pubKeyPEM, Base64.DEFAULT);
        storePublicKey(partnerName, pubKeyPEM);
    }

    void storePublicKey(String partnerName, String publicKey){
        partnerKeys.put(partnerName,publicKey);
        //Save the map to preferences.
        String mapString = new JSONObject(partnerKeys).toString();
        prefs.edit().putString(PARTNER_MAP_PREF, mapString).apply();
    }

    //This is for use for OUR key pair only!
    private KeyPair pemToKeys(String pem){
        String[] stringKeys = pem.split("\n-----END PRIVATE KEY-----\n-----BEGIN PUBLIC KEY-----\n");
        String pemPrivateKey = stringKeys[0].replace("-----BEGIN PRIVATE KEY-----\n","");
        String pemPublicKey = stringKeys[1].replace("-----END PUBLIC KEY-----","");


        try {
            byte[] encodedPrivateKey = Base64.decode(pemPrivateKey, Base64.DEFAULT);
            //Private keys are special apparently
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(privSpec);
            byte[] encodedPublicKey = Base64.decode(pemPublicKey, Base64.DEFAULT);
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(encodedPublicKey);
            RSAPublicKey rsaPublicKey = (RSAPublicKey) keyFactory.generatePublic(pubSpec);
            KeyPair keyPair = new KeyPair(rsaPublicKey,rsaPrivateKey);
            return keyPair;
        }catch (NoSuchAlgorithmException| InvalidKeySpecException e){
            Log.e("Encryption", "Conversion problem", e);
            return null;
        }

    }

    /**
     * Provides the public key of our user in PEM format.
     * @return A PEM formatted public key.
     */
    String getMyPublicKey(){
        if(keys != null){
            PublicKey key = keys.getPublic();
            byte[] keyBytes = key.getEncoded();

            String encodedKey = Base64.encodeToString(keyBytes,Base64.DEFAULT);
            String retVal = "-----BEGIN PUBLIC KEY-----\n"+encodedKey+"-----END PUBLIC KEY-----";
            Log.d("Public Key Export", retVal);
            return retVal;
        }
        return "";
    }

    RSAPublicKey getPublicKey(String partnerName){
        String pubkey = (String) partnerKeys.get(partnerName);
        if(pubkey == null){
            return null;
        }
        byte[] encoded = Base64.decode(pubkey, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(encoded);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey key = (RSAPublicKey)keyFactory.generatePublic(spec);
            return key;
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    /**
     * Encrypt with the partners public key.
     * @param plainText The actual text.
     * @param partnerName The partners name.
     * @return The cipher text.
     */
    public String encrypt(String plainText, String partnerName){
        RSAPublicKey publicKey = getPublicKey(partnerName);
        try{
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);
            byte[] encryptedText = cipher.doFinal(plainText.getBytes());
            return Base64.encodeToString(encryptedText, Base64.DEFAULT);
        }
        catch (NoSuchPaddingException|NoSuchAlgorithmException|
                BadPaddingException| IllegalBlockSizeException| InvalidKeyException e){
            Log.e("Crypto error", "Encrypt", e);
            return null;
        }
    }

    /**
     * Decrypt with our private key.
     * @param cipherText The encrypted text.
     * @return The decrypted text.
     */
    public String decrypt(String cipherText){
        byte[] encryptedText = Base64.decode(cipherText, Base64.DEFAULT);
        try{
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
            String decryptedText = new String(cipher.doFinal(encryptedText));
            return decryptedText;
        }
        catch (NoSuchPaddingException|NoSuchAlgorithmException|
                BadPaddingException| IllegalBlockSizeException| InvalidKeyException e){
            Log.e("Crypto error", "Decrypt", e);
            return null;
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String jsonString = prefs.getString(PARTNER_MAP_PREF, "");
        String pemString = prefs.getString(PEM_FILE_PREF, "");
        Gson gson = new Gson();
        if(!jsonString.equals("")) {
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            partnerKeys = gson.fromJson(jsonString, type);
        }
        Log.d("PEM EXISTS?",""+!pemString.equals(""));
        if(!pemString.equals("")){
            PEMFile = pemString;
            keys = pemToKeys(PEMFile);
        }
    }

    /**
     * Returns a set of the partners stored in the partner-key map.
     * @return Set of partner name Strings.
     */
    public ArrayList<String> getSavedPartners(){
        return new ArrayList<>(partnerKeys.keySet());
    }



    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}