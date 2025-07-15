package pl.catalogic.demo.s3.v2;

import java.util.function.Function;
import java.util.stream.Stream;
import org.springframework.data.util.CloseableIterator;
import com.mongodb.client.MongoCursor;

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
  
  public static <T, R> CloseableIterator<R> toCloseableIterator(MongoCursor<T> cursor, Function<T, R> converter) {
    return new CloseableIterator<R>() {
      @Override
      public void close() {
        cursor.close();
      }

      @Override
      public boolean hasNext() {
        return cursor.hasNext();
      }

      @Override
      public R next() {
        return converter.apply(cursor.next());
      }
    };
  }
}
