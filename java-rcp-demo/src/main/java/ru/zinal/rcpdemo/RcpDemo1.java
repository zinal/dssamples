package ru.zinal.rcpdemo;

import com.ibm.is.cc.javastage.api.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Needs to have RCP enabled.
 * Input and output tables:
 *   create table sample.demo1 (a decimal(15,2));
 *   create table sample.demo2 (a decimal(15,2));
 *   insert into sample.demo1 values (100.1), (200.2);
 * @author zinal
 */
public class RcpDemo1 extends Processor {
    
    private InputLink inputLink;
    private OutputLink outputLink;

    @Override
    public boolean validateConfiguration(Configuration config, boolean runtime)
            throws Exception {
        if (runtime) {
            inputLink = config.getInputLink(0);
            outputLink = config.getOutputLink(0);
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
        final int batchSize = 10;
        final List<OutputRecord> records = new ArrayList<>();
        while (true) {
            Logger.information("* Iteration started!");
            records.clear();
            while (records.size() < batchSize) {
                InputRecord in = inputLink.readRecord();
                if (in==null)
                    break; // No more input
                // Create and fill the output record
                OutputRecord out = outputLink.getOutputRecord(in);
                // ** Here real application extracts necessary input values
                // **   for batch processing and stores with relation to
                // **   the corresponding output records.
                Object tmp = in.getValue(0);
                records.add(out);
                Logger.information("\t..." + records.size());
            }
            if (records.isEmpty()) {
                Logger.information("No more data, exiting.");
                break; // No more data to be processed
            }
            Logger.information("Records read in iteration: " + records.size());
            // ** Here real application performs batch processing
            // **  and stores the results in the related OutputRecord's.
            for (OutputRecord rec : records) {
                outputLink.writeRecord(rec);
                Logger.information("Record written...");
            }
            Logger.information("* Iteration complete!");
        }
    }
    
}
