package scm.csm;

import java.io.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.rpc.ParameterMode;

import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.cms.CMSProcessable;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;


import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import java.util.Base64;
import org.apache.axis.encoding.XMLType;


public class LoginClientServices {

    protected static Log log =
            LogFactory.getLog(LoginClientServices.class.getName());

    public static String LoginTicketRequest_xml_string;

    public static byte [] create_cms(String p12file, String p12pass, String signer, String dstDN, String service,
                                     Long TicketTime) throws Exception {
        PrivateKey pKey = null;
        X509Certificate pCertificate = null;
        byte[] asn1_cms = null;
        CertStore cstore = null;
        String LoginTicketRequest_xml;
        String SignerDN = null;

        //
        // Manage Keys & Certificates
        //
        try {
            File resource = new ClassPathResource(p12file).getFile();

            // Create a keystore using keys from the pkcs#12 p12file
            KeyStore ks = KeyStore.getInstance("PKCS12");
            FileInputStream p12stream = new FileInputStream(resource);
            ks.load(p12stream, p12pass.toCharArray());
            p12stream.close();

            // Get Certificate & Private key from KeyStore
            pKey = (PrivateKey) ks.getKey(signer, p12pass.toCharArray());

            log.info("create_cms : normarl");
            pCertificate = (X509Certificate) ks.getCertificate(signer);

            SignerDN = pCertificate.getSubjectDN().toString();

            // Create a list of Certificates to include in the final CMS
            ArrayList<X509Certificate> certList = new ArrayList<X509Certificate>();
            certList.add(pCertificate);

            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }

            cstore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList), "BC");
        } catch (Exception e) {
            log.error("Manage Keys & Certificates create_cms : ");
            log.error(e);
            throw e;
        }

        //
        // Create XML Message
        //
        LoginTicketRequest_xml = create_LoginTicketRequest(SignerDN, dstDN, service, TicketTime);
        LoginTicketRequest_xml_string = LoginTicketRequest_xml;
        //
        // Create CMS Message
        //
        try {
            // Create a new empty CMS Message
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

            // Add a Signer to the Message
            gen.addSigner(pKey, pCertificate, CMSSignedDataGenerator.DIGEST_SHA1);

            // Add the Certificate to the Message
            gen.addCertificatesAndCRLs(cstore);

            // Add the data (XML) to the Message
            CMSProcessable data = new CMSProcessableByteArray(LoginTicketRequest_xml.getBytes("UTF-8"));

            // Add a Sign of the Data to the Message
            CMSSignedData signed = gen.generate(data, true, "BC");

            //
            asn1_cms = signed.getEncoded();
        } catch (Exception e) {
            log.error("Create CMS XML Message : ");
            log.error(e);
            throw e;
        }


        return (asn1_cms);

    }


    public static String invoke_wsaa(byte[] LoginTicketRequest_xml_cms, String endpoint) throws Exception {
        String LoginTicketResponse = null;


        Integer timeout = 250000;
        try {
            // activa el TLSv1.2

            Service  service = new Service();
            Call call = (Call)service.createCall();

            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("loginCms");
            call.addParameter("request", XMLType.XSD_STRING, ParameterMode.IN);
//			call.addParameter(paramName, paramq, parameterMode);
            call.setReturnType(XMLType.XSD_STRING);
            call.setTimeout(timeout);
            LoginTicketResponse = (String) call.invoke(new Object[] { Base64.getEncoder().encode(LoginTicketRequest_xml_cms) });

        } catch (Exception e) {
            log.error("Cinvoke_wsaa servicio : ");
            log.error(e);
            throw e;
        }

        byte[] encoded = Base64.getEncoder().encode(LoginTicketResponse.getBytes());
        return (new String(encoded));

    }


    public static Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) );
            return doc;
        } catch (Exception e) {
            log.error("convertStringToDocument : ");
            log.error(e);
        }
        return null;
    }


    //
    // Create XML Message for wsaa
    //
    public static String create_LoginTicketRequest(String SignerDN, String dstDN, String service, Long TicketTime) {

        String LoginTicketRequest_xml;

        Date GenTime = new Date();
        GregorianCalendar gentime = new GregorianCalendar();
        GregorianCalendar exptime = new GregorianCalendar();
        String UniqueId = String. valueOf(GenTime.getTime() / 1000);
        //String UniqueId = new Long(GenTime.getTime() / 1000).toString();

        exptime.setTime(new Date(GenTime.getTime() + TicketTime));


        XMLGregorianCalendar XMLGenTime = null;
        try {
            XMLGenTime = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(gentime);
        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } //


        XMLGregorianCalendar XMLExpTime = null;
        try {
            XMLExpTime = DatatypeFactory.newInstance()
                    .newXMLGregorianCalendar(exptime);
        } catch (DatatypeConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } //


        //XMLGregorianCalendarImpl XMLGenTime = new XMLGregorianCalendarImpl(gentime);
        //XMLGregorianCalendarImpl XMLExpTime = new XMLGregorianCalendarImpl(exptime);

        LoginTicketRequest_xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<loginTicketRequest version=\"1.0\">" + "<header>" + "<source>" + SignerDN + "</source>"
                + "<destination>" + dstDN + "</destination>" + "<uniqueId>" + UniqueId + "</uniqueId>"
                + "<generationTime>" + XMLGenTime + "</generationTime>" + "<expirationTime>" + XMLExpTime
                + "</expirationTime>" + "</header>" + "<service>" + service + "</service>" + "</loginTicketRequest>";

        return (LoginTicketRequest_xml);
    }


}
