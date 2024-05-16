package scm.csm;

import java.time.OffsetDateTime;

import org.apache.axis.components.logger.LogFactory;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Csm {

    protected static Log log =
            LogFactory.getLog(Csm.class.getName());

    @Autowired
    private Environment env;

    public void afipToken() {

        OffsetDateTime dfecha = OffsetDateTime.now();

        proccesToken(dfecha);
    }

    private void proccesToken(OffsetDateTime dfecha) {

//       log.info("infra: " + INFRA + " logitud: " + INFRA.length());

        AfipWSAAClientServices.LoginTicketRequest_xml_string = null;
        String LoginTicketResponse = null;
        String LoginTicketString = null;
        String endpoint = "https://wsaahomo.afip.gov.ar/ws/services/LoginCms";
        String service = "service";
        String dstDN = "cn=wsaahomo,o=afip,c=ar,serialNumber=CUIT 33190985365";

        String p12file = "test.p12";

        String signer = "test";
        String p12pass = "secret";

        Long TicketTime = Long.valueOf(3600000);

        // Create LoginTicketRequest_xml_cms
        AfipWSAAClientServices.LoginTicketRequest_xml_string = null;

        byte[] LoginTicketRequest_xml_cms = null;
        try {
            LoginTicketRequest_xml_cms = AfipWSAAClientServices.create_cms(p12file, p12pass,
                    signer, dstDN, service, TicketTime);
            LoginTicketString = AfipWSAAClientServices.LoginTicketRequest_xml_string;

            System.out.println("LoginTicketString : " + LoginTicketString);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        try {
            LoginTicketResponse = AfipWSAAClientServices.invoke_wsaa ( LoginTicketRequest_xml_cms, endpoint );
            System.out.println(LoginTicketResponse);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


        Document doc = AfipWSAAClientServices.convertStringToDocument(LoginTicketResponse);
        doc.getDocumentElement().normalize();

        //System.out.println("Root Element :" + doc.getDocumentElement().getNodeName());
        //System.out.println("------");

        NodeList nListcredencial = doc.getElementsByTagName("credentials");

        for (int temp = 0; temp < nListcredencial.getLength(); temp++) {
            Node nNode = nListcredencial.item(temp);
            //System.out.println("\nCurrent Element :" + nNode.getNodeName());
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;

                String token = eElement.getElementsByTagName("token").item(0).getTextContent();
                String sign = eElement.getElementsByTagName("sign").item(0).getTextContent();

                System.out.println("token : " + token);
                System.out.println("sign : " + sign);

            }
            //System.out.println("\nCurrent Element 2 :" + nNode.getNodeName());

        }

        //Document docHeader = AfipWSAAClientServices.convertStringToDocument(LoginTicketResponse);
        //docHeader.getDocumentElement().normalize();

        NodeList nheader = doc.getElementsByTagName("header");

        for (int tempp = 0; tempp < nheader.getLength(); tempp++) {
            Node nNodee = nheader.item(tempp);
            //System.out.println("\nCurrent Element :" + nNodee.getNodeName());
            if (nNodee.getNodeType() == Node.ELEMENT_NODE) {
                Element eeElement = (Element) nNodee;


//	            String token = eElement.getElementsByTagName("token").item(0).getTextContent();
                String source = eeElement.getElementsByTagName("source").item(0).getTextContent();
                String destination = eeElement.getElementsByTagName("destination").item(0).getTextContent();
                Long uniqueId=null;
                try {
                    uniqueId = Long.parseLong(eeElement.getElementsByTagName("uniqueId").item(0).getTextContent());
                } catch (Exception e) {
                }

                OffsetDateTime generationTime = OffsetDateTime.parse(eeElement.getElementsByTagName("generationTime").item(0).getTextContent());
                OffsetDateTime expirationTime = OffsetDateTime.parse(eeElement.getElementsByTagName("expirationTime").item(0).getTextContent());


    	            System.out.println("source : " + source);
    	            System.out.println("destination : " + destination);
    	            System.out.println("uniqueId : " + uniqueId);
    	            System.out.println("generationTime : " + generationTime);
    	            System.out.println("expirationTime : " + expirationTime);

            }
        }

    }
}
