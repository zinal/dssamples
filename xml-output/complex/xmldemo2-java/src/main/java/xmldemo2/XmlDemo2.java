package xmldemo2;

import com.ibm.is.cc.javastage.api.*;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

/**
 * Example implementation of custom Java-based XML output generator.
 * @author zinal
 */
public class XmlDemo2 extends Processor {

    public final String PROP_OUT_FILE = "OUT_FILE";

    private final List<String> configErrors = new ArrayList<>();

    private String config_OutFile = null;

    private InputLink inputLink = null;

    private FileOutputStream fos = null;
    private XMLStreamWriter writer = null;

    @Override
    public Capabilities getCapabilities() {
      Capabilities capabilities = new Capabilities();
      // we need the input data to process
      capabilities.setMinimumInputLinkCount(1);
      capabilities.setMaximumInputLinkCount(1);
      // there are no output links - data is saved to the file
      capabilities.setMinimumOutputStreamLinkCount(0);
      capabilities.setMaximumOutputStreamLinkCount(0);
      // no reject link
      capabilities.setMinimumRejectLinkCount(0);
      capabilities.setMaximumRejectLinkCount(0);
      // no wave control
      capabilities.setIsWaveGenerator(false);
//      capabilities.setColumnTransferBehavior(ColumnTransferBehavior.COPY_COLUMNS_FROM_SINGLE_INPUT_TO_ALL_OUTPUTS);
      return capabilities;
    }

    @Override
    public List<PropertyDefinition> getUserPropertyDefinitions() {
        List<PropertyDefinition> propList = new ArrayList<>();
        propList.add(new PropertyDefinition(PROP_OUT_FILE, "",
                "Output file",
                "Specifies the path to the output file.",
                PropertyDefinition.Scope.STAGE));
        return propList;
    }

    @Override
    public boolean validateConfiguration(Configuration config, boolean runtime)
            throws Exception {
        configErrors.clear();
        parseProperties(config.getUserProperties());

        if (runtime) {
            inputLink = config.getInputLink(0);
            if (inputLink==null)
                addConfError("input-link-0", "Missing input link");
        }

        return configErrors.isEmpty();
    }

    @Override
    public List<String> getConfigurationErrors() {
        final List<String> retval = new ArrayList<>();
        retval.addAll(configErrors);
        return retval;
    }

    private void parseProperties(Properties props) {
        config_OutFile = props.getProperty(PROP_OUT_FILE, "");
        if (config_OutFile==null || config_OutFile.length() < 1)
            addConfError(PROP_OUT_FILE, "Not specified");
    }

    private void addConfError(String propName, String text) {
        configErrors.add(propName + ": " + text);
    }

    @Override
    public void initialize() throws Exception {
        fos = new FileOutputStream(config_OutFile);
        try {
            XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
            writer = outputFactory.createXMLStreamWriter(fos, "UTF-8");
        } catch(Throwable ex) {
            try { fos.close(); } catch(Exception xxx) {}
            throw new Exception("Failed to create XML writer: " + ex.toString(), ex);
        }
    }

    @Override
    public void process() throws Exception {
        final List<ColumnMetadata> meta = inputLink.getColumnMetadata();
        writer.writeStartDocument("utf-8", "1.0");
        while (true) {
            InputRecord ir = inputLink.readRecord();
            if (ir==null)
                break; // no more input
            writer.writeStartElement("record");
            for (int i=0; i<meta.size(); ++i) {
                Object value = ir.getValue(i+1);
                if (value!=null) {
                    writer.writeStartElement(meta.get(i).getName());
                    writer.writeCharacters(value.toString());
                    writer.writeEndElement();
                }
            }
            writer.writeEndElement();
        }
        writer.writeEndDocument();
        writer.close();
        writer = null;
        fos.close();
        fos = null;
    }

}
