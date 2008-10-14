package org.maven.ide.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

/**
 * This class creates an org.eclipse.ltk.core.refactoring.DocumentChange instance
 * based on old and new text values
 *
 * diff algorithm is based on http://www.incava.org/projects/java/java-diff/index.html 
 *
 * @author Anton Kraev
 *
 */

public class ChangeCreator {

  class StringWithPos {
    public String str;
    public int offset;
    
    public StringWithPos(String str, int offset) {
      this.str = str;
      this.offset = offset;
    }
  }
  
  class StringWithPosComparator implements Comparator<StringWithPos> {
    public int compare(StringWithPos o1, StringWithPos o2) {
      return o1.str.equals(o2.str)? 0: 1;
    }
  }

  private String label;
  private String oldText;
  private String newText;
  private IDocument document;

  private List<StringWithPos> parse(String txt) {
    List<StringWithPos> res = new ArrayList<StringWithPos>();
    StringTokenizer st = new StringTokenizer(txt, "\n\r", true);
    int offset = 0;
    while (st.hasMoreElements()) {
      String str = st.nextToken();
      if (str.equals("\n") || str.equals("\r")) {
        if (res.size() > 0)
          res.get(res.size() - 1).str = res.get(res.size() - 1).str + str;
        continue;
      }
      int pos = txt.indexOf(str, offset);
      res.add(new StringWithPos(str, pos));
      offset = pos;
    }
    return res;
  }
  
  public ChangeCreator(IDocument document, String label, String oldText, String newText) {
    this.document = document;
    this.label = label;
    this.oldText = oldText;
    this.newText = newText;
  }

  public DocumentChange createChange() {
    List<StringWithPos> before = parse(oldText);
    List<StringWithPos> after = parse(newText);
    DocumentChange change = new DocumentChange(label, document);
    change.setEdit(new MultiTextEdit());
    Diff diff = new Diff(before.toArray(), after.toArray(), new StringWithPosComparator());
    List<Difference> diffs = diff.diff();
    for (int i=0; i<diffs.size(); i++) {
      Difference d = diffs.get(i);
      if (d.delEnd == -1) {
        //addition
        String adding = "";
        for (int j=d.addStart; j<=d.addEnd; j++) {
          adding = adding + after.get(j).str;
        }
        change.addEdit(new InsertEdit(before.get(d.addStart).offset, adding));
      } else if (d.addEnd == -1) {
        //deletion
        for (int j=d.delStart; j<=d.delEnd; j++) {
          change.addEdit(new DeleteEdit(before.get(j).offset, before.get(j).str.length()));
        }
      } else {
        //replacement
        for (int j=d.addStart; j<=d.addEnd; j++) {
          change.addEdit(new ReplaceEdit(before.get(j).offset, before.get(j).str.length(), after.get(d.delStart + j - d.addStart).str));
        }
      }
    }
    return change;
  }
  
  /**
   * Represents a difference, as used in <code>Diff</code>. A difference consists
   * of two pairs of starting and ending points, each pair representing either the
   * "from" or the "to" collection passed to <code>Diff</code>. If an ending point
   * is -1, then the difference was either a deletion or an addition. For example,
   * if <code>getDeletedEnd()</code> returns -1, then the difference represents an
   * addition.
   */
  public class Difference
  {
      public static final int NONE = -1;
      
      /**
       * The point at which the deletion starts.
       */
      int delStart = NONE;

      /**
       * The point at which the deletion ends.
       */
      int delEnd = NONE;

      /**
       * The point at which the addition starts.
       */
      int addStart = NONE;

      /**
       * The point at which the addition ends.
       */
      int addEnd = NONE;

      /**
       * Creates the difference for the given start and end points for the
       * deletion and addition.
       */
      public Difference(int delStart, int delEnd, int addStart, int addEnd)
      {
          this.delStart = delStart;
          this.delEnd   = delEnd;
          this.addStart = addStart;
          this.addEnd   = addEnd;
      }

      /**
       * The point at which the deletion starts, if any. A value equal to
       * <code>NONE</code> means this is an addition.
       */
      public int getDeletedStart()
      {
          return delStart;
      }

      /**
       * The point at which the deletion ends, if any. A value equal to
       * <code>NONE</code> means this is an addition.
       */
      public int getDeletedEnd()
      {
          return delEnd;
      }

      /**
       * The point at which the addition starts, if any. A value equal to
       * <code>NONE</code> means this must be an addition.
       */
      public int getAddedStart()
      {
          return addStart;
      }

      /**
       * The point at which the addition ends, if any. A value equal to
       * <code>NONE</code> means this must be an addition.
       */
      public int getAddedEnd()
      {
          return addEnd;
      }

      /**
       * Sets the point as deleted. The start and end points will be modified to
       * include the given line.
       */
      public void setDeleted(int line)
      {
          delStart = Math.min(line, delStart);
          delEnd   = Math.max(line, delEnd);
      }

      /**
       * Sets the point as added. The start and end points will be modified to
       * include the given line.
       */
      public void setAdded(int line)
      {
          addStart = Math.min(line, addStart);
          addEnd   = Math.max(line, addEnd);
      }

      /**
       * Compares this object to the other for equality. Both objects must be of
       * type Difference, with the same starting and ending points.
       */
      public boolean equals(Object obj)
      {
          if (obj instanceof Difference) {
              Difference other = (Difference)obj;

              return (delStart == other.delStart && 
                      delEnd   == other.delEnd && 
                      addStart == other.addStart && 
                      addEnd   == other.addEnd);
          }
          else {
              return false;
          }
      }

      /**
       * Returns a string representation of this difference.
       */
      public String toString()
      {
          StringBuffer buf = new StringBuffer();
          buf.append("del: [" + delStart + ", " + delEnd + "]");
          buf.append(" ");
          buf.append("add: [" + addStart + ", " + addEnd + "]");
          return buf.toString();
      }

  }

  /**
   * Compares two collections, returning a list of the additions, changes, and
   * deletions between them. A <code>Comparator</code> may be passed as an
   * argument to the constructor, and will thus be used. If not provided, the
   * initial value in the <code>a</code> ("from") collection will be looked at to
   * see if it supports the <code>Comparable</code> interface. If so, its
   * <code>equals</code> and <code>compareTo</code> methods will be invoked on the
   * instances in the "from" and "to" collections; otherwise, for speed, hash
   * codes from the objects will be used instead for comparison.
   *
   * <p>The file FileDiff.java shows an example usage of this class, in an
   * application similar to the Unix "diff" program.</p>
   */
  public class Diff
  {
      /**
       * The source array, AKA the "from" values.
       */
      protected Object[] a;

      /**
       * The target array, AKA the "to" values.
       */
      protected Object[] b;

      /**
       * The list of differences, as <code>Difference</code> instances.
       */
      protected List<Difference> diffs = new ArrayList<Difference>();

      /**
       * The pending, uncommitted difference.
       */
      private Difference pending;

      /**
       * The comparator used, if any.
       */
      @SuppressWarnings("unchecked")
      private Comparator comparator;

      /**
       * The thresholds.
       */
      private TreeMap<Integer, Integer> thresh;

      /**
       * Constructs the Diff object for the two arrays, using the given comparator.
       */
      @SuppressWarnings("unchecked")
      public Diff(Object[] a, Object[] b, Comparator comp)
      {
          this.a = a;
          this.b = b;
          this.comparator = comp;
          this.thresh = null;     // created in getLongestCommonSubsequences
      }

      /**
       * Constructs the Diff object for the two arrays, using the default
       * comparison mechanism between the objects, such as <code>equals</code> and
       * <code>compareTo</code>.
       */
      public Diff(Object[] a, Object[] b)
      {
          this(a, b, null);
      }

      /**
       * Constructs the Diff object for the two collections, using the given
       * comparator.
       */
      public Diff(Collection<Object> a, Collection<Object> b, Comparator<Object> comp)
      {
          this(a.toArray(), b.toArray(), comp);
      }

      /**
       * Constructs the Diff object for the two collections, using the default
       * comparison mechanism between the objects, such as <code>equals</code> and
       * <code>compareTo</code>.
       */
      public Diff(Collection<Object> a, Collection<Object> b)
      {
          this(a, b, null);
      }

      /**
       * Runs diff and returns the results.
       */
      public List<Difference> diff()
      {
          traverseSequences();

          // add the last difference, if pending:
          if (pending != null) {
              diffs.add(pending);
          }

          return diffs;
      }

      /**
       * Traverses the sequences, seeking the longest common subsequences,
       * invoking the methods <code>finishedA</code>, <code>finishedB</code>,
       * <code>onANotB</code>, and <code>onBNotA</code>.
       */
      protected void traverseSequences()
      {
          Integer[] matches = getLongestCommonSubsequences();

          int lastA = a.length - 1;
          int lastB = b.length - 1;
          int bi = 0;
          int ai;

          int lastMatch = matches.length - 1;
          
          for (ai = 0; ai <= lastMatch; ++ai) {
              Integer bLine = matches[ai];

              if (bLine == null) {
                  onANotB(ai, bi);
              }
              else {
                  while (bi < bLine.intValue()) {
                      onBNotA(ai, bi++);
                  }

                  onMatch(ai, bi++);
              }
          }

          boolean calledFinishA = false;
          boolean calledFinishB = false;

          while (ai <= lastA || bi <= lastB) {

              // last A?
              if (ai == lastA + 1 && bi <= lastB) {
                  if (!calledFinishA && callFinishedA()) {
                      finishedA(lastA);
                      calledFinishA = true;
                  }
                  else {
                      while (bi <= lastB) {
                          onBNotA(ai, bi++);
                      }
                  }
              }

              // last B?
              if (bi == lastB + 1 && ai <= lastA) {
                  if (!calledFinishB && callFinishedB()) {
                      finishedB(lastB);
                      calledFinishB = true;
                  }
                  else {
                      while (ai <= lastA) {
                          onANotB(ai++, bi);
                      }
                  }
              }

              if (ai <= lastA) {
                  onANotB(ai++, bi);
              }

              if (bi <= lastB) {
                  onBNotA(ai, bi++);
              }
          }
      }

      /**
       * Override and return true in order to have <code>finishedA</code> invoked
       * at the last element in the <code>a</code> array.
       */
      protected boolean callFinishedA()
      {
          return false;
      }

      /**
       * Override and return true in order to have <code>finishedB</code> invoked
       * at the last element in the <code>b</code> array.
       */
      protected boolean callFinishedB()
      {
          return false;
      }

      /**
       * Invoked at the last element in <code>a</code>, if
       * <code>callFinishedA</code> returns true.
       */
      protected void finishedA(int lastA)
      {
      }

      /**
       * Invoked at the last element in <code>b</code>, if
       * <code>callFinishedB</code> returns true.
       */
      protected void finishedB(int lastB)
      {
      }

      /**
       * Invoked for elements in <code>a</code> and not in <code>b</code>.
       */
      protected void onANotB(int ai, int bi)
      {
          if (pending == null) {
              pending = new Difference(ai, ai, bi, -1);
          }
          else {
              pending.setDeleted(ai);
          }
      }

      /**
       * Invoked for elements in <code>b</code> and not in <code>a</code>.
       */
      protected void onBNotA(int ai, int bi)
      {
          if (pending == null) {
              pending = new Difference(ai, -1, bi, bi);
          }
          else {
              pending.setAdded(bi);
          }
      }

      /**
       * Invoked for elements matching in <code>a</code> and <code>b</code>.
       */
      protected void onMatch(int ai, int bi)
      {
          if (pending == null) {
              // no current pending
          }
          else {
              diffs.add(pending);
              pending = null;
          }
      }

      /**
       * Compares the two objects, using the comparator provided with the
       * constructor, if any.
       */
      @SuppressWarnings("unchecked")
      protected boolean equals(Object x, Object y)
      {
          return comparator == null ? x.equals(y) : comparator.compare(x, y) == 0;
      }
      
      /**
       * Returns an array of the longest common subsequences.
       */
      @SuppressWarnings("unchecked")
      public Integer[] getLongestCommonSubsequences()
      {
          int aStart = 0;
          int aEnd = a.length - 1;

          int bStart = 0;
          int bEnd = b.length - 1;

          TreeMap<Integer, Integer> matches = new TreeMap<Integer, Integer>();

          while (aStart <= aEnd && bStart <= bEnd && equals(a[aStart], b[bStart])) {
              matches.put(new Integer(aStart++), new Integer(bStart++));
          }

          while (aStart <= aEnd && bStart <= bEnd && equals(a[aEnd], b[bEnd])) {
              matches.put(new Integer(aEnd--), new Integer(bEnd--));
          }

          Map<Object, List<Integer>> bMatches = null;
          if (comparator == null) {
              if (a.length > 0 && a[0] instanceof Comparable) {
                  // this uses the Comparable interface
                  bMatches = new TreeMap<Object, List<Integer>>();
              }
              else {
                  // this just uses hashCode()
                  bMatches = new HashMap<Object, List<Integer>>();
              }
          }
          else {
              // we don't really want them sorted, but this is the only Map
              // implementation (as of JDK 1.4) that takes a comparator.
              bMatches = new TreeMap<Object, List<Integer>>(comparator);
          }

          for (int bi = bStart; bi <= bEnd; ++bi) {
              Object element   = b[bi];
              Object key       = element;
              List<Integer>   positions = bMatches.get(key);
              if (positions == null) {
                  positions = new ArrayList<Integer>();
                  bMatches.put(key, positions);
              }
              positions.add(new Integer(bi));
          }

          thresh = new TreeMap<Integer, Integer>();
          Map<Integer, Object[]> links = new HashMap<Integer, Object[]>();

          for (int i = aStart; i <= aEnd; ++i) {
              Object aElement  = a[i]; // keygen here.
              List<Integer>   positions = bMatches.get(aElement);

              if (positions != null) {
                  Integer  k   = new Integer(0);
                  ListIterator<Integer> pit = positions.listIterator(positions.size());
                  while (pit.hasPrevious()) {
                      Integer j = pit.previous();

                      k = insert(j, k);

                      if (k == null) {
                          // nothing
                      }
                      else {
                          Object value = k.intValue() > 0 ? links.get(new Integer(k.intValue() - 1)) : null;
                          links.put(k, new Object[] { value, new Integer(i), j });
                      }   
                  }
              }
          }

          if (thresh.size() > 0) {
              Integer  ti   = thresh.lastKey();
              Object[] link = links.get(ti);
              while (link != null) {
                  Integer x = (Integer)link[1];
                  Integer y = (Integer)link[2];
                  matches.put(x, y);
                  link = (Object[])link[0];
              }
          }

          return toArray(matches);        
      }

      /**
       * Converts the map (indexed by java.lang.Integers) into an array.
       */
      protected Integer[] toArray(TreeMap<Integer, Integer> map)
      {
          int       size = map.size() == 0 ? 0 : 1 + map.lastKey().intValue();
          Integer[] ary  = new Integer[size];
          Iterator<Integer>  it   = map.keySet().iterator();
          
          while (it.hasNext()) {
              Integer idx = it.next();
              Integer val = map.get(idx);
              ary[idx.intValue()] = val;
          }
          return ary;
      }

      /**
       * Returns whether the integer is not zero (including if it is not null).
       */
      protected boolean isNonzero(Integer i)
      {
          return i != null && i.intValue() != 0;
      }

      /**
       * Returns whether the value in the map for the given index is greater than
       * the given value.
       */
      protected boolean isGreaterThan(Integer index, Integer val)
      {
          Integer lhs = thresh.get(index);
          return lhs != null && val != null && lhs.compareTo(val) > 0;
      }

      /**
       * Returns whether the value in the map for the given index is less than
       * the given value.
       */
      protected boolean isLessThan(Integer index, Integer val)
      {
          Integer lhs = thresh.get(index);
          return lhs != null && (val == null || lhs.compareTo(val) < 0);
      }

      /**
       * Returns the value for the greatest key in the map.
       */
      protected Integer getLastValue()
      {
          return thresh.get(thresh.lastKey());
      }

      /**
       * Adds the given value to the "end" of the threshold map, that is, with the
       * greatest index/key.
       */
      protected void append(Integer value)
      {
          Integer addIdx = null;
          if (thresh.size() == 0) {
              addIdx = new Integer(0);
          }
          else {
              Integer lastKey = thresh.lastKey();
              addIdx = new Integer(lastKey.intValue() + 1);
          }
          thresh.put(addIdx, value);
      }

      /**
       * Inserts the given values into the threshold map.
       */
      protected Integer insert(Integer j, Integer k)
      {
          if (isNonzero(k) && isGreaterThan(k, j) && isLessThan(new Integer(k.intValue() - 1), j)) {
              thresh.put(k, j);
          }
          else {
              int hi = -1;
              
              if (isNonzero(k)) {
                  hi = k.intValue();
              }
              else if (thresh.size() > 0) {
                  hi = thresh.lastKey().intValue();
              }

              // off the end?
              if (hi == -1 || j.compareTo(getLastValue()) > 0) {
                  append(j);
                  k = new Integer(hi + 1);
              }
              else {
                  // binary search for insertion point:
                  int lo = 0;
          
                  while (lo <= hi) {
                      int     index = (hi + lo) / 2;
                      Integer val   = thresh.get(new Integer(index));
                      int     cmp   = j.compareTo(val);

                      if (cmp == 0) {
                          return null;
                      }
                      else if (cmp > 0) {
                          lo = index + 1;
                      }
                      else {
                          hi = index - 1;
                      }
                  }
          
                  thresh.put(new Integer(lo), j);
                  k = new Integer(lo);
              }
          }

          return k;
      }
  }
}
