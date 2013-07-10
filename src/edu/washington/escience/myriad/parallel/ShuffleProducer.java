package edu.washington.escience.myriad.parallel;

import edu.washington.escience.myriad.DbException;
import edu.washington.escience.myriad.TupleBatch;
import edu.washington.escience.myriad.TupleBatchBuffer;
import edu.washington.escience.myriad.operator.Operator;

/**
 * The producer part of the Shuffle Exchange operator.
 * 
 * ShuffleProducer distributes tuples to the workers according to some partition function (provided as a
 * PartitionFunction object during the ShuffleProducer's instantiation).
 * 
 */
public class ShuffleProducer extends Producer {

  /** Required for Java serialization. */
  private static final long serialVersionUID = 1L;

  /**
   * the partition function.
   * */
  private final PartitionFunction<?, ?> partitionFunction;

  /**
   * @param child the child who provides data for this producer to distribute.
   * @param operatorID destination operators the data goes
   * @param workerIDs destination workers the data goes.
   * @param pf the partition function
   * */
  public ShuffleProducer(final Operator child, final ExchangePairID operatorID, final int[] workerIDs,
      final PartitionFunction<?, ?> pf) {
    super(child, operatorID, workerIDs);
    partitionFunction = pf;
  }

  /**
   * @return the partition function I'm using.
   * */
  public final PartitionFunction<?, ?> getPartitionFunction() {
    return partitionFunction;
  }

  @Override
  protected final void consumeTuples(final TupleBatch tup) throws DbException {
    TupleBatchBuffer[] buffers = getBuffers();
    tup.partition(partitionFunction, buffers);
    TupleBatch dm = null;
    for (int i = 0; i < numChannels(); i++) {
      final TupleBatchBuffer etb = buffers[i];
      while ((dm = etb.popAnyUsingTimeout()) != null) {
        try {
          writeMessage(i, dm);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
  }

  @Override
  protected final void childEOS() throws DbException {
    TupleBatch dm = null;
    TupleBatchBuffer[] buffers = getBuffers();
    for (int i = 0; i < numChannels(); i++) {
      while ((dm = buffers[i].popAny()) != null) {
        try {
          writeMessage(i, dm);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
    for (int p = 0; p < numChannels(); p++) {
      super.channelEnds(p);
    }

  }

  @Override
  protected final void childEOI() throws DbException {
    TupleBatch dm = null;
    TupleBatchBuffer[] buffers = getBuffers();
    TupleBatch eoiTB = TupleBatch.eoiTupleBatch(getSchema());
    for (int i = 0; i < numChannels(); i++) {
      while ((dm = buffers[i].popAny()) != null) {
        try {
          writeMessage(i, dm);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    }
    for (int i = 0; i < numChannels(); i++) {
      try {
        writeMessage(i, eoiTB);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }
}
