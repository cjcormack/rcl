package org.netkernelroc.lang.rcl.endpoint;

import org.netkernel.layer0.nkf.*;
import org.netkernel.layer0.util.RequestBuilder;
import org.netkernel.layer0.util.XMLReadable;
import org.netkernel.layer0.util.XMLUtils;
import org.netkernel.module.standard.endpoint.StandardAccessorImpl;
import org.netkernel.util.Utils;
import org.w3c.dom.*;

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
  private static final String RCL_NS = "http://netkernelroc.org/rcl";
  private static final String ARGUMENT_ATT = "argument-";

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
    processTemplate(template.getDocumentElement(), context, tolerant);

    INKFResponse response = context.createResponseFrom(template);

    if (mimeType != null)
      {
      response.setMimeType(mimeType);
      }
    else
      {
      response.setMimeType("application/xml");
      }
    }




  //========== Private ==========

  private void processTemplate(Element documentElement, INKFRequestContext context, boolean tolerant) throws Exception
    {
    String rclns = documentElement.getAttribute("xmlns:rcl");
    // Remove the namespace attribute from the processed node
    if (rclns.length() > 0)
      {
      documentElement.removeAttribute("xmlns:rcl");
      }

    Element e = XMLUtils.getFirstChildElement(documentElement);
    while (e != null)
      {
      String ns = e.getNamespaceURI();
      if (ns != null && ns.equals(RCL_NS))
        {
        String name = e.getLocalName();
        if (name.equals("include"))
          {
          processInclude(e, context, tolerant);
          }
        else if (name.equals("resolve"))
          {
//          processResolve(e, context, tolerant);
          }
        else if (name.equals("eval"))
          {
//          processEval(e, context, tolerant);
          }
        else
          {
          exceptionHandler(context, tolerant, "EXP_PROCESSING", "MSG_UNSUPPORTED_TAG", documentElement, null, e.getNodeName());
          }
        }
      else
        {
        String inlineResolve = e.getAttribute("rcl:resolve");
        if (inlineResolve.length() > 0)
          {
//          processInlineResolve(e, inlineResolve, context, tolerant);
          }
        String inlineEval = e.getAttribute("rcl:eval");
        if (inlineEval.length() > 0)
          {
//          processInlineEval(e, inlineEval, context, tolerant);
          }
        processTemplate(e, context, tolerant);
        }
      e = XMLUtils.getNextSiblingElement(e);
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
  private ArrayList<Element> processInclude(Element includeElement, INKFRequestContext context, boolean tolerant) throws Exception
    {
    ArrayList<Element> elementsToInclude = null;
    ArrayList<Element> elementsFromInclude;
    String target = "include";

    try
      {
      // Build the list of elements that are the set of included "replacement" elements
      NodeList children = includeElement.getChildNodes();
      elementsToInclude = new ArrayList<Element>(children.getLength());

      Element element = XMLUtils.getFirstChildElement(includeElement);
      while(element != null)
        {
        String ns = element.getNamespaceURI();
        if (ns != null && ns.equals(RCL_NS))
          {
          String name = element.getLocalName();
          if ("request".equals(name))
            {
            Element newElement = processRequest(element, context);
            elementsToInclude.add((Element)includeElement.getOwnerDocument().importNode(newElement, true));
            }
          else if ("include".equals(name))
            {
            elementsFromInclude = processInclude(element, context, tolerant);
            elementsToInclude.addAll(elementsFromInclude);
            }
          else if ("xpath".equals(name))
            {
            System.out.println("We don't handle xpath yet.");
            }
          else
            {
            exceptionHandler(context, tolerant, "EXP_INCLUDE", "MSG_UNSUPPORTED_TAG", includeElement, null, target);
            }
          }
        else
          {
          processTemplate(element, context, tolerant);
          elementsToInclude.add(element);
          }
        element = XMLUtils.getNextSiblingElement(element);
        }

      Element toReplace = includeElement;
      Node owningNode = toReplace.getParentNode();

      Node n = owningNode.getFirstChild();
      while(n != toReplace) {
        n = n.getNextSibling();
      }

      // Insert all of the include elements before the replace target
      for (int i = 0; i < elementsToInclude.size(); i++)
        {
        owningNode.insertBefore(elementsToInclude.get(i), n);
        }

      toReplace.getParentNode().removeChild(toReplace);
      }
    catch (Exception e)
      {
      e.printStackTrace();
//      exceptionHandler(context, tolerant, "EX_INCLUDE", "MSG_EVAL", includeElement, e, target);
      }
    return elementsToInclude;
    }


      // Without an XPath the only option is to replace the rcl:include includeElement
//      boolean replaceInclude = XMLUtils.getChildElementNamed(includeElement, "xpath") == null;
//      if (replaceInclude)
//        {
//        Element requestElement = XMLUtils.getChildElementNamed(includeElement, "request");
//        XMLReadable readableDOM = new XMLReadable(requestElement);
//        request = buildRequest(readableDOM, context);
//        processArgumentAttributes(request, includeElement);
//        }
//      else
//        {
        // Not supporting rcl:xpath right now
//        XMLReadable readableDOM = new XMLReadable(includeElement);
//        request = buildRequest(readableDOM, context);
//
//        String xpath = readableDOM.getTrimText("xrl:xpath");
//        if (xpath.length() > 0)
//          {
//          List<Node> nodes = XPath.eval(xpath, includeElement);
//          if (nodes.size() == 1 && nodes.get(0) instanceof Element)
//            {
//            toReplace = (Element) nodes.get(0);
//            }
//          else
//            {
//            exceptionHandler(context, tolerant, "EX_INCLUDE", "MSG_BAD_XPATH", includeElement, null, target);
//            }
//          }
//        }

//      request.setRepresentationClass(Node.class);
//      Node n = (Node) context.issueRequest(request);
//      if (n == null)
//        {
//        exceptionHandler(context, tolerant, "EX_INCLUDE", "MSG_NULL", includeElement, null, target);
//        return;
//        }
//      else if (n instanceof Document)
//        {
//        n = ((Document) n).getDocumentElement();
//        }





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



  private Element processRequest(Element requestElement, INKFRequestContext context) throws Exception
    {
    INKFRequest request;
    XMLReadable readableDOM = new XMLReadable(requestElement);

    request = buildRequest(readableDOM, context);
    // Not supporting attribute processing now
    // processArgumentAttributes(request, includeElement);
    request.setRepresentationClass(Node.class);
    Node n = (Node) context.issueRequest(request);
    if (n == null)
      {
      // Do some sort of exception processing
      // exceptionHandler(context, tolerant, "EX_INCLUDE", "MSG_NULL", includeElement, null, target);
      }
    else if (n instanceof Document)
      {
      n = ((Document) n).getDocumentElement();
      }


    return (Element)n;
    }

  /**
   * Build and request a request given an rcl:request element
   *
   * @param aReadable
   * @param aContext
   * @return
   * @throws Exception
   */
  private INKFRequest buildRequest(XMLReadable aReadable, INKFRequestContext aContext) throws Exception
    {
    Document d = XMLUtils.newDocument();
    Element docEl = d.createElement("request");
    d.appendChild(docEl);
    String identifier = aReadable.getTrimText("rcl:identifier");
    Element idEl = d.createElement("identifier");
    XMLUtils.setText(idEl, identifier);
    docEl.appendChild(idEl);
    List<Element> arguments = (List<Element>) (List) aReadable.getNodes("rcl:argument");
    for (Element argument : arguments)
      {
      Element argEl = d.createElement("argument");
      NamedNodeMap nnm = argument.getAttributes();
      for (int i = 0; i < nnm.getLength(); i++)
        {
        Node attIn = nnm.item(i);
        argEl.setAttribute(attIn.getLocalName(), attIn.getNodeValue());
        }
      for (Node n = argument.getFirstChild(); n != null; n = n.getNextSibling())
        {
        argEl.appendChild(d.importNode(n, true));
        }
      docEl.appendChild(argEl);
      }

    RequestBuilder b = new RequestBuilder(docEl, aContext.getKernelContext().getKernel().getLogger());
    INKFRequest req = b.buildRequest(aContext, null, null);
    return req;
    }

  // We are not doing this in RCL (yet?)
  private void processArgumentAttributes(INKFRequest aReq, Element aElement) throws NKFException
    {
    NamedNodeMap nnm = aElement.getAttributes();
    for (int i = nnm.getLength() - 1; i >= 0; i--)
      {
      Node n = nnm.item(i);
      String argName = ("xrl".equals(n.getPrefix())) ? n.getLocalName() : n.getNodeName();
      if (argName.startsWith(ARGUMENT_ATT))
        {
        String name = argName.substring(ARGUMENT_ATT.length());
        String value = n.getNodeValue();
        try
          {
          aReq.addArgument(name, value);
          }
        catch (NKFException e)
          {  //argument not available so don't add it- if mandatory then grammar will catch it
          if (!isArg(value))
            {
            throw e;
            }
          }
        aElement.removeAttributeNode((Attr) n);
        }
      }
    }

  private static boolean isArg(String aIdentifier)
    {
    return aIdentifier.startsWith("arg:");
    }


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


  }
