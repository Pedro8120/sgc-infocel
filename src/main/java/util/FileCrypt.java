package util;

import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;

public class FileCrypt {
    
    private static String chaveencriptacao = "0123456789abcdef";
    private static int iteration = 5;
    static final int TAMANHO_BUFFER = 32768; //32kb  
    byte[] buf = new byte[TAMANHO_BUFFER];
    static String IV = "AAAAAAAAAAAAAAAA";

    public Boolean criptografar(InputStream in, OutputStream out) throws Exception {
        try {
            Cipher cifra = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
            SecretKeySpec key = new SecretKeySpec(chaveencriptacao.getBytes("UTF-8"), "AES");
            cifra.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV.getBytes("UTF-8")));

            out = new CipherOutputStream(out, cifra);

            int numRead = 0;

            while ((numRead = in.read(buf, 0, TAMANHO_BUFFER)) >= 0) {
                out.write(buf, 0, numRead);
            }

            out.close();
            in.close();
            return true;

        } catch (Exception ex) {
            Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
            return false;
        }
    }

    public Boolean descriptografar(InputStream in, OutputStream out) throws Exception {
        try {
            Cipher cifra = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
            SecretKeySpec key = new SecretKeySpec(chaveencriptacao.getBytes("UTF-8"), "AES");
            cifra.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(IV.getBytes("UTF-8")));

            out = new CipherOutputStream(out, cifra);

            int numRead = 0;

            while ((numRead = in.read(buf, 0, TAMANHO_BUFFER)) >= 0) {
                out.write(buf, 0, numRead);
            }

            out.close();
            in.close();
            return true;

        } catch (Exception ex) {
            Logger.getLogger(getClass()).error(ex);
            ex.printStackTrace();
            return false;
        }
    }
}
