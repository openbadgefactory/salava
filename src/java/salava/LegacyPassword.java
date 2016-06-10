package salava;

import java.security.NoSuchAlgorithmException;

public class LegacyPassword {

    private static final int DRUPAL_HASH_LENGTH = 55;

    private static String _password_itoa64() {
        return "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    }

    private static int password_get_count_log2(String setting) {
        return _password_itoa64().indexOf(setting.charAt(3));
    }


    private static byte[] sha512(String input) {
        try {
            return java.security.MessageDigest.getInstance("SHA-512").digest(input.getBytes());
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return new byte[0];
    }

    private static byte[] sha512(byte[] input) {
        try {
            return java.security.MessageDigest.getInstance("SHA-512").digest(input);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return new byte[0];
    }

    /**
     * @param candidate               the clear text password
     * @param saltedEncryptedPassword the salted encrypted password string to check => NEEDS TO BE THE DEFAULT DRUPAL 7 PASSWORD HASH.
     * @return true if the candidate matches, false otherwise.
     */
    public static boolean checkPassword(String candidate, String saltedEncryptedPassword) throws Exception {
        if (candidate == null || saltedEncryptedPassword == null) {
            return false;
        }
        String hash = password_crypt(candidate, saltedEncryptedPassword);
        return saltedEncryptedPassword.equalsIgnoreCase(hash);
    }


    private static String password_crypt(String password, String passwordHash) throws Exception {
        // The first 12 characters of an existing hash are its setting string.
        passwordHash = passwordHash.substring(0, 12);
        int count_log2 = password_get_count_log2(passwordHash);
        String salt = passwordHash.substring(4, 12);
        // Hashes must have an 8 character salt.
        if (salt.length() != 8) {
            return null;
        }

        int count = 1 << count_log2;

        byte[] hash;
        try {
            hash = sha512(salt.concat(password));

            do {
                hash = sha512(joinBytes(hash, password.getBytes("UTF-8")));
            } while (--count > 0);
        } catch (Exception e) {
            return null;
        }

        String output = passwordHash + _password_base64_encode(hash, hash.length);
        return (output.length() > 0) ? output.substring(0, DRUPAL_HASH_LENGTH) : null;
    }

    private static byte[] joinBytes(byte[] a, byte[] b) {
        byte[] combined = new byte[a.length + b.length];

        System.arraycopy(a, 0, combined, 0, a.length);
        System.arraycopy(b, 0, combined, a.length, b.length);
        return combined;
    }



    private static String _password_base64_encode(byte[] input, int count) throws Exception {

        StringBuffer output = new StringBuffer();
        int i = 0;
        CharSequence itoa64 = _password_itoa64();
        do {
            long value = SignedByteToUnsignedLong(input[i++]);

            output.append(itoa64.charAt((int) value & 0x3f));
            if (i < count) {
                value |= SignedByteToUnsignedLong(input[i]) << 8;
            }
            output.append(itoa64.charAt((int) (value >> 6) & 0x3f));
            if (i++ >= count) {
                break;
            }
            if (i < count) {
                value |=  SignedByteToUnsignedLong(input[i]) << 16;
            }

            output.append(itoa64.charAt((int) (value >> 12) & 0x3f));
            if (i++ >= count) {
                break;
            }
            output.append(itoa64.charAt((int) (value >> 18) & 0x3f));
        } while (i < count);

        return output.toString();
    }


    public static long SignedByteToUnsignedLong(byte b) {
        return b & 0xFF;
    }

}