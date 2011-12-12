/* Ivan Trendafilov 0837795 */
/**
 * ByteUtils is a helper class for byte manipulations
 */
public class ByteUtils {

    public static byte[] convertToBytes(short value) {
        byte[] byteArray = new byte[2];

        byteArray[0] = (byte) (value >> 8);
        byteArray[1] = (byte) value;
        return byteArray;
    }

    public static short convertShortFromBytes(byte[] byteArray) {
        return convertShortFromBytes(byteArray, 0);
    }

    public static short convertShortFromBytes(byte[] byteArray, int offset) {
        // Convert it to a short
        short number = (short) ((byteArray[offset+1] & 0xFF) + ((byteArray[offset+0] & 0xFF) << 8));
        return number;
    }

    public static byte[] convertToBytes(double n) {
        long bits = Double.doubleToLongBits(n);
        return convertToBytes(bits);
    }
    
    public static byte[] convertToBytes(long n) {
        byte[] bytes = new byte[8];

        bytes[7] = (byte) (n);
        n >>>= 8;
        bytes[6] = (byte) (n);
        n >>>= 8;
        bytes[5] = (byte) (n);
        n >>>= 8;
        bytes[4] = (byte) (n);
        n >>>= 8;
        bytes[3] = (byte) (n);
        n >>>= 8;
        bytes[2] = (byte) (n);
        n >>>= 8;
        bytes[1] = (byte) (n);
        n >>>= 8;
        bytes[0] = (byte) (n);

        return bytes;
    }

    public static byte[] subbytes(byte[] source, int srcBegin) {
      return subbytes(source, srcBegin, source.length);
    }

    public static byte[] subbytes(byte[] source, int srcBegin, int srcEnd) {
      byte destination[];

      destination = new byte[srcEnd - srcBegin];
      getBytes(source, srcBegin, srcEnd, destination, 0);

      return destination;
    }
    
    public static void getBytes(byte[] source, int srcBegin, int srcEnd, byte[] destination,
        int dstBegin) {
      System.arraycopy(source, srcBegin, destination, dstBegin, srcEnd - srcBegin);
    }
  }




