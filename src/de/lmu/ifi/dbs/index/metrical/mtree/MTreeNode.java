package de.lmu.ifi.dbs.index.metrical.mtree;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.persistent.PageFile;
import de.lmu.ifi.dbs.index.Node;
import de.lmu.ifi.dbs.index.Identifier;
import de.lmu.ifi.dbs.index.BreadthFirstEnumeration;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a node in a M-Tree.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MTreeNode<O extends MetricalObject, D extends Distance> implements Node {
  /**
   * Logger object for logging messages.
   */
  private static Logger logger;

  /**
   * The level for logging messages.
   */
  private static Level level = Level.ALL;

  /**
   * The file storing the RTree.
   */
  private PageFile<MTreeNode<O, D>> file;

  /**
   * The unique id if this node.
   */
  Integer nodeID;

  /**
   * The id of the parent of this node.
   */
  Integer parentID;

  /**
   * The index of this node in its parent node.
   */
  int index;

  /**
   * The number of entries in this node.
   */
  int numEntries;

  /**
   * Indicates wether this node is a leaf node.
   */
  boolean isLeaf;

  /**
   * The entries (children) of this node.
   */
  Entry<O, D>[] entries;

  /**
   * Empty constructor for Externalizable interface.
   */
  public MTreeNode() {
  }

  /**
   * Creates a new Node object.
   *
   * @param file     the file storing the RTree
   * @param capacity the capacity (maximum number of entries plus 1 for overflow) of this node
   * @param isLeaf   indicates wether this node is a leaf node
   */
  public MTreeNode(PageFile<MTreeNode<O, D>> file, int capacity, boolean isLeaf) {
    initLogger();
    this.file = file;
    this.nodeID = null;
    this.parentID = null;
    this.index = -1;
    this.numEntries = 0;
    //noinspection unchecked
    this.entries = new Entry[capacity];
    this.isLeaf = isLeaf;
  }

  /**
   * Returns the id of this node.
   *
   * @return the id of this node
   */
  public Integer getID() {
    return nodeID;
  }

  /**
   * Sets the unique id of this Page.
   *
   * @param id the id to be set
   */
  public void setID(int id) {
    this.nodeID = id;
  }

  /**
   * Sets the page file of this page.
   *
   * @param file the page file to be set
   */
  public void setFile(PageFile file) {
    //noinspection unchecked
    this.file = file;
  }

  /**
   * Returns the id of this node.
   *
   * @return the id of this node
   */
  public int getNodeID() {
    return nodeID;
  }

  /**
   * Returns the id of the parent node of this node.
   *
   * @return the id of the parent node of this node
   */
  public int getParentID() {
    return parentID;
  }

  /**
   * Returns the number of entries of this node.
   *
   * @return the number of entries of this node
   */
  public int getNumEntries() {
    return numEntries;
  }

  /**
   * Returns true if this node is a leaf node, false otherwise.
   *
   * @return true if this node is a leaf node, false otherwise
   */
  public boolean isLeaf() {
    return isLeaf;
  }

  /**
   * Returns an enumeration of the children of this node.
   *
   * @return an enumeration of the children of this node
   */
  public Enumeration<Identifier> children() {
    return new Enumeration<Identifier>() {
      int count = 0;

      public boolean hasMoreElements() {
        return count < numEntries;
      }

      public Entry<O, D> nextElement() {
        synchronized (MTreeNode.this) {
          if (count < numEntries) {
            return entries[count++];
          }
        }
        throw new NoSuchElementException();
      }
    };
  }

  /**
   * Returns the entry at the specified index.
   *
   * @param index the index of the entry to be returned
   * @return the entry at the specified index
   */
  public Entry<O, D> getEntry(int index) {
    return entries[index];
  }

  /**
   * @see Object#equals(Object)
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MTreeNode)) return false;

    final MTreeNode<O, D> node = (MTreeNode<O, D>) o;
    if (nodeID != node.nodeID) return false;

    if (parentID != node.parentID)
      throw new RuntimeException("Should never happen! parentID: " +
                                 parentID + " != " + node.parentID);

    if (index != node.index)
      throw new RuntimeException("Should never happen! index " +
                                 index + " != " + node.index);

    if (numEntries != node.numEntries)
      throw new RuntimeException("Should never happen! numEntries " +
                                 numEntries + " != " + node.numEntries);

    for (int i = 0; i < numEntries; i++) {
      Entry<O, D> e1 = entries[i];
      Entry<O, D> e2 = node.entries[i];
      if (!e1.equals(e2))
        throw new RuntimeException("Should never happen! entry " +
                                   e1 + " != " + e2);
    }

    return true;
  }

  /**
   * @see Object#hashCode()
   */
  public int hashCode() {
    return nodeID;
  }

  /**
   * Computes and returns the covering radius of this node.
   * If this node is a leaf node, null will be returned.
   *
   * @return the covering radius of this node
   */
  public D coveringRadius(Entry<O,D> rootEntry, DistanceFunction<O,D> distanceFunction) {
    throw new UnsupportedOperationException();
    /*
    if (rootEntry.getObject() == null) return null;

    D result = distanceFunction.nullDistance();

    Identifier rootID = new Identifier() {
      /**
       * Returns the value of this identifier.
       *
       * @return the value of this identifier
       */
      /*public Integer value() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
      }

      /**
       * Returns true, if this identifier represents a node id, false otherwise.
       *
       * @return true, if this identifier represents a node id, false otherwise
       */
      /*public boolean isNodeID() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
      }
    };
    BreadthFirstEnumeration<MTreeNode<O,D>> bfs = new BreadthFirstEnumeration<MTreeNode<O,D>>(file, rootID);
    while (bfs.hasMoreElements()) {
      Entry e = (Entry) bfs.nextElement();
      D dist = distanceFunction.distance(e.getObject(), rootEntry.getObject());
      if (result.compareTo(dist) < 0)
        result = dist;
    }
    return result;
  */
  }

  /**
   * Returns a string representation of this node.
   *
   * @return a string representation of this node
   */
  public String toString() {
    if (isLeaf) return "LeafNode " + nodeID;
    else
      return "DirNode " + nodeID;
  }

  /**
   * The object implements the writeExternal method to save its contents
   * by calling the methods of DataOutput for its primitive values or
   * calling the writeObject method of ObjectOutput for objects, strings,
   * and arrays.
   *
   * @param out the stream to write the object to
   * @throws java.io.IOException Includes any I/O exceptions that may occur
   * @serialData Overriding methods should use this tag to describe
   * the data layout of this Externalizable object.
   * List the sequence of element types and, if possible,
   * relate the element to a public/protected field and/or
   * method of this Externalizable class.
   */
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(nodeID);
    out.writeInt(parentID);
    out.writeInt(index);
    out.writeInt(numEntries);
    out.writeObject(entries);
  }

  /**
   * The object implements the readExternal method to restore its
   * contents by calling the methods of DataInput for primitive
   * types and readObject for objects, strings and arrays.  The
   * readExternal method must read the values in the same sequence
   * and with the same types as were written by writeExternal.
   *
   * @param in the stream to read data from in order to restore the object
   * @throws java.io.IOException    if I/O errors occur
   * @throws ClassNotFoundException If the class for an object being
   *                                restored cannot be found.
   */
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.nodeID = in.readInt();
    this.parentID = in.readInt();
    this.index = in.readInt();
    this.numEntries = in.readInt();
    this.entries = (Entry<O, D>[]) in.readObject();
  }

  /**
   * Returns the index of this node in its parent.
   *
   * @return the index of this node in its parent
   */
  public int getIndex() {
    return index;
  }

  /**
   * Adds a new leaf entry to this node's children.
   * Note that this node must be a leaf node.
   *
   * @param entry the entry to be added
   */
  protected void addLeafEntry(LeafEntry<O, D> entry) {
    // directory node
    if (! isLeaf) {
      throw new UnsupportedOperationException("Node is not a leaf node!");
    }

    // leaf node
    entries[numEntries++] = entry;
  }

  /**
   * Adds a new node to this node's children.
   * Note that this node must be a directory node.
   *
   * @param node           the node to be added
   * @param routingObject  the routing object of the entry
   * @param parentDistance the parent distance of the entry
   * @param coveringRadius the covering radius of the entry
   */
  protected void addNode(MTreeNode<O, D> node, O routingObject, D parentDistance, D coveringRadius) {
    // leaf node
    if (isLeaf) {
      throw new UnsupportedOperationException("Node is a leaf node!");
    }

    // directory node
    entries[numEntries++] = new DirectoryEntry<O, D>(routingObject, parentDistance, node.getID(), coveringRadius);

    node.parentID = nodeID;
    node.index = numEntries - 1;
    file.writePage(node);
  }

  /**
   * Splits the entries of this node into a new node at the specified splitPoint
   * and returns the newly created node.
   *
   * @param assignmentsToFirst  the assignment to this node
   * @param assignmentsToSecond the assignment to the new node
   * @return the newly created split node
   */
  protected MTreeNode<O, D> splitEntries(List<Entry<O, D>> assignmentsToFirst, List<Entry<O, D>> assignmentsToSecond) {
    if (isLeaf) {
      MTreeNode<O, D> newNode = createNewLeafNode(entries.length);
      file.writePage(newNode);

      //noinspection unchecked
      this.entries = new Entry[entries.length];
      this.numEntries = 0;

      // assignments to this node
      String msg = "\n";
      for (Entry<O, D> entry : assignmentsToFirst) {
        msg += "n_" + getID() + " " + entry + "\n";
        entries[numEntries++] = entry;
      }

      // assignments to the new node
      for (Entry<O, D> entry : assignmentsToSecond) {
        msg += "n_" + newNode.getID() + " " + entry + "\n";
        newNode.entries[newNode.numEntries++] = entry;
      }
      logger.fine(msg);
      return newNode;
    }

    else {
      MTreeNode<O, D> newNode = createNewDirectoryNode(entries.length);
      file.writePage(newNode);

      //noinspection unchecked
      this.entries = new Entry[entries.length];
      this.numEntries = 0;

      String msg = "\n";
      for (Entry<O, D> e : assignmentsToFirst) {
        DirectoryEntry<O, D> entry = (DirectoryEntry<O, D>) e;
        msg += "n_" + getID() + " " + entry + "\n";
        MTreeNode<O, D> node = file.readPage(entry.getNodeID());
        addNode(node, entry.getObject(), entry.getParentDistance(), entry.getCoveringRadius());
      }

      for (Entry<O, D> e : assignmentsToSecond) {
        DirectoryEntry<O, D> entry = (DirectoryEntry<O, D>) e;
        msg += "n_" + newNode.getID() + " " + entry + "\n";
        MTreeNode<O, D> node = file.readPage(entry.getNodeID());
        newNode.addNode(node, entry.getObject(), entry.getParentDistance(), entry.getCoveringRadius());
      }
      logger.fine(msg);
      return newNode;
    }
  }

  /**
   * Tests this node (for debugging purposes).
   */
  protected void test() {
    // leaf node
    if (isLeaf) {
      for (int i = 0; i < entries.length; i++) {
        Entry e = entries[i];
        if (i < numEntries && e == null)
          throw new RuntimeException("i < numEntries && entry == null");
        if (i >= numEntries && e != null)
          throw new RuntimeException("i >= numEntries && entry != null");
      }
    }

    // dir node
    else {
      MTreeNode<O, D> tmp = file.readPage(((DirectoryEntry<O, D>) entries[0]).getNodeID());
      boolean childIsLeaf = tmp.isLeaf();

      for (int i = 0; i < entries.length; i++) {
        DirectoryEntry<O, D> e = (DirectoryEntry<O, D>) entries[i];

        if (i < numEntries && e == null)
          throw new RuntimeException("i < numEntries && entry == null");

        if (i >= numEntries && e != null)
          throw new RuntimeException("i >= numEntries && entry != null");

        if (e != null) {
          MTreeNode<O, D> node = file.readPage(e.getNodeID());

          if (childIsLeaf && !node.isLeaf()) {
            throw new RuntimeException("Wrong Child in " + this + " at " + i +
                                       ": child id no leaf, but node is leaf!");
          }

          if (!childIsLeaf && node.isLeaf())
            throw new RuntimeException("Wrong Child in " + this + " at " + i +
                                       ": child id no leaf, but node is leaf!");


          if (node.parentID != nodeID)
            throw new RuntimeException("Wrong parent in node " + e.getNodeID() +
                                       ": " + node.parentID + " != " + nodeID);

          if (node.index != i) {
            throw new RuntimeException("Wrong index in node " + node +
                                       ": ist " + node.index + " != " + i +
                                       " soll, parent is " + this);
          }
        }
      }
    }
  }

  /**
   * Tests, if the covering radii are correctly set.
   */
  protected void testCoveringRadius(O object, D coveringRadius, DistanceFunction<O, D> distanceFunction) {
    if (object == null) return;
    for (int i = 0; i < numEntries; i++) {
      D dist = distanceFunction.distance(entries[i].getObject(), object);
      if (dist.compareTo(coveringRadius) > 0)
        throw new RuntimeException("dist > cr \n" +
                                   dist + " > " + coveringRadius + "\n" +
                                   "in " + this.toString() + " at entry " + entries[i] + "\n" +
                                   "distance(" + entries[i].getObject().getID() + " - " + object.getID() + ")");
    }

  }

  /**
   * Tests, if the parent distances are correctly set.
   */
  protected void testParentDistance(O object, DistanceFunction<O, D> distanceFunction) {
    for (int i = 0; i < numEntries; i++) {
      if (object != null) {
        D dist = distanceFunction.distance(entries[i].getObject(), object);
        if (! entries[i].getParentDistance().equals(dist)) {
          throw new RuntimeException("entry.pd != dist: \n" +
                                     entries[i].getParentDistance() + " != " + dist + "\n" +
                                     "in " + this.toString() + " at entry " + entries[i] + "\n" +
                                     "distance(" + entries[i].getObject().getID() + " - " + object.getID() + ")");
        }
      }
      else {
        if (entries[i].getParentDistance() != null)
          throw new RuntimeException("entry.pd != null : \n" +
                                     entries[i].getParentDistance() + " != null \n" +
                                     "in " + this.toString() + " at entry " + entries[i] + "\n");
      }
    }
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MTreeNode<O, D> createNewLeafNode
  (
  int capacity) {
    return new MTreeNode<O, D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MTreeNode<O, D> createNewDirectoryNode
  (
  int capacity) {
    return new MTreeNode<O, D>(file, capacity, false);
  }

  /**
   * Initializes the logger object.
   */
  private void initLogger() {
    logger = Logger.getLogger(getClass().toString());
    logger.setLevel(level);
  }



}
