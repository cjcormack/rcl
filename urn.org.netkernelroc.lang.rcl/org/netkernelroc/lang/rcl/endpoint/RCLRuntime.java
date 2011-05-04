package org.netkernelroc.lang.rcl.endpoint;

import nu.xom.converters.DOMConverter;
import org.netkernel.layer0.nkf.INKFLocale;
import org.netkernel.layer0.nkf.INKFRequest;
import org.netkernel.layer0.nkf.INKFRequestContext;
import org.netkernel.layer0.nkf.INKFResponse;
import org.netkernel.layer0.util.XMLUtils;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.netkernel.util.Utils;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;

/**
 * RCL Runtime endpoint
 *
 * @author Randolph Kahle
 */
public class RCLRuntime extends StandardAccessorImpl
  {
  private static final String RCL_NS = "http://netkernelroc.org/rcl";

  public RCLRuntime()
    {
    declareThreadSafe();
    }


  public void onSource(INKFRequestContext context) throws Exception
    {
    boolean tolerant = context.getThisRequest().argumentExists("tolerant");
    String mimeType = context.getThisRequest().getArgumentValue("mimeType");

    Node templateNode = context.source("arg:template", Node.class);
    // Make a copy so we don't change an immutable resource representation
    Document template = getMutableClone(templateNode);

    nu.xom.Document document = DOMConverter.convert(template);
    nu.xom.Element element = document.getRootElement();


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

  protected void processTemplate(nu.xom.Element element, INKFRequestContext context, boolean tolerant) throws Exception
    {
    element.removeNamespaceDeclaration("rcl");

    // Gather all of the children of the passed Element, put them
    // into an array and remove them from the passed Element.
    ArrayList<nu.xom.Element> childElements = new ArrayList<nu.xom.Element>();
    nu.xom.Elements elements = element.getChildElements();
    for (int i=0; i<elements.size(); i++)
      {
      nu.xom.Element ee = elements.get(i);
      if (ee.getChildElements().size() > 0 && ee.getNamespaceURI("rcl")==null)
        {
        processTemplate(ee,context, tolerant);
        }
      childElements.add(ee);
      }
    element.removeChildren();


    ArrayList<nu.xom.Node> replacementNodes = new ArrayList<nu.xom.Node>();


    // Go through the childElements and process them
    // into the replacement Elements
    for (int i = 0; i < childElements.size(); i++)
      {
      nu.xom.Element e = childElements.get(i);

      if( e.getNamespaceURI("rcl")!= null)
        {
        String name = e.getLocalName();
        if ("include".equals(name))
          {
          ArrayList<nu.xom.Node> includeNodes = processInclude(e, context, tolerant);

          for( int j=0; j< includeNodes.size(); j++)
            {
            replacementNodes.add(includeNodes.get(j));
            }

          }
        else if ("if".equals(name))
          {
          ArrayList<nu.xom.Node> includeNodes = processIf(e, context, tolerant);

          for( int j=0; j< includeNodes.size(); j++)
            {
            replacementNodes.add(includeNodes.get(j));
            }

          }
        }
      else
        {
        nu.xom.Element ee = childElements.get(i);
        replacementNodes.add(ee);
        }

      }
    // Now use the replacement nodes to build up the template element
    for( int i=0; i<replacementNodes.size(); i++)
      {
      element.appendChild(replacementNodes.get(i));
      }

    }





  /**
   * Called when an rcl:include tag is discovered in the template.
   *
   * @param includeElement The DOM includeElement that is the rcl:include
   * @param context The request context
   * @param tolerant Indicates if processing should be tolerant of errors.
   *
   */
  protected ArrayList<nu.xom.Node> processInclude(nu.xom.Element includeElement, INKFRequestContext context, boolean tolerant) throws Exception
    {
    nu.xom.Element newElement;

    // Gather the children of the include element
    // and remove them from the include element itself
    ArrayList<nu.xom.Element> childElements = new ArrayList<nu.xom.Element>();
    nu.xom.Elements elements = includeElement.getChildElements();
    for (int i=0; i<elements.size(); i++)
      {
      nu.xom.Element ee = elements.get(i);
      if (ee.getChildElements().size() > 0 && ee.getNamespaceURI("rcl")==null)
        {
        processTemplate(ee,context, tolerant);
        }

      childElements.add(ee);
      }
    includeElement.removeChildren();

    ArrayList<nu.xom.Node> replacementNodes = new ArrayList<nu.xom.Node>();


    for (int i=0; i<childElements.size(); i++)
      {
      newElement = null;
      nu.xom.Element e = elements.get(i);

      if( e.getNamespaceURI("rcl")!= null)
         {
         String name = e.getLocalName();
         if ("request".equals(name))
           {
           newElement = processRequest(e, context);
           nu.xom.Node nn = newElement.copy();
           replacementNodes.add(nn);
           }
         if("include".equals(name))
           {
           ArrayList<nu.xom.Node> includeNodes = processInclude(e, context,  tolerant);
           for(int j=0; j < includeNodes.size();j++)
             {
             replacementNodes.add(includeNodes.get(j));
             }
           }
         }
      else
        {
        nu.xom.Element ee = childElements.get(i);
        //processTemplate(ee, context, tolerant);
        replacementNodes.add(ee);
        }
      }

    return replacementNodes;
    }



  protected nu.xom.Element processRequest(nu.xom.Element requestElement, INKFRequestContext context) throws Exception
    {
    INKFRequest request = buildRequest(requestElement, context);
    request.setRepresentationClass(org.w3c.dom.Node.class);
    org.w3c.dom.Node node = (Node)context.issueRequest(request);

    Document template = getMutableClone(node);

    nu.xom.Document document = DOMConverter.convert(template);
    return document.getRootElement();
    }



  protected boolean processRequestForBoolean(nu.xom.Element requestElement, INKFRequestContext context) throws Exception
    {
    INKFRequest request = buildRequest(requestElement, context);
    request.setRepresentationClass(java.lang.Boolean.class);
    Boolean response = (Boolean)context.issueRequest(request);
    return response.booleanValue();
    }




    /**
     * Build an NKF request object from an XML specification
     *
     * rcl:request
     *   rcl:identifier
     *   rcl:verb
     *   rcl:argument
     *   rcl:representation
     */

    protected INKFRequest buildRequest(nu.xom.Element requestElement, INKFRequestContext context) throws Exception
      {
      INKFRequest request = null;

      nu.xom.Elements elements = requestElement.getChildElements();

      // We have to know the identifier first
      for (int i=0; i<elements.size(); i++)
        {
        nu.xom.Element e = elements.get(i);
        if (e.getNamespaceURI("rcl")!=null && "identifier".equals(e.getLocalName()))
          {
          String uri = e.getValue();
          request = context.createRequest(uri);
          }
        }

      return request;
      }



  protected ArrayList<nu.xom.Node> processIf(nu.xom.Element ifElement, INKFRequestContext context, boolean tolerant) throws Exception
    {
    nu.xom.Element newElement = null;
    boolean test = false;

    // Gather the children of the if element
    // and remove them from the if element itself
    ArrayList<nu.xom.Element> childElements = new ArrayList<nu.xom.Element>();
    nu.xom.Elements elements = ifElement.getChildElements();
    for (int i=0; i<elements.size(); i++)
      {
      nu.xom.Element ee = elements.get(i);
      if (ee.getChildElements().size() > 0 && ee.getNamespaceURI("rcl")==null)
        {
        processTemplate(ee,context, tolerant);
        }

      childElements.add(ee);
      }
    ifElement.removeChildren();


    ArrayList<nu.xom.Node> replacementNodes = new ArrayList<nu.xom.Node>();

    // Find the request node, issue the request and find out which sub-node fills the replacementNodes collection
    for (int i=0; i<childElements.size(); i++)
      {
      nu.xom.Element e = childElements.get(i);
      if( e.getNamespaceURI("rcl")!= null)
         {
         String name = e.getLocalName();
         if ("request".equals(name))
           {
           test = processRequestForBoolean(e, context);
           }
         }
      }

     for (int i = 0; i< childElements.size(); i++)
       {
       nu.xom.Element e = childElements.get(i);
       if (e.getNamespaceURI("rcl")!=null)
         {
         if ("true".equals(e.getLocalName()) && test)
           {
           ArrayList<nu.xom.Node> includeNodes = processIfTrueFalse(e, context,  tolerant);
           for(int j=0; j < includeNodes.size();j++)
             {
             replacementNodes.add(includeNodes.get(j));
             }
           }
         if ("false".equals(e.getLocalName()) && !test)
           {
           ArrayList<nu.xom.Node> includeNodes = processIfTrueFalse(e, context,  tolerant);
           for(int j=0; j < includeNodes.size();j++)
             {
             replacementNodes.add(includeNodes.get(j));
             }
           }
         // We also need if and include here...
         }
       else
         {
         replacementNodes.add(childElements.get(i));
         }
       }

    return replacementNodes;
    }


  protected ArrayList<nu.xom.Node> processIfTrueFalse(nu.xom.Element trueFalseElement, INKFRequestContext context, boolean tolerant) throws Exception
    {
    ArrayList<nu.xom.Element> childElements = new ArrayList<nu.xom.Element>();
    nu.xom.Elements elements = trueFalseElement.getChildElements();
    for (int i=0; i<elements.size(); i++)
      {
      nu.xom.Element ee = elements.get(i);
      if (ee.getChildElements().size() > 0 && ee.getNamespaceURI("rcl")==null)
        {
        processTemplate(ee,context, tolerant);
        }

      childElements.add(ee);
      }
    trueFalseElement.removeChildren();


    ArrayList<nu.xom.Node> replacementNodes = new ArrayList<nu.xom.Node>();

    for (int i = 0; i< childElements.size(); i++)
      {
      nu.xom.Element e = childElements.get(i);
      if (e.getNamespaceURI("rcl")!=null)
        {
        // For now we will ignore rcl: sub-elements
        // We also need if and include here...
        }
      else
        {
        replacementNodes.add(childElements.get(i));
        }
      }

   return replacementNodes;


    }


  //========== THIS IS THE OLD CODE ==========




  private Document getMutableClone(Node node) throws ParserConfigurationException
    {
    Document result;
    if (node instanceof Document)
      {
      result = (Document) XMLUtils.safeDeepClone(node);
      }
    else
      {
      result = org.netkernel.layer0.util.XMLUtils.newDocument();
      result.appendChild(result.importNode(node, true));
      }
    return result;
    }


  private void exceptionHandler(INKFRequestContext aHelper, boolean tolerant, String aException, String aMessage, Node aElement, Exception e, String target) throws Exception
    {
    if (tolerant)
      {
      aHelper.logFormatted(INKFLocale.LEVEL_WARNING, aMessage, XMLUtils.getPathFor(aElement), e, target);
      if (e != null)
        {
        aHelper.logRaw(INKFLocale.LEVEL_WARNING, Utils.throwableToString(e));
        }
      }
    else
      {
      throw aHelper.createFormattedException(aException, aMessage, XMLUtils.getPathFor(aElement), e, target);
      }
    }



  }
