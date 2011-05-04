package org.netkernelroc.lang.rcl.endpoint;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.converters.DOMConverter;
import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFResponse;
import org.netkernel.layer0.nkf.NKFException;
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
    ArrayList<Node> replacementNodes = new ArrayList<Node>();

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
        if (TAG_IF.equals(e.getLocalName()))
          {
          replacementNodes.addAll(processIf(e, context, tolerant));
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
    ArrayList<Node> replacementNodes = new ArrayList<Node>();

    List<Element> childElements = stripAndReturnChildElements(includeElement, context, tolerant);

    for(Element e : childElements)
      {
      if( e.getNamespaceURI(NS_RCL)!= null)
         {
         if ("request".equals(e.getLocalName()))
           {
           replacementNodes.add(processRequest(e, context).copy());
           }
         if(TAG_INCLUDE.equals(e.getLocalName()))
           {
           replacementNodes.addAll(processInclude(e, context,  tolerant));
           }
         if(TAG_IF.equals(e.getLocalName()))
           {
           replacementNodes.addAll(processIf(e, context,  tolerant));
           }
         }
      else
        {
        replacementNodes.add(e);
        }
      }

    return replacementNodes;
    }



  protected Element processRequest(final Element requestElement, final INKFRequestContext context) throws NKFException, ParserConfigurationException
    {
    INKFRequest request = buildRequest(requestElement, context);
    request.setRepresentationClass(org.w3c.dom.Node.class);
    org.w3c.dom.Node node = (org.w3c.dom.Node)context.issueRequest(request);

    org.w3c.dom.Document template = getMutableClone(node);

    Document document = DOMConverter.convert(template);
    return document.getRootElement();
    }



  protected final boolean processRequestForBoolean(final Element requestElement, final INKFRequestContext context) throws NKFException
    {
    INKFRequest request = buildRequest(requestElement, context);
    request.setRepresentationClass(java.lang.Boolean.class);
    return (Boolean)context.issueRequest(request);
    }



    protected INKFRequest buildRequest(final Element requestElement, final INKFRequestContext context) throws NKFException
      {
      INKFRequest request = null;

      Elements elements = requestElement.getChildElements();

      // We have to know the identifier first
      for (int i=0; i<elements.size(); i++)
        {
        nu.xom.Element e = elements.get(i);
        if (e.getNamespaceURI(NS_RCL)!=null && "identifier".equals(e.getLocalName()))
          {
          String uri = e.getValue();
          request = context.createRequest(uri);
          }
        }

      return request;
      }



  protected List<Node> processIf(final Element ifElement, final INKFRequestContext context, boolean tolerant)   throws NKFException, ParserConfigurationException
    {
    ArrayList<Node> replacementNodes = new ArrayList<Node>();
    boolean test = false;

    List<Element> childElements = stripAndReturnChildElements(ifElement, context, tolerant);

    // Find and issue the request
    for(Element e : childElements)
      {
      if( e.getNamespaceURI(NS_RCL)!= null && "request".equals(e.getLocalName()))
         {
         test = processRequestForBoolean(e, context);
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
         if (TAG_FALSE.equals(e.getLocalName()) && !test)
           {
           replacementNodes.addAll(processIfTrueFalse(e, context, tolerant));
           }
         if(TAG_INCLUDE.equals(e.getLocalName()))
           {
           replacementNodes.addAll(processInclude(e, context,  tolerant));
           }
         if(TAG_IF.equals(e.getLocalName()))
           {
           replacementNodes.addAll(processIf(e, context,  tolerant));
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
    ArrayList<Node> replacementNodes = new ArrayList<Node>();

    List<Element> childElements = stripAndReturnChildElements(trueFalseElement, context, tolerant);

    for(Element e: childElements)
      {
      if (e.getNamespaceURI(NS_RCL)!=null)
        {
        if(TAG_INCLUDE.equals(e.getLocalName()))
          {
          replacementNodes.addAll(processInclude(e, context,  tolerant));
          }
        if(TAG_IF.equals(e.getLocalName()))
          {
          replacementNodes.addAll(processIf(e, context,  tolerant));
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
    ArrayList<Element> childElements = new ArrayList<Element>();
    Elements elements = element.getChildElements();
    for (int i=0; i<elements.size(); i++)
      {
      Element e = elements.get(i);
      if (e.getChildElements().size() > 0 && e.getNamespaceURI("rcl")==null)
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
