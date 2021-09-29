package ru.zinal.rcpdemo;

import com.ibm.is.cc.javastage.api.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Needs to have RCP enabled.
 * Input and output tables:
 *   create table sample.demo1 (a decimal(15,2));
 *   create table sample.demo2 (a decimal(15,2));
 *   insert into sample.demo1 values (100.1), (200.2);
 * @author zinal
 */
public class RcpDemo2 extends Processor {
    
    private InputLink inputLink;
    private OutputLink outputLink;
    private Map<Integer, Integer> indexMap;

    @Override
    public boolean validateConfiguration(Configuration config, boolean runtime)
            throws Exception {
        if (runtime) {
            inputLink = config.getInputLink(0);
            outputLink = config.getOutputLink(0);
            // index map is necessary to properly construct OutputRecords
            indexMap = new HashMap<>();
            for (ColumnMetadata out : outputLink.getColumnMetadata()) {
                if (out.getIndex() < inputLink.getColumnCount()) {
                    // try to find the corresponding column by position
                    ColumnMetadata inLucky 
                            = inputLink.getColumn(out.getIndex());
                    if (inLucky!=null && inLucky.getName()
                            .equalsIgnoreCase(out.getName())) {
                        indexMap.put(out.getIndex(), inLucky.getIndex());
                        continue;
                    }
                }
                // if the positions are meshed, look up by column names
                for (ColumnMetadata in : inputLink.getColumnMetadata()) {
                    if (in.getName().equalsIgnoreCase(out.getName())) {
                        indexMap.put(out.getIndex(), in.getIndex());
                        break;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Capabilities getCapabilities() {
      Capabilities capabilities = new Capabilities();
      capabilities.setMinimumInputLinkCount(1);
      capabilities.setMaximumInputLinkCount(1);
      capabilities.setMinimumOutputStreamLinkCount(1);
      capabilities.setMaximumOutputStreamLinkCount(1);
      capabilities.setMaximumRejectLinkCount(0);
      capabilities.setIsWaveGenerator(false);
      return capabilities;
    }

    @Override
    public void process() throws Exception {
        while (true) {
            // Grab next record
            InputRecord in = inputLink.readRecord();
            if (in==null)
                break; // No more input
            // Manual replacement for getOutputRecord(in)
            OutputRecord out = outputLink.getOutputRecord();
            for (Map.Entry<Integer, Integer> me : indexMap.entrySet()) {
                out.setValue(me.getKey(), in.getValue(me.getValue()));
            }
            // Write output record
            outputLink.writeRecord(out);
        }
    }
    
}
