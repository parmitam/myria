package edu.washington.escience.myriad.parallel;

import org.jboss.netty.channel.Channel;

import edu.washington.escience.myriad.DbException;
import edu.washington.escience.myriad.TupleBatch;
import edu.washington.escience.myriad.TupleBatchBuffer;
import edu.washington.escience.myriad.operator.Operator;
import edu.washington.escience.myriad.proto.TransportProto.TransportMessage;
import edu.washington.escience.myriad.util.IPCUtils;

/**
 * The producer part of the Collect Exchange operator.
 * 
 * The producer actively pushes the tuples generated by the child operator to the paired LocalMultiwayConsumer.
 * 
 */
public final class LocalMultiwayProducer extends Producer {
  /** Required for Java serialization. */
  private static final long serialVersionUID = 1L;

  public LocalMultiwayProducer(final Operator child, final ExchangePairID[] operatorIDs) {
    super(child, operatorIDs);
  }

  @Override
  protected void consumeTuples(final TupleBatch tup) throws DbException {
    TupleBatchBuffer[] buffers = getBuffers();
    Channel[] ioChannels = getChannels();
    TransportMessage dm = null;
    tup.compactInto(buffers[0]);
    while ((dm = buffers[0].popFilledAsTM(super.outputSeq[0])) != null) {
      super.outputSeq[0]++;
      for (Channel ch : ioChannels) {
        ch.write(dm);
      }
    }
  }

  @Override
  protected void childEOS() throws DbException {
    TransportMessage dm = null;
    TupleBatchBuffer[] buffers = getBuffers();
    Channel[] ioChannels = getChannels();
    while ((dm = buffers[0].popAnyAsTM(super.outputSeq[0])) != null) {
      super.outputSeq[0]++;
      for (Channel ch : ioChannels) {
        ch.write(dm);
      }
    }
    for (Channel channel : ioChannels) {
      channel.write(IPCUtils.EOS);
    }
  }

  @Override
  protected void childEOI() throws DbException {
    TransportMessage dm = null;
    TupleBatchBuffer[] buffers = getBuffers();
    Channel[] ioChannels = getChannels();
    while ((dm = buffers[0].popAnyAsTM(super.outputSeq[0])) != null) {
      super.outputSeq[0]++;
      for (Channel ch : ioChannels) {
        ch.write(dm);
      }
    }
    for (Channel channel : ioChannels) {
      channel.write(IPCUtils.EOI);
    }
  }
}
