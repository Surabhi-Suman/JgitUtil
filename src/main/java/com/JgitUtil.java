package com;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Arrays;

public class JgitUtil {
    private static String userName = System.getenv(Constants.USER_NAME);
    private static String password = System.getenv(Constants.PASSWORD);
    private static String git_url = System.getenv(Constants.URL);
    private static String version_to_be_updated = System.getenv(Constants.VERSION);
    public static void main(String args[]) throws IOException, GitAPIException,ParserConfigurationException,SAXException, XPathExpressionException,TransformerException {

        File tempFile = File.createTempFile("TestGitRepository", "/");
        if (!tempFile.delete()) {
            throw new IOException("Could not delete temporary file " + tempFile);
        }
        Git git = Git.cloneRepository()
                .setURI(git_url)
                .setDirectory(tempFile)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, password))
                .call();
        System.out.println("Having repository: " + git.getRepository().getDirectory());
        FilenameFilter filenameFilter = (dir, name) -> name.endsWith(".xml");
        File pom = null;
        if (tempFile.isDirectory()) {
            File[] files = tempFile.listFiles(filenameFilter);
            pom = Arrays.stream(files).filter(e -> e.getName().contains("pom")).findFirst().get();
        }
        System.out.println("Name"+pom.getName());
        System.out.println("Path"+pom.getAbsolutePath());


        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder b = dbf.newDocumentBuilder();
        Document doc = b.parse(new File(pom.getAbsolutePath()));
        XPath xPath = XPathFactory.newInstance().newXPath();
        Node versionNode = (Node) xPath.compile("/project/parent/version").evaluate(doc, XPathConstants.NODE);
        versionNode.setTextContent(version_to_be_updated);
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty(OutputKeys.METHOD, "xml");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource domSource = new DOMSource(doc);
        StreamResult sr = new StreamResult(new File(pom.getAbsolutePath()));
        tf.transform(domSource, sr);
        git.branchCreate()
                .setName(Constants.BRANCH_NAME)
                .call();
        Ref checkout = git.checkout().setName(Constants.BRANCH_NAME).call();
        System.out.println("Result of checking out the branch: " + checkout);
        git.add()
                .addFilepattern("pom.xml")
                .call();
        RevCommit revCommit = git.commit()
                .setMessage("Updated pom")
                .call();

        System.out.println("Committed file " + "pom.xml" + " as " + revCommit + " to repository at ");
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(Constants.USER_NAME, Constants.PASSWORD)).call();




    }




}