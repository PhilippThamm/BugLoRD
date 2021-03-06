package se.de.hu_berlin.informatik.experiments.ibugs.utils;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An event handler for the extraction of bug ids and their properties from the
 * repository.xml file that is supposed to be inside the iBugs project root directory.
 *
 * @author Roy Lieck
 */
public class SAXEventHandler4IBugsRepoDescriptor extends DefaultHandler {

    private static final String BUG_KEY = "bug";
    private static final String ID_KEY = "id";
    private static final String TRANSACTIONID_KEY = "transactionid";

    private final Collection<BugDataFromRDWrapper> allBugs = new ArrayList<>();

    /**
     * @return the allBugs
     */
    public Collection<BugDataFromRDWrapper> getAllBugs() {
        return allBugs;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {

        if (localName.equalsIgnoreCase(BUG_KEY)) {
            String id = atts.getValue(ID_KEY);
            String transId = atts.getValue(TRANSACTIONID_KEY);

            allBugs.add(new BugDataFromRDWrapper(id, transId));
        }

    }

}
