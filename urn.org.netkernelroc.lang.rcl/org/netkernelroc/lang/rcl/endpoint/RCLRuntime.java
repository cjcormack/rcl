package org.netkernelroc.lang.rcl.endpoint;


import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.converters.DOMConverter;
import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.util.XMLUtils;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.w3c.dom.DOMImplementation;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;


public class RCLRuntime extends StandardAccessorImpl implements ITagProcessor
  {
  protected static final String NS_RCL = "rcl";
  protected static final String TAG_REQUEST = "request";
  protected static final String TAG_IDENTIFIER = "identifier";



  public RCLRuntime()
    {
    this.declareThreadSafe();
    }



  @Override
  public void onSource(INKFRequestContext context) throws NKFException
    {
    boolean tolerant = context.getThisRequest().argumentExists("tolerant");
    String mimeType = context.getThisRequest().getArgumentValue("mimeType");

    org.w3c.dom.Node templateNode = context.source("arg:template", org.w3c.dom.Node.class);
    // Make a copy so we don't change an immutable resource representation
    org.w3c.dom.Document template = getMutableClone(templateNode);

    Document document = DOMConverter.convert(template);


    Element element = processElement(document.getRootElement(), context, tolerant);


    // Convert back!!
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    org.w3c.dom.Document domdoc = null;
    try
      {
      DocumentBuilder builder = factory.newDocumentBuilder();
      DOMImplementation impl = builder.getDOMImplementation();
      domdoc = DOMConverter.convert(element.getDocument(), impl);
      }
    catch (ParserConfigurationException e)
      {

      }

    INKFResponse response = context.createResponseFrom(domdoc);

    if (mimeType != null)
      {
      response.setMimeType(mimeType);
      }
    else
      {
      response.setMimeType("application/xml");
      }
    }

  /**
   * Performs RCL recursive processing the passed element and returns the resulting element
   *
   * @param element
   * @param context
   * @param tolerant
   * @return
   * @throws NKFException
   */
  public  Element processElement(Element element, INKFRequestContext context, boolean tolerant) throws NKFException
    {
    List<Node> childNodes = getChildNodes(element);
    element.removeChildren();

    List<Node> replacementNodes = processChildren(childNodes, context, tolerant);
    for(Node newChild : replacementNodes)
      {
      element.appendChild(newChild );
      }

    return element;
    }




  public  List<Node> processChildren(List<Node> childNodes, INKFRequestContext context, boolean tolerant) throws NKFException
    {
    List<Node> replacementNodes = new ArrayList<Node>();


    for (Node node : childNodes)
      {
      if (node instanceof Element)
        {
        Element element = (Element)node;

        if( element.getNamespaceURI(NS_RCL)!= null)
          {
          ITagProcessor tagProcessor = TagProcessorRegistry.getTagProcessor(element.getLocalName());
          if (tagProcessor != null)
            {
            List<Node> grandchildElements = getChildNodes(element);
            element.removeChildren();
            replacementNodes.addAll(tagProcessor.processChildren(grandchildElements, context, tolerant));
            }
          else
            {
            //Handle exception
            }
          }
        else
          {
          element = processElement(element, context, tolerant);
          replacementNodes.add(element);
          }
        }
      else
        {
        replacementNodes.add(node);
        }
      }
    return replacementNodes;
    }


  public List<Node> getChildNodes(final Element element)
    {
    List<Node> childNodes = new ArrayList<Node>();

    for(int i=0; i< element.getChildCount(); i++ )
      {
      childNodes.add(element.getChild(i));
      }
    return childNodes;
    }

  protected Element processRequest(final Element requestElement, final INKFRequestContext context, boolean tolerant) throws NKFException
    {
    Element returnValue = null;
    try
      {
      INKFRequest request = buildRequest(requestElement, context);
      request.setRepresentationClass(org.w3c.dom.Node.class);

      org.w3c.dom.Node node = (org.w3c.dom.Node)context.issueRequest(request);

      org.w3c.dom.Document template = getMutableClone(node);

      returnValue = DOMConverter.convert(template).getRootElement();
      returnValue = (Element)returnValue.copy();
      }
    catch(NKFException e)
      {
      exceptionHandler(context, e, requestElement, tolerant);
      }
      return returnValue;
    }




  public INKFRequest buildRequest(final Element requestElement, final INKFRequestContext context) throws NKFException
      {
      INKFRequest request = null;

      Elements elements = requestElement.getChildElements();

      // We have to know the identifier first
      for (int i=0; i<elements.size(); i++)
        {
        Element e = elements.get(i);
        if (e.getNamespaceURI(NS_RCL)!=null && TAG_IDENTIFIER.equals(e.getLocalName()))
          {
          String uri = e.getValue();
          request = context.createRequest(uri);
          }
        }

      return request;
      }


  public org.w3c.dom.Document getMutableClone(final org.w3c.dom.Node node) throws NKFException
    {
    org.w3c.dom.Document result = null;
    try
      {
      if (node instanceof org.w3c.dom.Document)
        {
        result = (org.w3c.dom.Document) XMLUtils.safeDeepClone(node);
        }
      else
        {
        result = org.netkernel.layer0.util.XMLUtils.newDocument();
        result.appendChild(result.importNode(node, true));
        }
      }
    catch (ParserConfigurationException e)
      {
      throw new NKFException("DOM Parsing failure", "Bad Stuff", e);
      }
    return result;
    }


  public void exceptionHandler(INKFRequestContext context, Exception exception, Node node, boolean tolerant)  throws NKFException
     {
     if(tolerant)
       {
       context.logFormatted(INKFLocale.LEVEL_DEBUG, "Message", "Path to element", node, "target");
       }
     else
       {
       throw context.createFormattedException("ID", "Message", "Location", exception, "");
       }
     }
  }
