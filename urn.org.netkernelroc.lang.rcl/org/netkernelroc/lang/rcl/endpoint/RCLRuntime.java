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

/**
 * RCL Runtime endpoint
 *
 * @author Randolph Kahle
 */
public class RCLRuntime extends StandardAccessorImpl
  {
  private static final String NS_RCL = "rcl";
  private static final String TAG_INCLUDE = "include";
  private static final String TAG_IF = "if";
  private static final String TAG_TRUE = "true";
  private static final String TAG_FALSE = "false";
  private static final String TAG_IDENTIFIER = "identifier";
  private static final String TAG_REQUEST = "request";

  public RCLRuntime()
    {
    declareThreadSafe();
    }


  @Override
  public void onSource(final INKFRequestContext context) throws NKFException, ParserConfigurationException
    {
    boolean tolerant = context.getThisRequest().argumentExists("tolerant");
    String mimeType = context.getThisRequest().getArgumentValue("mimeType");

    org.w3c.dom.Node templateNode = context.source("arg:template", org.w3c.dom.Node.class);
    // Make a copy so we don't change an immutable resource representation
    org.w3c.dom.Document template = getMutableClone(templateNode);

    Document document = DOMConverter.convert(template);
    Element element = document.getRootElement();


    processTemplate(element, context, tolerant);

    // Convert back!!
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    DOMImplementation impl = builder.getDOMImplementation();

    org.w3c.dom.Document domdoc = DOMConverter.convert(element.getDocument(), impl );
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




  //========== Protected ==========

  protected void processTemplate(final Element element, final INKFRequestContext context, boolean tolerant)   throws NKFException, ParserConfigurationException
    {
    List<Node> replacementNodes = new ArrayList<Node>();

    element.removeNamespaceDeclaration(NS_RCL);

    List<Element> childElements = stripAndReturnChildElements(element, context, tolerant);

    for (Element e : childElements)
      {
      if( e.getNamespaceURI(NS_RCL)!= null)
        {
        if (TAG_INCLUDE.equals(e.getLocalName()))
          {
          replacementNodes.addAll(processInclude(e, context, tolerant));
          }
        else if (TAG_IF.equals(e.getLocalName()))
          {
          replacementNodes.addAll(processIf(e, context, tolerant));
          }
        else
          {
          // unsupported rcl: tag. Need exception handling code
          }
        }
      else
        {
        replacementNodes.add(e);
        }
      }

    for(Node node : replacementNodes)
      {
      element.appendChild(node);
      }
    }





  protected List<Node> processInclude(final Element includeElement, final INKFRequestContext context, boolean tolerant) throws NKFException, ParserConfigurationException
    {
    List<Node> replacementNodes = new ArrayList<Node>();

    List<Element> childElements = stripAndReturnChildElements(includeElement, context, tolerant);

    for(Element e : childElements)
      {
      if( e.getNamespaceURI(NS_RCL)!= null)
         {
         if (TAG_REQUEST.equals(e.getLocalName()))
           {
           replacementNodes.add(processRequest(e, context, tolerant).copy());
           }
         else if(TAG_INCLUDE.equals(e.getLocalName()))
           {
           replacementNodes.addAll(processInclude(e, context,  tolerant));
           }
         else if(TAG_IF.equals(e.getLocalName()))
           {
           replacementNodes.addAll(processIf(e, context,  tolerant));
           }
         else
           {
           // unsupported rcl: tag. Need exception handling code
           }
         }
      else
        {
        replacementNodes.add(e);
        }
      }

    return replacementNodes;
    }



  protected Element processRequest(final Element requestElement, final INKFRequestContext context, boolean tolerant) throws NKFException, ParserConfigurationException
    {
    Element returnValue = null;
    try
      {
      INKFRequest request = buildRequest(requestElement, context);
      request.setRepresentationClass(org.w3c.dom.Node.class);

      org.w3c.dom.Node node = (org.w3c.dom.Node)context.issueRequest(request);

      org.w3c.dom.Document template = getMutableClone(node);

      returnValue = DOMConverter.convert(template).getRootElement();
      }
    catch(NKFException e)
      {
      exceptionHandler(context, e, requestElement, tolerant);
      }
      return returnValue;
    }



  protected  boolean processRequestForBoolean(final Element requestElement, final INKFRequestContext context, boolean tolerant) throws NKFException
    {
    boolean returnValue = true;
    try
      {
      INKFRequest request = buildRequest(requestElement, context);
      request.setRepresentationClass(java.lang.Boolean.class);
      returnValue = (Boolean)context.issueRequest(request);
      }
    catch(NKFException e)
      {
      exceptionHandler(context,  e, requestElement, tolerant);
      }

    return returnValue;
    }



    protected INKFRequest buildRequest(final Element requestElement, final INKFRequestContext context) throws NKFException
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



  protected List<Node> processIf(final Element ifElement, final INKFRequestContext context, boolean tolerant)   throws NKFException, ParserConfigurationException
    {
    List<Node> replacementNodes = new ArrayList<Node>();
    boolean test = false;

    List<Element> childElements = stripAndReturnChildElements(ifElement, context, tolerant);

    // Find and issue the request
    for(Element e : childElements)
      {
      if( e.getNamespaceURI(NS_RCL)!= null && TAG_REQUEST.equals(e.getLocalName()))
         {
         test = processRequestForBoolean(e, context, tolerant);
         }
      }

    for(Element e : childElements)
      {
       if (e.getNamespaceURI(NS_RCL)!=null)
         {
         if (TAG_TRUE.equals(e.getLocalName()) && test)
           {
           replacementNodes.addAll(processIfTrueFalse(e, context, tolerant));
           }
         else if (TAG_FALSE.equals(e.getLocalName()) && !test)
           {
           replacementNodes.addAll(processIfTrueFalse(e, context, tolerant));
           }
         else if(TAG_INCLUDE.equals(e.getLocalName()))
           {
           replacementNodes.addAll(processInclude(e, context,  tolerant));
           }
         else if(TAG_IF.equals(e.getLocalName()))
           {
           replacementNodes.addAll(processIf(e, context,  tolerant));
           }
         else
           {
           // unsupported rcl: tag. Need exception handling code
           }
         }
       else
         {
         replacementNodes.add(e);
         }
       }

    return replacementNodes;
    }


  protected List<Node> processIfTrueFalse(final Element trueFalseElement, final INKFRequestContext context, boolean tolerant)  throws NKFException, ParserConfigurationException
    {
    List<Node> replacementNodes = new ArrayList<Node>();

    List<Element> childElements = stripAndReturnChildElements(trueFalseElement, context, tolerant);

    for(Element e: childElements)
      {
      if (e.getNamespaceURI(NS_RCL)!=null)
        {
        if(TAG_INCLUDE.equals(e.getLocalName()))
          {
          replacementNodes.addAll(processInclude(e, context,  tolerant));
          }
        else if(TAG_IF.equals(e.getLocalName()))
          {
          replacementNodes.addAll(processIf(e, context,  tolerant));
          }
        else
          {
           // unsupported rcl: tag. Need exception handling code
          }
        }
      else
        {
        replacementNodes.add(e);
        }
      }

   return replacementNodes;
    }

  protected List<Element> stripAndReturnChildElements(final Element element, final INKFRequestContext context, boolean tolerant ) throws NKFException, ParserConfigurationException
    {
    List<Element> childElements = new ArrayList<Element>();
    Elements elements = element.getChildElements();
    for (int i=0; i<elements.size(); i++)
      {
      Element e = elements.get(i);
      if (e.getChildElements().size() > 0 && "".equals(e.getNamespacePrefix()))
        {
        processTemplate(e, context, tolerant);
        }

      childElements.add(e);
      }
    element.removeChildren();

    return childElements;
    }



  private org.w3c.dom.Document getMutableClone(final org.w3c.dom.Node node) throws ParserConfigurationException
    {
    org.w3c.dom.Document result;
    if (node instanceof org.w3c.dom.Document)
      {
      result = (org.w3c.dom.Document) XMLUtils.safeDeepClone(node);
      }
    else
      {
      result = org.netkernel.layer0.util.XMLUtils.newDocument();
      result.appendChild(result.importNode(node, true));
      }
    return result;
    }

  protected void exceptionHandler(INKFRequestContext context, Exception exception, Node node, boolean tolerant)  throws NKFException
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


//  private void exceptionHandler(INKFRequestContext aHelper, boolean tolerant, String aException, String aMessage, org.w3c.dom.Node aElement, Exception e, String target) throws Exception
//    {
//    if (tolerant)
//      {
//      aHelper.logFormatted(INKFLocale.LEVEL_WARNING, aMessage, XMLUtils.getPathFor(aElement), e, target);
//      if (e != null)
//        {
//        aHelper.logRaw(INKFLocale.LEVEL_WARNING, Utils.throwableToString(e));
//        }
//      }
//    else
//      {
//      throw aHelper.createFormattedException(aException, aMessage, XMLUtils.getPathFor(aElement), e, target);
//      }
//    }



  }
