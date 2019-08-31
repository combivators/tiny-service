package net.tiny.ws;

import java.io.Serializable;
import java.util.UUID;

public final class Unique implements Serializable {

    private static final long serialVersionUID = 1L;
    private long upValue;
    private long lowValue;

    public Unique() {
        this(UUID.randomUUID());
    }

    protected Unique(UUID uuid) {
        this(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    private Unique(long up, long low) {
        this.upValue = up;
        this.lowValue = low;
    }

    private boolean equalsKey(Unique key) {
        return (key.upValue == this.upValue && key.lowValue == this.lowValue);
    }

    private static String toString(long i) {
        char[] buf = new char[32];
        int z = 64; // 1 << 6;
        int cp = 32;
        long b = z - 1;
        do {
            buf[--cp] = DIGITS66[(int)(i & b)];
            i >>>= 6;
        } while (i != 0);
        return new String(buf, cp, (32-cp));
    }

   // array de 64+2 digitos
   private final static char[] DIGITS66 = {
      '0','1','2','3','4','5','6','7','8','9',
      'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
      'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
      '-','.','_','~'
    };

    private static String getKeyId(long up, long low) {
        return toString(up) + toString(low);
    }

    @Override
    public boolean equals(Object key) {
        if (key instanceof Unique) {
            return equalsKey((Unique) key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int)(upValue - lowValue);
    }

    @Override
    public String toString() {
        return getKeyId(upValue, lowValue);
    }

    public static String uniqueKey() {
        UUID u = UUID.randomUUID();
        return getKeyId(u.getMostSignificantBits(), u.getLeastSignificantBits());
    }

//    public static Unique valueOf(String value) {
//        return new Unique(value);
//    }

}
