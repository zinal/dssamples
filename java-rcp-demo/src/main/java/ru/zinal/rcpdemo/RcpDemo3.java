package ru.zinal.rcpdemo;

import com.ibm.is.cc.javastage.api.*;
import java.math.BigDecimal;
import java.util.Random;

/**
 * Input and output tables:
 *   create table sample.demo3_src (a decimal(15,2), b decimal(15,2), c decimal(15,2));
 *   create table sample.demo3_dst (a decimal(15,2), b decimal(15,2), c decimal(15,2));
 *   insert into sample.demo3_src values
 *     (101.1, 102.2, 103.3),
 *     (201.1, 202.2, 203.3);
 * @author zinal
 */
public class RcpDemo3 extends Processor {
    
    private Random random;
    private InputLink inputLink;
    private OutputLink outputLink;

    @Override
    public boolean validateConfiguration(Configuration config, boolean runtime)
            throws Exception {
        if (runtime) {
            random = new Random();
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
        final BigDecimal v999 = new BigDecimal(999);
        while (true) {
            // Grab next record
            InputRecord in = inputLink.readRecord();
            if (in==null)
                break; // No more input
            // Create new record, copying the input
            OutputRecord out = outputLink.getOutputRecord(in);
            // Set the values of two first fields
            BigDecimal val = new BigDecimal(100.0 + 10.0 * random.nextGaussian());
            out.setValue(0, val);
            out.setValue(1, val.add(v999));
            // Write output record
            outputLink.writeRecord(out);
        }
    }

}
