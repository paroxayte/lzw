package encoders.coder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import encoders.util.Streams;


public class GolombRice implements Coder<Integer, Boolean>{

  private int k;
  private int m;

  /**
   * @param k The rice parameter
   */
  public GolombRice(int k) {
    setTunable(k);
  }

  public void setTunable(int k) {
    this.k = k;

    int acc = 1;
    for(int i = 0; i < k; i++) {
      acc *= 2;
    }
    m = acc;
  }

  /**
   * Encode a single digit using this GolombRice coder.
   * @param x Digit to be encoded
   * @return  Stream<Boolean> of bits
   */
  public Stream<Boolean> encodeDigit(Integer x) {
    Stream.Builder<Boolean> ret = Stream.builder();

    for(int q = x / m; q > 0; q--) {
      ret.add(Boolean.TRUE);
    }
    ret.add(Boolean.FALSE);

    Deque<Boolean> acc = new ArrayDeque<>();
    for(int i = 0; i < k; i++) {
      if(x % 2 == 0) {
        acc.push(Boolean.FALSE);
      } else {
        acc.push(Boolean.TRUE);
      }

      x = x >>> 1;
    }

    while(!acc.isEmpty()) {
      ret.add(acc.pop());
    }

    return ret.build();
  }

  /**
   * Encode a Stream<Integer> using this GolombRice coder.
   * @param in  Stream<Integer>
   * @return    Stream<Boolean>
   */
  @Override
  public Stream<Boolean> encode(Stream<Integer> in) {
    return in.flatMap(this::encodeDigit);
  }

  /**
   * Decode a stream of bits using this GolombRice coder.
   * @param in  Stream<Boolean>
   * @return    Stream<Integer>
   */
  @Override
  public Stream<Integer> decode(Stream<Boolean> in) {

    BiFunction<Boolean,Boolean,Deque<Integer>> gen = new BiFunction<Boolean,Boolean,Deque<Integer>>() {
      private int mRef = m;
      private int kRef = k;
      private int q = 0;
      private int r = 0;
      private Deque<Integer> rCache = new ArrayDeque<>();
      private boolean unary = true;

      @Override
      public Deque<Integer> apply(Boolean bit, Boolean end) {
        if(unary) {
          if(bit) {
            q++;
          } else {
            unary = false;
          }
          return rCache;
        }

        mRef = mRef >>> 1;
        if(kRef-- >= 0) {
          if(bit) {
            r += mRef;
          }
        }
        if(kRef == 0) {
          rCache.push(q * m + r);
          q = 0;
          r = 0;
          mRef = m;
          kRef = k;
          unary = true;
        }

        return rCache;
      }

    };
    return Streams.fold(gen,in);
  }

}
