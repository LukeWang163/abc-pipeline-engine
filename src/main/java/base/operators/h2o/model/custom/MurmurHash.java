package base.operators.h2o.model.custom;

public class MurmurHash
{
    private static MurmurHash _instance = new MurmurHash();


    public static MurmurHash getInstance() { return _instance; }


    public int hash(byte[] data, int length, int seed) {
        int m = 1540483477;
        int r = 24;
        int h;
        h = seed ^ length;

        int len_4 = length >> 2;

        for (int i = 0; i < len_4; i++) {
            int i_4 = i << 2;
            int k = data[i_4 + 3];
            k <<= 8;
            k |= data[i_4 + 2] & 0xFF;
            k <<= 8;
            k |= data[i_4 + 1] & 0xFF;
            k <<= 8;
            k |= data[i_4 + 0] & 0xFF;
            k *= m;
            k ^= k >>> r;
            k *= m;
            h *= m;
            h ^= k;
        }


        int len_m = len_4 << 2;
        int left = length - len_m;

        if (left != 0) {
            if (left >= 3) {
                h ^= data[length - 3] << 16;
            }
            if (left >= 2) {
                h ^= data[length - 2] << 8;
            }
            if (left >= 1) {
                h ^= data[length - 1];
            }

            h *= m;
        }

        h ^= h >>> 13;
        h *= m;
        return h >>> 15;
    }
}
