
package polimi.util;

import java.util.Collection;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A bounded {@linkplain Queue queue} backed by an array. This queue orders elements FIFO
 * (first-in-first-out). The <em>head</em> of the queue is that element that has been on the queue
 * the longest time. The <em>tail</em> of the queue is that element that has been on the queue the
 * shortest time. New elements are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 * 
 * <p>
 * Once created, the capacity cannot be increased. Attempts to <tt>add</tt> an element into a full
 * queue will result in an exception; attempts to <tt>remove</tt> an element from an empty queue
 * will similarly return an exception.
 * 
 * <p>
 * This class and its iterator implement all of the <em>optional</em> methods of the
 * {@link Collection} and {@link Iterator} interfaces.
 * 
 * <p>
 * The implementation of this class has been adapted from the <tt>ArrayBlockingQueue</tt>, part
 * of the Java Collections Framework</a>.
 * 
 * @param <E> the type of elements held in this collection
 */
public class BoundedQueue<E> extends AbstractQueue<E> implements java.io.Serializable {
  /**
   * Serialization ID. This class relies on default serialization even for the items array, which is
   * default-serialized, even if it is empty.
   */
  private static final long serialVersionUID = 6233639726177610918L;
  /** The queued items */
  private final E[] items;
  /** items index for next take, poll or remove */
  private int takeIndex;
  /** items index for next put, offer, or add. */
  private int putIndex;
  /** Number of items in the queue */
  private int count;

  /**
   * Creates a <tt>BoundedQueue</tt> with the given (fixed) capacity.
   * 
   * @param capacity the capacity of this queue
   * @throws IllegalArgumentException if <tt>capacity</tt> is less than 1
   */
  public BoundedQueue(int capacity) {
    if(capacity<=0) throw new IllegalArgumentException();
    this.items = (E[]) new Object[capacity];
    takeIndex = putIndex = count = 0;
  }

  /**
   * Creates an <tt>BoundedQueue</tt> with the given (fixed) capacity and initially containing the
   * elements of the given collection, added in traversal order of the collection's iterator.
   * 
   * @param capacity the capacity of this queue
   * @param c the collection of elements to initially contain
   * @throws IllegalArgumentException if <tt>capacity</tt> is less than <tt>c.size()</tt>, or
   *             less than 1.
   * @throws NullPointerException if the specified collection or any of its elements are null
   */
  public BoundedQueue(int capacity, Collection<? extends E> c) {
    this(capacity);
    if(capacity<c.size()) throw new IllegalArgumentException();
    for(Iterator<? extends E> it = c.iterator(); it.hasNext();)
      add(it.next());
  }

  /**
   * Returns an iterator over the elements in this queue in proper sequence. The returned
   * <tt>Iterator</tt> is a "weakly consistent" iterator that will never throw
   * {@link ConcurrentModificationException}, and guarantees to traverse elements as they existed
   * upon construction of the iterator, and may (but is not guaranteed to) reflect any modifications
   * subsequent to construction.
   * 
   * @return an iterator over the elements in this queue in proper sequence
   */
  @Override
  public Iterator<E> iterator() {
    return new Itr();
  }

  // this doc comment is overridden to remove the reference to collections
  // greater in size than Integer.MAX_VALUE
  /**
   * Returns the number of elements in this queue.
   * 
   * @return the number of elements in this queue
   */
  @Override
  public int size() {
    return count;
  }

  /**
   * If the queue is not full this method inserts the specified element at the tail of this queue
   * and returns <tt>true</tt>, otherwise the queue is left untouched and the method returns
   * <tt>false</tt>. This method is generally preferable to method {@link #add}, which can fail
   * to insert an element only by throwing an exception.
   * 
   * @throws NullPointerException if the specified element is null
   */
  @Override
  public boolean offer(E e) {
    if(e==null) throw new NullPointerException();
    if(count==items.length) return false;
    else {
      items[putIndex] = e;
      putIndex = inc(putIndex);
      ++count;
      return true;
    }
  }

  @Override
  public E peek() {
    return (count==0) ? null : items[takeIndex];
  }

  @Override
  public E poll() {
    if(count==0) return null;
    E x = items[takeIndex];
    items[takeIndex] = null;
    takeIndex = inc(takeIndex);
    --count;
    return x;
  }

  @Override
  public boolean remove(Object o) {
    if(o==null) return false;
    int i = takeIndex;
    int k = 0;
    for(;;) {
      if(k++>=count) return false;
      if(o.equals(items[i])) {
        removeAt(i);
        return true;
      }
      i = inc(i);
    }
  }

  @Override
  public boolean contains(Object o) {
    if(o==null) return false;
    int i = takeIndex;
    int k = 0;
    while(k++<count) {
      if(o.equals(items[i])) return true;
      i = inc(i);
    }
    return false;
  }

  @Override
  public Object[] toArray() {
    Object[] a = new Object[count];
    int k = 0;
    int i = takeIndex;
    while(k<count) {
      a[k++] = items[i];
      i = inc(i);
    }
    return a;
  }

  @Override
  public <T> T[] toArray(T[] a) {
    if(a.length<count)
      a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), count);
    int k = 0;
    int i = takeIndex;
    while(k<count) {
      a[k++] = (T) items[i];
      i = inc(i);
    }
    if(a.length>count) a[count] = null;
    return a;
  }

  @Override
  public void clear() {
    int i = takeIndex;
    int k = count;
    while(k-->0) {
      items[i] = null;
      i = inc(i);
    }
    count = 0;
    putIndex = 0;
    takeIndex = 0;
  }

  /**
   * Utility for remove and iterator.remove: Delete item at position i.
   */
  void removeAt(int i) {
    // if removing front item, just advance
    if(i==takeIndex) {
      items[takeIndex] = null;
      takeIndex = inc(takeIndex);
    } else {
      // slide over all others up through putIndex.
      for(;;) {
        int nexti = inc(i);
        if(nexti!=putIndex) {
          items[i] = items[nexti];
          i = nexti;
        } else {
          items[i] = null;
          putIndex = i;
          break;
        }
      }
    }
    --count;
  }

  /**
   * Circularly increment i (faster than using modulo).
   */
  private final int inc(int i) {
    return (++i==items.length) ? 0 : i;
  }

  /**
   * Iterator for BoundedQueue
   */
  private class Itr implements Iterator<E> {
    /**
     * Index of element to be returned by next, or a negative number if no such.
     */
    private int nextIndex;
    /**
     * nextItem holds on to item fields because once we claim that an element exists in hasNext(),
     * we must return it in the following next() call even if it was in the process of being removed
     * when hasNext() was called.
     */
    private E nextItem;
    /**
     * Index of element returned by most recent call to next. Reset to -1 if this element is deleted
     * by a call to remove.
     */
    private int lastRet;

    Itr() {
      lastRet = -1;
      if(count==0) nextIndex = -1;
      else {
        nextIndex = takeIndex;
        nextItem = items[takeIndex];
      }
    }

    public boolean hasNext() {
      return nextIndex>=0;
    }

    /**
     * Checks whether nextIndex is valid; if so setting nextItem. Stops iterator when either hits
     * putIndex or sees null item.
     */
    private void checkNext() {
      if(nextIndex==putIndex) {
        nextIndex = -1;
        nextItem = null;
      } else {
        nextItem = items[nextIndex];
        if(nextItem==null) nextIndex = -1;
      }
    }

    public E next() {
      if(nextIndex<0) throw new NoSuchElementException();
      lastRet = nextIndex;
      E x = nextItem;
      nextIndex = inc(nextIndex);
      checkNext();
      return x;
    }

    public void remove() {
      int i = lastRet;
      if(i==-1) throw new IllegalStateException();
      lastRet = -1;
      int ti = takeIndex;
      removeAt(i);
      // back up cursor (reset to front if was first element)
      nextIndex = (i==ti) ? takeIndex : i;
      checkNext();
    }
  }
}
