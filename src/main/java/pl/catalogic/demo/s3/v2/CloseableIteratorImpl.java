package pl.catalogic.demo.s3.v2;

import java.util.stream.Stream;
import org.springframework.data.util.CloseableIterator;

public class CloseableIteratorImpl {

  private CloseableIteratorImpl() {}

  public static <T> CloseableIterator<T> toCloseableIterator(Stream<T> stream) {
    var iterator = stream.iterator();
    return new CloseableIterator<T>() {
      @Override
      public void close() {
        stream.close();
      }

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public T next() {
        return iterator.next();
      }
    };
  }
}
